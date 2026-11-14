package peekniko.ui

import peekniko.Peekniko
import peekniko.NetworkMethod
import peekniko.internal.support.ViewModelStore
import peekniko.ui.networkcalls.CallStatusFilter
import peekniko.ui.networkcalls.PeeknikoViewModel
import peekniko.ui.networkcalls.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

public class PeeknikoObservationToken internal constructor(
    private val onCancel: () -> Unit,
) {
    public fun cancel() {
        onCancel()
    }
}

public class PeeknikoCallsController internal constructor(
    private val viewModel: PeeknikoViewModel,
    private val scope: CoroutineScope,
    private val store: ViewModelStore,
) {
    public fun observeState(onEach: (State) -> Unit): PeeknikoObservationToken {
        val job = scope.launch {
            viewModel.state.collect { onEach(it) }
        }
        return PeeknikoObservationToken { job.cancel() }
    }

    public fun setSearchQuery(query: String) {
        viewModel.onSearchQueryChanged(query)
    }

    public fun setMethodFilter(method: NetworkMethod?) {
        viewModel.onMethodFilterSelected(method)
    }

    public fun setStatusFilter(status: CallStatusFilter) {
        viewModel.onStatusFilterSelected(status)
    }

    public fun clearFilters() {
        viewModel.onClearFiltersClick()
    }

    public fun clearCalls() {
        viewModel.onClearClick()
    }

    public fun close() {
        scope.cancel()
        store.clear()
    }
}

public object PeeknikoIosUiBridge {
    public fun createCallsController(): PeeknikoCallsController {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val store = ViewModelStore()
        val viewModel = Peekniko.getPeeknikoViewModel(store)
        return PeeknikoCallsController(viewModel, scope, store)
    }

    public fun observeCalls(
        onEach: (List<PeeknikoCallRowItem>) -> Unit,
    ): PeeknikoObservationToken {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val store = ViewModelStore()
        val viewModel = Peekniko.getPeeknikoViewModel(store)
        val job = scope.launch {
            viewModel.state.collect { list ->
                onEach(list.calls)
            }
        }
        return PeeknikoObservationToken {
            job.cancel()
            scope.cancel()
            store.clear()
        }
    }

    public fun observeCallDetail(
        callId: String,
        onEach: (PeeknikoCallDetailItem?) -> Unit,
    ): PeeknikoObservationToken {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val viewModel = Peekniko.createNetworkCallDetailViewModel(callId)

        val job = scope.launch {
            viewModel.state.collect { item ->
                onEach(item.transaction)
            }
        }
        return PeeknikoObservationToken {
            job.cancel()
            scope.cancel()
        }
    }
}
