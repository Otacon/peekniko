package peekniko.ui.details

import app.cash.turbine.test
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import peekniko.*
import peekniko.internal.bodyrenderer.*

internal class ResponseViewModelTest : FunSpec({
    useBBTextSpannedText()

    suspend fun withFixture(block: suspend ResponseViewModel.(MutableSharedFlow<NetworkCall?>) -> Unit) {
        // Retain the emission if the eagerly started collector hasn't subscribed yet.
        val transaction = MutableSharedFlow<NetworkCall?>(replay = 1)
        val repository = mock<NetworkRepository> {
            every { observeById(any()) } returns transaction
        }
        withViewModel(
            createViewModel = {
                ResponseViewModel(
                    repository = repository,
                    themeProvider = BodyRendererThemeProvider { BodyRendererTheme.Light },
                    bodyRenderer = BodyRendererComposite(
                        renderers = listOf(BodyRendererJson()),
                    ),
                    transactionId = "response-id",
                )
            },
        ) { viewModel -> viewModel.block(transaction) }
    }

    context("initial state") {
        test("starts loading") {
            withFixture { state.value.isLoading shouldBe true }
        }
    }

    context("missing response") {
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

    context("successful response") {
        test("stops loading after the response arrives") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            id = "response-id",
                            request = sampleRequest(),
                            response = sampleResponse(body = "response body".encodeToByteArray()),
                        ),
                    )
                    awaitItem().isLoading shouldBe false
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    context("error response") {
        test("stops loading") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            id = "response-id",
                            request = sampleRequest(),
                            response = sampleError(),
                        ),
                    )
                    awaitItem().isLoading shouldBe false
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("shows the exception message and stack trace as plain text") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            id = "response-id",
                            request = sampleRequest(),
                            response = sampleError(),
                        ),
                    )
                    awaitItem().body.bbText shouldBe "connection failed\ntrace line 1\ntrace line 2"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("makes pretty print unavailable") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            id = "response-id",
                            request = sampleRequest(),
                            response = sampleError(),
                        ),
                    )
                    awaitItem().prettyPrintState shouldBe PrettyPrintState.Unavailable
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("shows the exception message and stack trace as plain text when pretty print is disabled") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    onPrettyPrintClick(false)
                    transaction.emit(
                        sampleCall(
                            id = "response-id",
                            request = sampleRequest(),
                            response = sampleError(),
                        ),
                    )
                    val error = awaitItem()
                    error.prettyPrintState shouldBe PrettyPrintState.Unavailable
                    error.body.bbText shouldBe "connection failed\ntrace line 1\ntrace line 2"
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

        fun responseCall(body: String?, contentType: String?) = sampleCall(
            id = "response-id",
            request = sampleRequest(),
            response = sampleResponse(
                body = body?.encodeToByteArray(),
                headers = listOfNotNull(contentType?.let { NetworkHeader("Content-Type", it) }),
            ),
        )

        val jsonCall = responseCall(body = rawJson, contentType = "application/json")

        test("is unavailable before the response arrives") {
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
                        transaction.emit(responseCall(body = rawJson, contentType = "text/plain"))
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
                        transaction.emit(responseCall(body = rawJson, contentType = null))
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
                        transaction.emit(responseCall(body = "  ", contentType = "application/json"))
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
                        transaction.emit(responseCall(body = null, contentType = "application/json"))
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
                        transaction.emit(responseCall(body = rawJson, contentType = "text/plain"))
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
