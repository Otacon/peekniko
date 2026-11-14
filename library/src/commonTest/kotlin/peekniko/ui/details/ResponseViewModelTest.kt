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
        val viewModel = ResponseViewModel(
            repository = repository,
            themeProvider = BodyRendererThemeProvider { BodyRendererTheme.Light },
            bodyRenderer = BodyRendererComposite(
                renderers = emptyList(),
                fallback = BodyRendererPlainText(defaultSpannedTextBuilderFactory()),
            ),
            transactionId = "response-id",
        )
        withViewModel(viewModel) { block(viewModel, transaction) }
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
    }
})
