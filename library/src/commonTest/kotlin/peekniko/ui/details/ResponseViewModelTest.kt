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
                        renderers = listOf(BodyRendererJson { SpannedTextBuilderBBText() }),
                        fallback = BodyRendererPlainText { SpannedTextBuilderBBText() },
                    ),
                    noopBodyRenderer = BodyRendererPlainText { SpannedTextBuilderBBText() },
                    transactionId = "response-id",
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

        test("uses the response body content type") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            id = "response-id",
                            request = sampleRequest(),
                            response = sampleResponse(
                                body = "response body".encodeToByteArray(),
                                headers = listOf(NetworkHeader("Content-Type", "text/plain")),
                            ),
                        ),
                    )
                    awaitItem().contentType shouldBe BodyContentType.PLAIN_TEXT
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

        test("uses plain-text content type") {
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
                    awaitItem().contentType shouldBe BodyContentType.PLAIN_TEXT
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
                    error.isPrettyPrintEnabled shouldBe false
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
        val jsonCall = sampleCall(
            id = "response-id",
            request = sampleRequest(),
            response = sampleResponse(
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
