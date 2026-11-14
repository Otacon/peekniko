package peekniko.ui.networkcalls

import app.cash.turbine.test
import dev.mokkery.answering.calls as answerCalls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow
import peekniko.internal.formatting.toReadableTime
import peekniko.*
import peekniko.ui.PeeknikoCallRowItem

internal class PeeknikoViewModelTest : FunSpec({
    val pendingCall = sampleCall(
        id = "pending",
        request = sampleRequest(endpoint = "/users", method = NetworkMethod.GET, timestamp = 100),
    )
    val createdCall = sampleCall(
        id = "created",
        request = sampleRequest(endpoint = "/users/1", method = NetworkMethod.POST, timestamp = 200),
        response = sampleResponse(code = 201, timestamp = 250),
    )
    val notFoundCall = sampleCall(
        id = "not-found",
        request = sampleRequest(endpoint = "/missing", method = NetworkMethod.GET, timestamp = 300),
        response = sampleResponse(code = 404, timestamp = 350),
    )
    val errorCall = sampleCall(
        id = "error",
        request = sampleRequest(endpoint = "/offline", method = NetworkMethod.GET, timestamp = 400),
        response = sampleError(message = "connection failed", timestamp = 450),
    )
    val calls = listOf(pendingCall, createdCall, notFoundCall, errorCall)

    val pendingTime = pendingCall.request.timestamp.toReadableTime(includeMillis = true)
    val createdTime = createdCall.request.timestamp.toReadableTime(includeMillis = true)
    val notFoundTime = notFoundCall.request.timestamp.toReadableTime(includeMillis = true)
    val errorTime = errorCall.request.timestamp.toReadableTime(includeMillis = true)
    val pendingMappedCall = PeeknikoCallRowItem(
        id = "pending",
        method = "GET",
        urlPath = "/users",
        requestTime = pendingTime,
        details = "0 B",
        responseCode = null,
        exceptionMessage = null,
        statusBadge = "...",
        subtitle = "$pendingTime · 0 B",
        isSelected = false,
    )
    val createdMappedCall = PeeknikoCallRowItem(
        id = "created",
        method = "POST",
        urlPath = "/users/1",
        requestTime = createdTime,
        details = "50 ms · 0 B",
        responseCode = 201,
        exceptionMessage = null,
        statusBadge = "201",
        subtitle = "$createdTime · 50 ms · 0 B",
        isSelected = false,
    )
    val notFoundMappedCall = PeeknikoCallRowItem(
        id = "not-found",
        method = "GET",
        urlPath = "/missing",
        requestTime = notFoundTime,
        details = "50 ms · 0 B",
        responseCode = 404,
        exceptionMessage = null,
        statusBadge = "404",
        subtitle = "$notFoundTime · 50 ms · 0 B",
        isSelected = false,
    )
    val errorMappedCall = PeeknikoCallRowItem(
        id = "error",
        method = "GET",
        urlPath = "/offline",
        requestTime = errorTime,
        details = "50 ms · 0 B",
        responseCode = null,
        exceptionMessage = "connection failed",
        statusBadge = "ERR",
        subtitle = "connection failed",
        isSelected = false,
    )

    suspend fun withFixture(block: suspend PeeknikoViewModelFixture.() -> Unit) {
        val fixture = PeeknikoViewModelFixture()
        withViewModel(fixture.viewModel) { fixture.block() }
    }

    context("initial state") {
        test("starts loading") {
            withFixture { viewModel.state.value.isLoading shouldBe true }
        }
        test("has no calls") {
            withFixture { viewModel.state.value.calls shouldBe emptyList() }
        }
        test("has zero total count") {
            withFixture { viewModel.state.value.totalCount shouldBe 0 }
        }
        test("has zero filtered count") {
            withFixture { viewModel.state.value.filteredCount shouldBe 0 }
        }
        test("has no active filters") {
            withFixture { viewModel.state.value.hasActiveFilters shouldBe false }
        }
    }

    context("call list") {
        test("stops loading when calls arrive") {
            withFixture {
                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().isLoading shouldBe false
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("maps all calls") {
            withFixture {
                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    val state = awaitItem()
                    state.calls shouldBe listOf(pendingMappedCall, createdMappedCall, notFoundMappedCall, errorMappedCall)
                    state.totalCount shouldBe calls.size
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("shows the clear-calls action when calls are available") {
            withFixture {
                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().showClearCalls shouldBe true
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    context("URL filter") {
        test("filters paths without regard to case") {
            withFixture {
                viewModel.onSearchQueryChanged(" USERS ")

                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().calls shouldBe listOf(pendingMappedCall, createdMappedCall)
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("sets the query in state") {
            withFixture {
                viewModel.onSearchQueryChanged("/users")

                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().query shouldBe "/users"
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("shows the filtered empty state when there are no matches") {
            withFixture {
                viewModel.onSearchQueryChanged("not-present")

                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().showFilteredEmptyState shouldBe true
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("shows the reset-filters action when a query is active") {
            withFixture {
                viewModel.onSearchQueryChanged("/users")

                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().showResetFilters shouldBe true
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    context("method filter") {
        test("filters calls by method") {
            withFixture {
                viewModel.onMethodFilterSelected(NetworkMethod.POST)

                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().calls shouldBe listOf(createdMappedCall)
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("sets the selected method in state") {
            withFixture {
                viewModel.onMethodFilterSelected(NetworkMethod.POST)

                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().method shouldBe NetworkMethod.POST
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    context("status filter") {
        test("filters successful responses") {
            withFixture {
                viewModel.onStatusFilterSelected(CallStatusFilter.SUCCESS_2XX)

                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().calls shouldBe listOf(createdMappedCall)
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("filters non-success responses") {
            withFixture {
                viewModel.onStatusFilterSelected(CallStatusFilter.NON_SUCCESS)

                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().calls shouldBe listOf(notFoundMappedCall)
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("filters exceptions") {
            withFixture {
                viewModel.onStatusFilterSelected(CallStatusFilter.EXCEPTION)

                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().calls shouldBe listOf(errorMappedCall)
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    context("clearing filters") {
        test("resets the URL filter") {
            withFixture {
                viewModel.onSearchQueryChanged("/users")
                viewModel.onClearFiltersClick()

                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().query shouldBe ""
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("resets the method filter") {
            withFixture {
                viewModel.onMethodFilterSelected(NetworkMethod.POST)
                viewModel.onClearFiltersClick()

                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().method shouldBe null
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }

        test("resets the status filter") {
            withFixture {
                viewModel.onStatusFilterSelected(CallStatusFilter.EXCEPTION)
                viewModel.onClearFiltersClick()

                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    awaitItem().status shouldBe null
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    context("selecting a call") {
        test("marks the clicked call as selected") {
            withFixture {
                viewModel.state.test {
                    awaitItem()
                    callsFlow.emit(calls)
                    val populatedState = awaitItem()
                    viewModel.onCallClick(populatedState.calls.first { it.id == "created" })
                    awaitItem().calls shouldBe listOf(
                        pendingMappedCall,
                        createdMappedCall.copy(isSelected = true),
                        notFoundMappedCall,
                        errorMappedCall,
                    )
                    cancelAndIgnoreRemainingEvents()
                }
            }
        }
    }

    context("clearing calls") {
        test("clears the repository when the clear action is clicked") {
            withFixture {
                var clearInvoked = false
                everySuspend { repository.clear() } answerCalls { clearInvoked = true }

                viewModel.onClearClick().join()

                clearInvoked shouldBe true
            }
        }
    }
})

private class PeeknikoViewModelFixture {
    // Retain the emission if the eagerly started collector hasn't subscribed yet.
    val callsFlow = MutableSharedFlow<List<NetworkCall>>(replay = 1)
    val repository = mock<NetworkRepository> {
        every { networkCalls } returns callsFlow
    }
    val viewModel = PeeknikoViewModel(repository)
}
