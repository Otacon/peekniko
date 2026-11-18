package peekniko.ui.details

import app.cash.turbine.test
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import peekniko.useBBTextSpannedText
import peekniko.NetworkCall
import peekniko.NetworkHeader
import peekniko.NetworkRepository
import peekniko.internal.bodyrenderer.*
import peekniko.bbText
import peekniko.sampleCall
import peekniko.sampleRequest
import peekniko.withViewModel

internal class RequestViewModelTest : FunSpec({
    useBBTextSpannedText()


    suspend fun withFixture(block: suspend RequestViewModel.(MutableSharedFlow<NetworkCall?>) -> Unit) {
        // Retain the emission if the eagerly started collector hasn't subscribed yet.
        val transaction = MutableSharedFlow<NetworkCall?>(replay = 1)
        val repository = mock<NetworkRepository> {
            every { observeById(any()) } returns transaction
        }
        withViewModel(
            createViewModel = {
                RequestViewModel(
                    repository = repository,
                    themeProvider = BodyRendererThemeProvider { BodyRendererTheme.Light },
                    bodyRenderer = BodyRendererComposite(
                        renderers = listOf(BodyRendererJson()),
                        fallback = BodyRendererPlainText(),
                    ),
                    noopBodyRenderer = BodyRendererPlainText(),
                    transactionId = "request-id",
                )
            },
        ) { viewModel -> viewModel.block(transaction) }
    }

    context("initial state") {
        test("starts loading") {
            withFixture { state.value.isLoading shouldBe true }
        }
        test("has content type PLAIN_TEXT") {
            withFixture { state.value.contentType shouldBe BodyContentType.PLAIN_TEXT }
        }
    }

    context("missing request") {
        test("stops loading when the transaction is absent") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(null)
                    awaitItem().isLoading shouldBe false
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    context("available request") {
        test("stops loading after the request arrives") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(sampleCall())
                    awaitItem().isLoading shouldBe false
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("uses plain-text content type for the request body") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(sampleCall())
                    awaitItem().contentType shouldBe BodyContentType.PLAIN_TEXT
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    context("pretty print") {
        val rawJson = """{"name":"Ada"}"""
        val prettyJson =
            "[c=Light.separator]{[/c]\n" +
                "  [c=Light.key]\"name\"[/c][c=Light.separator]:[/c] [c=Light.string]\"Ada\"[/c]\n" +
                "[c=Light.separator]}[/c]"

        fun requestCall(body: String?, contentType: String?) = sampleCall(
            id = "request-id",
            request = sampleRequest(
                body = body?.encodeToByteArray(),
                headers = listOfNotNull(contentType?.let { NetworkHeader("Content-Type", it) }),
            ),
        )

        val jsonCall = requestCall(body = rawJson, contentType = "application/json")

        test("is unavailable before the request arrives") {
            withFixture { state.value.prettyPrintState shouldBe PrettyPrintState.Unavailable }
        }

        test("is unavailable when the transaction is absent") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(null)
                    awaitItem().prettyPrintState shouldBe PrettyPrintState.Unavailable
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        context("supported content type") {
            test("is enabled by default, formatting and highlighting the body") {
                withFixture { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(jsonCall)
                        val loaded = awaitItem()
                        loaded.prettyPrintState shouldBe PrettyPrintState.Enabled
                        loaded.body.bbText shouldBe prettyJson
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

            test("shows the raw body, neither formatted nor highlighted, when disabled") {
                withFixture { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(jsonCall)
                        awaitItem()
                        onPrettyPrintClick(false)
                        val raw = awaitItem()
                        raw.prettyPrintState shouldBe PrettyPrintState.Disabled
                        raw.body.bbText shouldBe rawJson
                        raw.contentType shouldBe BodyContentType.JSON
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

            test("formats and highlights the body again when re-enabled") {
                withFixture { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(jsonCall)
                        awaitItem()
                        onPrettyPrintClick(false)
                        awaitItem().prettyPrintState shouldBe PrettyPrintState.Disabled
                        onPrettyPrintClick(true)
                        val pretty = awaitItem()
                        pretty.prettyPrintState shouldBe PrettyPrintState.Enabled
                        pretty.body.bbText shouldBe prettyJson
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }

        context("unavailable") {
            test("for an unsupported content type, showing the raw body") {
                withFixture { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(requestCall(body = rawJson, contentType = "text/plain"))
                        val loaded = awaitItem()
                        loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                        loaded.body.bbText shouldBe rawJson
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

            test("for a missing content type, showing the raw body") {
                withFixture { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(requestCall(body = rawJson, contentType = null))
                        val loaded = awaitItem()
                        loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                        loaded.body.bbText shouldBe rawJson
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

            test("for a blank body, even with a supported content type") {
                withFixture { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(requestCall(body = "  ", contentType = "application/json"))
                        val loaded = awaitItem()
                        loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                        loaded.body.bbText shouldBe "  "
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

            test("for a missing body, even with a supported content type") {
                withFixture { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(requestCall(body = null, contentType = "application/json"))
                        val loaded = awaitItem()
                        loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                        loaded.body.bbText shouldBe ""
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

            test("regardless of the toggle, for an unsupported content type") {
                withFixture { transaction ->
                    state.test {
                        awaitItem()
                        onPrettyPrintClick(false)
                        transaction.emit(requestCall(body = rawJson, contentType = "text/plain"))
                        val loaded = awaitItem()
                        loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                        loaded.body.bbText shouldBe rawJson
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }
    }
})
