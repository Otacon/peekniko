package peekniko

import peekniko.internal.support.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T : ViewModel> withViewModel(
    createViewModel: () -> T,
    block: suspend (T) -> Unit,
) {
    Dispatchers.setMain(StandardTestDispatcher())
    val viewModel = createViewModel()
    try {
        runTest { block(viewModel) }
    } finally {
        viewModel.clear()
        Dispatchers.resetMain()
    }
}
