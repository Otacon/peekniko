package peekniko.ui.details

import app.cash.turbine.test
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableSharedFlow
import peekniko.*
import peekniko.internal.bodyrenderer.*

internal class BodyViewModelTest : FunSpec({
    suspend fun withFixture(
        isRequest: Boolean,
        block: suspend BodyViewModel.(MutableSharedFlow<NetworkCall?>) -> Unit,
    ) {
        // Retain the emission if the eagerly started collector hasn't subscribed yet.
        val transaction = MutableSharedFlow<NetworkCall?>(replay = 1)
        val repository = mock<NetworkRepository> {
            every { observeById(any()) } returns transaction
        }
        withViewModel(
            createViewModel = { backgroundDispatcher ->
                BodyViewModel(
                    repository = repository,
                    themeProvider = BodyRendererThemeProvider { BodyRendererTheme.Light },
                    bodyRenderer = BodyRendererComposite(
                        renderers = listOf(BodyRendererJson()),
                    ),
                    transactionId = "call-id",
                    isRequest = isRequest,
                    backgroundDispatcher = backgroundDispatcher,
                )
            },
        ) { viewModel -> viewModel.block(transaction) }
    }

    // Behaviour shared by both sides: only where the body comes from differs.
    for (isRequest in listOf(true, false)) {
        val side = if (isRequest) "request" else "response"

        fun callWithBytes(body: ByteArray?, contentType: String?): NetworkCall {
            val headers = listOfNotNull(contentType?.let { NetworkHeader("Content-Type", it) })
            return if (isRequest) {
                sampleCall(id = "call-id", request = sampleRequest(body = body, headers = headers))
            } else {
                sampleCall(id = "call-id", response = sampleResponse(body = body, headers = headers))
            }
        }

        fun callWithBody(body: String?, contentType: String?) =
            callWithBytes(body?.encodeToByteArray(), contentType)

        context("$side body") {
            test("starts loading") {
                withFixture(isRequest) { state.value.isLoading shouldBe true }
            }

            test("stops loading when the transaction is absent") {
                withFixture(isRequest) { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(null)
                        awaitItem().isLoading shouldBe false
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

            test("stops loading after the $side arrives") {
                withFixture(isRequest) { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(callWithBody(body = "$side body", contentType = null))
                        val loaded = awaitItem()
                        loaded.isLoading shouldBe false
                        loaded.body.bbText shouldBe "$side body"
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

            context("pretty print") {
                val rawJson = """{"name":"Ada"}"""
                val prettyJson =
                    "[c=Light.separator]{[/c]\n" +
                        "  [c=Light.key]\"name\"[/c][c=Light.separator]:[/c] [c=Light.string]\"Ada\"[/c]\n" +
                        "[c=Light.separator]}[/c]"

                val jsonCall = callWithBody(body = rawJson, contentType = "application/json")

                test("is unavailable before the $side arrives") {
                    withFixture(isRequest) { state.value.prettyPrintState shouldBe PrettyPrintState.Unavailable }
                }

                test("is unavailable when the transaction is absent") {
                    withFixture(isRequest) { transaction ->
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
                        withFixture(isRequest) { transaction ->
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
                        withFixture(isRequest) { transaction ->
                            state.test {
                                awaitItem()
                                transaction.emit(jsonCall)
                                awaitItem()
                                onPrettyPrintClick(false)
                                awaitItem() shouldBe BodyViewModel.State(isLoading = true)
                                val raw = awaitItem()
                                raw.prettyPrintState shouldBe PrettyPrintState.Disabled
                                raw.body.bbText shouldBe rawJson
                                cancelAndIgnoreRemainingEvents()
                            }
                        }
                    }

                    test("formats and highlights the body again when re-enabled") {
                        withFixture(isRequest) { transaction ->
                            state.test {
                                awaitItem()
                                transaction.emit(jsonCall)
                                awaitItem()
                                onPrettyPrintClick(false)
                                awaitItem() shouldBe BodyViewModel.State(isLoading = true)
                                awaitItem().prettyPrintState shouldBe PrettyPrintState.Disabled
                                onPrettyPrintClick(true)
                                awaitItem() shouldBe BodyViewModel.State(isLoading = true)
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
                        withFixture(isRequest) { transaction ->
                            state.test {
                                awaitItem()
                                transaction.emit(callWithBody(body = rawJson, contentType = "text/plain"))
                                val loaded = awaitItem()
                                loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                                loaded.body.bbText shouldBe rawJson
                                cancelAndIgnoreRemainingEvents()
                            }
                        }
                    }

                    test("for a missing content type, showing the raw body") {
                        withFixture(isRequest) { transaction ->
                            state.test {
                                awaitItem()
                                transaction.emit(callWithBody(body = rawJson, contentType = null))
                                val loaded = awaitItem()
                                loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                                loaded.body.bbText shouldBe rawJson
                                cancelAndIgnoreRemainingEvents()
                            }
                        }
                    }

                    test("for a blank body, even with a supported content type") {
                        withFixture(isRequest) { transaction ->
                            state.test {
                                awaitItem()
                                transaction.emit(callWithBody(body = "  ", contentType = "application/json"))
                                val loaded = awaitItem()
                                loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                                loaded.body.bbText shouldBe "  "
                                cancelAndIgnoreRemainingEvents()
                            }
                        }
                    }

                    test("for a missing body, even with a supported content type") {
                        withFixture(isRequest) { transaction ->
                            state.test {
                                awaitItem()
                                transaction.emit(callWithBody(body = null, contentType = "application/json"))
                                val loaded = awaitItem()
                                loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                                loaded.body.bbText shouldBe ""
                                cancelAndIgnoreRemainingEvents()
                            }
                        }
                    }

                    test("regardless of the toggle, for an unsupported content type") {
                        withFixture(isRequest) { transaction ->
                            state.test {
                                awaitItem()
                                onPrettyPrintClick(false)
                                transaction.emit(callWithBody(body = rawJson, contentType = "text/plain"))
                                val loaded = awaitItem()
                                loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                                loaded.body.bbText shouldBe rawJson
                                cancelAndIgnoreRemainingEvents()
                            }
                        }
                    }
                }

                context("image content type") {
                    val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

                    test("exposes the raw bytes as an image, with pretty print unavailable") {
                        withFixture(isRequest) { transaction ->
                            state.test {
                                awaitItem()
                                transaction.emit(callWithBytes(body = pngBytes, contentType = "image/png"))
                                val loaded = awaitItem()
                                loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                                loaded.body.shouldBeInstanceOf<BodyViewModel.Body.Image>()
                                cancelAndIgnoreRemainingEvents()
                            }
                        }
                    }

                    context("renders supported raster formats as images") {
                        withData(
                            "image/png",
                            "image/x-png",
                            "image/apng",
                            "image/jpeg",
                            "image/jpg",
                            "image/pjpeg",
                            "image/gif",
                            "image/webp",
                            "image/bmp",
                            "image/x-bmp",
                            "image/x-ms-bmp",
                            "image/x-icon",
                            "image/vnd.microsoft.icon",
                            "image/vnd.wap.wbmp",
                            "IMAGE/PNG",
                            " image/jpeg ; charset=binary",
                        ) { contentType ->
                            withFixture(isRequest) { transaction ->
                                state.test {
                                    awaitItem()
                                    transaction.emit(callWithBytes(body = pngBytes, contentType = contentType))
                                    awaitItem().body.shouldBeInstanceOf<BodyViewModel.Body.Image>()
                                    cancelAndIgnoreRemainingEvents()
                                }
                            }
                        }
                    }

                    context("renders other content types as text") {
                        withData(
                            "image/svg+xml",
                            "image/tiff",
                            "image/heic",
                            "image/avif",
                            "image/*",
                            "image",
                            "application/octet-stream",
                            "text/plain",
                        ) { contentType ->
                            withFixture(isRequest) { transaction ->
                                state.test {
                                    awaitItem()
                                    transaction.emit(callWithBody(body = "not an image", contentType = contentType))
                                    awaitItem().body.bbText shouldBe "not an image"
                                    cancelAndIgnoreRemainingEvents()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    context("request body") {
        test("ignores the response body") {
            withFixture(isRequest = true) { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            request = sampleRequest(body = "request body".encodeToByteArray()),
                            response = sampleResponse(body = "response body".encodeToByteArray()),
                        ),
                    )
                    awaitItem().body.bbText shouldBe "request body"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    context("response body") {
        test("ignores the request body") {
            withFixture(isRequest = false) { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(
                        sampleCall(
                            request = sampleRequest(body = "request body".encodeToByteArray()),
                            response = sampleResponse(body = "response body".encodeToByteArray()),
                        ),
                    )
                    awaitItem().body.bbText shouldBe "response body"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("is empty while the response is pending") {
            withFixture(isRequest = false) { transaction ->
                state.test {
                    awaitItem()
                    transaction.emit(sampleCall(request = sampleRequest(body = "request body".encodeToByteArray())))
                    val loaded = awaitItem()
                    loaded.isLoading shouldBe false
                    loaded.body shouldBe null
                    loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        context("error") {
            val errorCall = sampleCall(response = sampleError())

            test("stops loading") {
                withFixture(isRequest = false) { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(errorCall)
                        awaitItem().isLoading shouldBe false
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

            test("shows the exception message and stack trace as plain text") {
                withFixture(isRequest = false) { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(errorCall)
                        awaitItem().body.bbText shouldBe "connection failed\ntrace line 1\ntrace line 2"
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

            test("makes pretty print unavailable") {
                withFixture(isRequest = false) { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(errorCall)
                        awaitItem().prettyPrintState shouldBe PrettyPrintState.Unavailable
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }

            test("shows the exception message and stack trace as plain text when pretty print is disabled") {
                withFixture(isRequest = false) { transaction ->
                    state.test {
                        awaitItem()
                        onPrettyPrintClick(false)
                        transaction.emit(errorCall)
                        val error = awaitItem()
                        error.prettyPrintState shouldBe PrettyPrintState.Unavailable
                        error.body.bbText shouldBe "connection failed\ntrace line 1\ntrace line 2"
                        cancelAndIgnoreRemainingEvents()
                    }
                }
            }
        }
    }
})

private val BodyViewModel.Body?.bbText: String
    get() = ((this as BodyViewModel.Body.Text).text as SpannedTextBBText).text
