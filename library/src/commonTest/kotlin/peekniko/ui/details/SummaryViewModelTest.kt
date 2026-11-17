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

internal class SummaryViewModelTest : FunSpec({

    suspend fun withFixture(block: suspend SummaryViewModel.(MutableSharedFlow<NetworkCall?>) -> Unit) {
        // Retain the emission if the eagerly started collector hasn't subscribed yet.
        val transaction = MutableSharedFlow<NetworkCall?>(replay = 1)
        val repository = mock<NetworkRepository> {
            every { observeById(any()) } returns transaction
        }
        withViewModel(
            createViewModel = { SummaryViewModel(repository = repository, transactionId = "summary-id") },
        ) { viewModel -> viewModel.block(transaction) }
    }

    context("initial state") {
        test("starts loading") {
            withFixture { state.value.isLoading shouldBe true }
        }
        test("has empty general text") {
            withFixture { state.value.generalText shouldBe "" }
        }
        test("has empty request headers") {
            withFixture { state.value.requestHeadersText shouldBe "" }
        }
        test("has empty response headers") {
            withFixture { state.value.responseHeadersText shouldBe "" }
        }
    }

    context("missing transaction") {
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

    context("pending request") {
        test("shows the running status") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            request = sampleRequest(endpoint = "/items"),
                        ),
                    )
                    awaitItem().generalText shouldBe "Full URL: https://example.com/items\nMethod: GET\nStatus code: Running..."
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("shows request headers") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            request = sampleRequest(
                                headers = listOf(NetworkHeader("X-Request", "request value")),
                            ),
                        ),
                    )
                    awaitItem().requestHeadersText shouldBe "X-Request: request value"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("shows no response headers") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            response = null
                        ),
                    )
                    awaitItem().responseHeadersText shouldBe "(none)"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    context("successful response") {
        test("shows the HTTP status") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            request = sampleRequest(
                                method = NetworkMethod.GET,
                                scheme = "https",
                                host = "example.com",
                                endpoint = "/items",
                            ),
                            response = sampleResponse(code = 201),
                        ),
                    )
                    awaitItem().generalText shouldBe "Full URL: https://example.com/items\nMethod: GET\nStatus code: 201 - Created"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("shows response headers") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            response = sampleResponse(
                                headers = listOf(NetworkHeader("X-Response", "response value")),
                            ),
                        ),
                    )
                    awaitItem().responseHeadersText shouldBe "X-Response: response value"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    context("error response") {
        test("shows the error message as status") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            request = sampleRequest(
                                method = NetworkMethod.GET,
                                scheme = "https",
                                host = "example.com",
                                endpoint = "/items",
                            ),
                            response = sampleError(message = "connection failed"),
                        ),
                    )
                    awaitItem().generalText shouldBe "Full URL: https://example.com/items\nMethod: GET\nStatus code: connection failed"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("shows no response headers") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            response = sampleError(),
                        ),
                    )
                    awaitItem().responseHeadersText shouldBe "(none)"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }
})
