package peekniko.ui.details

import app.cash.turbine.test
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import peekniko.*
import peekniko.internal.formatting.TimeFormatter
import kotlin.time.Instant

internal class SummaryViewModelTest : FunSpec({
    suspend fun withFixture(block: suspend SummaryViewModel.(MutableSharedFlow<NetworkCall?>) -> Unit) {
        // Retain the emission if the eagerly started collector hasn't subscribed yet.
        val transaction = MutableSharedFlow<NetworkCall?>(replay = 1)
        val repository = mock<NetworkRepository> {
            every { observeById(any()) } returns transaction
        }
        val timeFormatter = mock<TimeFormatter> {
            every { format(any()) } calls { (instant: Instant) -> "t${instant.toEpochMilliseconds()}" }
        }
        withViewModel(
            createViewModel = { backgroundDispatcher ->
                SummaryViewModel(
                    repository = repository,
                    transactionId = "summary-id",
                    backgroundDispatcher = backgroundDispatcher,
                    timeFormatter = timeFormatter,
                )
            },
        ) { viewModel -> viewModel.block(transaction) }
    }

    context("initial state") {
        test("starts loading") {
            withFixture { state.value.isLoading shouldBe true }
        }
        test("has empty general text") {
            withFixture { state.value.general.bbText shouldBe "" }
        }
        test("has empty request headers") {
            withFixture { state.value.requestHeaders.bbText shouldBe "" }
        }
        test("has empty response headers") {
            withFixture { state.value.responseHeaders.bbText shouldBe "" }
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
                    awaitItem().general.bbText shouldBe "[b]URL: [/b]https://example.com/items\n" +
                            "[b]Method: [/b]GET\n" +
                            "[b]Code: [/b]Running...\n" +
                            "[b]Request time: [/b]t0\n" +
                            "[b]SSL: [/b]Yes"
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
                    awaitItem().requestHeaders.bbText shouldBe "[b]X-Request: [/b]request value"
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
                    awaitItem().responseHeaders.bbText shouldBe ""
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
                            response = sampleResponse(code = 201, protocol = null, tlsVersion = null, cipherSuite = null),
                        ),
                    )
                    awaitItem().general.bbText shouldBe "[b]URL: [/b]https://example.com/items\n" +
                            "[b]Method: [/b]GET\n" +
                            "[b]Code: [/b]201 - Created\n" +
                            "[b]Request time: [/b]t0\n" +
                            "[b]Response time: [/b]t11\n" +
                            "[b]Duration: [/b]11 ms\n" +
                            "[b]SSL: [/b]Yes"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("measures duration from the request timestamp") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            request = sampleRequest(timestamp = 1_000, endpoint = "/items", scheme = "http"),
                            response = sampleResponse(timestamp = 1_250, protocol = null, tlsVersion = null, cipherSuite = null),
                        ),
                    )
                    awaitItem().general.bbText shouldBe "[b]URL: [/b]http://example.com/items\n" +
                            "[b]Method: [/b]GET\n" +
                            "[b]Code: [/b]201 - Created\n" +
                            "[b]Request time: [/b]t1000\n" +
                            "[b]Response time: [/b]t1250\n" +
                            "[b]Duration: [/b]250 ms\n" +
                            "[b]SSL: [/b]No"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("shows protocol, TLS version, and cipher suite when known") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            request = sampleRequest(endpoint = "/items"),
                            response = sampleResponse(
                                code = 200,
                                protocol = "h2",
                                tlsVersion = "TLSv1.3",
                                cipherSuite = "TLS_AES_128_GCM_SHA256",
                            ),
                        ),
                    )
                    awaitItem().general.bbText shouldBe "[b]URL: [/b]https://example.com/items\n" +
                            "[b]Method: [/b]GET\n" +
                            "[b]Code: [/b]200 - OK\n" +
                            "[b]Request time: [/b]t0\n" +
                            "[b]Response time: [/b]t11\n" +
                            "[b]Duration: [/b]11 ms\n" +
                            "[b]Protocol: [/b]h2\n" +
                            "[b]SSL: [/b]Yes\n" +
                            "[b]TLS version: [/b]TLSv1.3\n" +
                            "[b]Cipher suite: [/b]TLS_AES_128_GCM_SHA256"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("hides blank connection details") {
            withFixture { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            request = sampleRequest(endpoint = "/items"),
                            response = sampleResponse(code = 200, protocol = "http/1.1", tlsVersion = "", cipherSuite = null),
                        ),
                    )
                    awaitItem().general.bbText shouldBe "[b]URL: [/b]https://example.com/items\n" +
                            "[b]Method: [/b]GET\n" +
                            "[b]Code: [/b]200 - OK\n" +
                            "[b]Request time: [/b]t0\n" +
                            "[b]Response time: [/b]t11\n" +
                            "[b]Duration: [/b]11 ms\n" +
                            "[b]Protocol: [/b]http/1.1\n" +
                            "[b]SSL: [/b]Yes"
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
                    awaitItem().responseHeaders.bbText shouldBe "[b]X-Response: [/b]response value"
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
                    awaitItem().general.bbText shouldBe "[b]URL: [/b]https://example.com/items\n" +
                            "[b]Method: [/b]GET\n" +
                            "[b]Code: [/b]connection failed\n" +
                            "[b]Request time: [/b]t0\n" +
                            "[b]Response time: [/b]t789\n" +
                            "[b]Duration: [/b]789 ms\n" +
                            "[b]SSL: [/b]Yes"
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
                    awaitItem().responseHeaders.bbText shouldBe ""
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }
})
