package peekniko

import peekniko.internal.support.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun <T : ViewModel> withViewModel(
    createViewModel: () -> T,
    block: suspend (T) -> Unit,
) {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    val viewModel = createViewModel()
    try {
        block(viewModel)
    } finally {
        viewModel.clear()
        Dispatchers.resetMain()
    }
}
