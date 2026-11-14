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
import peekniko.NetworkRepository
import peekniko.internal.bodyrenderer.*
import peekniko.sampleCall
import peekniko.withViewModel

internal class RequestViewModelTest : FunSpec({

    suspend fun withFixture(block: suspend RequestViewModel.(MutableSharedFlow<NetworkCall?>) -> Unit) {
        // Retain the emission if the eagerly started collector hasn't subscribed yet.
        val transaction = MutableSharedFlow<NetworkCall?>(replay = 1)
        val repository = mock<NetworkRepository> {
            every { observeById(any()) } returns transaction
        }
        val viewModel = RequestViewModel(
            repository = repository,
            themeProvider = BodyRendererThemeProvider { BodyRendererTheme.Light },
            bodyRenderer = BodyRendererComposite(
                renderers = emptyList(),
                fallback = BodyRendererPlainText(defaultSpannedTextBuilderFactory()),
            ),
            transactionId = "request-id",
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
})
