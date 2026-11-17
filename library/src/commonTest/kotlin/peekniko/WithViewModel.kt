package peekniko

import peekniko.internal.support.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun <T : ViewModel> withViewModel(
    createViewModel: () -> T,
    block: suspend (T) -> Unit,
) {
    val scheduler = checkNotNull(currentCoroutineContext()[TestCoroutineScheduler]) {
        "withViewModel needs a test scheduler: is coroutineTestScope = true set in io.kotest.provided.ProjectConfig?"
    }
    Dispatchers.setMain(StandardTestDispatcher(scheduler))
    try {
        val viewModel = createViewModel()
        try {
            block(viewModel)
        } finally {
            viewModel.clear()
        }
    } finally {
        Dispatchers.resetMain()
    }
}
