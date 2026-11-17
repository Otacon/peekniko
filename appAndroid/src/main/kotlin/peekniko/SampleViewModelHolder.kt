package peekniko

import androidx.lifecycle.ViewModel

internal class SampleViewModelHolder : ViewModel() {
    val sampleViewModel: SampleViewModel

    init {
        AppModule.initialize()
        sampleViewModel = SampleViewModel()
    }

    override fun onCleared() {
        sampleViewModel.clear()
    }
}
