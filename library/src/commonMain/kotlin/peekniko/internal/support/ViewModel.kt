package peekniko.internal.support

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal abstract class ViewModel(
    protected val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val viewModelJob = SupervisorJob()

    protected val viewModelScope: CoroutineScope =
        CoroutineScope(viewModelJob + Dispatchers.Main.immediate)

    protected open fun onCleared(): Unit = Unit

    internal fun clear() {
        try {
            onCleared()
        } finally {
            viewModelJob.cancel()
        }
    }
}
