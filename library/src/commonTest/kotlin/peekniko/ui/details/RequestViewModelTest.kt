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
import peekniko.NetworkCall
import peekniko.NetworkHeader
import peekniko.NetworkRepository
import peekniko.internal.bodyrenderer.*
import peekniko.SpannedTextBBText
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
            createViewModel = { backgroundDispatcher ->
                RequestViewModel(
                    repository = repository,
                    themeProvider = BodyRendererThemeProvider { BodyRendererTheme.Light },
                    bodyRenderer = BodyRendererComposite(
                        renderers = listOf(BodyRendererJson()),
                    ),
                    transactionId = "request-id",
                    backgroundDispatcher = backgroundDispatcher,
                )
            },
        ) { viewModel -> viewModel.block(transaction) }
    }

    context("initial state") {
        test("starts loading") {
            withFixture { state.value.isLoading shouldBe true }
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
                        awaitItem() shouldBe RequestViewModel.State(isLoading = true)
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
                        awaitItem() shouldBe RequestViewModel.State(isLoading = true)
                        awaitItem().prettyPrintState shouldBe PrettyPrintState.Disabled
                        onPrettyPrintClick(true)
                        awaitItem() shouldBe RequestViewModel.State(isLoading = true)
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

        context("image content type") {
            val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            fun imageCall(contentType: String) = requestCall(body = null, contentType = contentType).let { call ->
                call.copy(request = call.request.copy(body = pngBytes))
            }

            test("exposes the raw bytes as an image, with pretty print unavailable") {
                withFixture { transaction ->
                    state.test {
                        awaitItem()
                        transaction.emit(imageCall("image/png"))
                        val loaded = awaitItem()
                        loaded.prettyPrintState shouldBe PrettyPrintState.Unavailable
                        loaded.body.shouldBeInstanceOf<RequestViewModel.Body.Image>()
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
                    withFixture { transaction ->
                        state.test {
                            awaitItem()
                            transaction.emit(imageCall(contentType))
                            awaitItem().body.shouldBeInstanceOf<RequestViewModel.Body.Image>()
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
                    withFixture { transaction ->
                        state.test {
                            awaitItem()
                            transaction.emit(requestCall(body = "not an image", contentType = contentType))
                            awaitItem().body.bbText shouldBe "not an image"
                            cancelAndIgnoreRemainingEvents()
                        }
                    }
                }
            }
        }
    }
})

private val RequestViewModel.Body?.bbText: String
    get() = ((this as RequestViewModel.Body.Text).text as SpannedTextBBText).text
