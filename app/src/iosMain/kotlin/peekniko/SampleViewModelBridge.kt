package peekniko

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SampleViewModelBridge {
    init {
        AppModule.initialize()
    }

    private val sampleViewModel = SampleViewModel()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observation: Job? = null

    public fun observeState(onChanged: (SampleUiState) -> Unit) {
        observation?.cancel()
        observation = scope.launch {
            sampleViewModel.state.collect { state -> onChanged(state) }
        }
    }

    public fun clear() {
        scope.cancel()
        observation = null
    }

    public fun launchKtor() {
        sampleViewModel.onRunKtor()
    }

    public fun launchUrlSession() {
        sampleViewModel.onRunUrlSession()
    }
}
