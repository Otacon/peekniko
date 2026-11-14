package peekniko

import peekniko.internal.support.ViewModel

internal suspend fun withViewModel(
    viewModel: ViewModel,
    block: suspend (ViewModel) -> Unit,
): Unit = try {
    block(viewModel)
} finally {
    viewModel.clear()
}
