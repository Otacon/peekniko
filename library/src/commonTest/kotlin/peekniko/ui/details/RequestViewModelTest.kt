package peekniko.ui.details

import app.cash.turbine.test
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import peekniko.NetworkCall
import peekniko.NetworkHeader
import peekniko.NetworkRepository
import peekniko.SpannedTextBuilderBBText
import peekniko.internal.bodyrenderer.*
import peekniko.bbText
import peekniko.sampleCall
import peekniko.sampleRequest
import peekniko.withViewModel

internal class RequestViewModelTest : FunSpec({

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
                        renderers = listOf(BodyRendererJson { SpannedTextBuilderBBText() }),
                        fallback = BodyRendererPlainText { SpannedTextBuilderBBText() },
                    ),
                    noopBodyRenderer = BodyRendererPlainText { SpannedTextBuilderBBText() },
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
        val jsonCall = sampleCall(
            id = "request-id",
            request = sampleRequest(
                body = rawJson.encodeToByteArray(),
                headers = listOf(NetworkHeader("Content-Type", "application/json")),
            ),
        )

        test("is enabled by default") {
            withFixture { state.value.isPrettyPrintEnabled shouldBe true }
        }

        test("formats and highlights the body when enabled") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(jsonCall)
                    val loaded = awaitItem()
                    loaded.isPrettyPrintEnabled shouldBe true
                    loaded.body.bbText shouldBe prettyJson
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("shows the raw body when disabled") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(jsonCall)
                    awaitItem()
                    onPrettyPrintClick(false)
                    val raw = awaitItem()
                    raw.isPrettyPrintEnabled shouldBe false
                    raw.body.bbText shouldBe rawJson
                    raw.contentType shouldBe BodyContentType.JSON
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

    }
})
