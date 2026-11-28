package peekniko.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import peekniko.internal.support.ViewModel
import peekniko.internal.support.ViewModelStore
import kotlin.reflect.KClass

internal val LocalViewModelStore = staticCompositionLocalOf<ViewModelStore> {
    error("No ViewModelStore provided")
}

@Composable
internal fun rememberViewModelStore(key: Any?): ViewModelStore {
    val store = remember(key) { ViewModelStore() }
    DisposableEffect(store) {
        onDispose { store.clear() }
    }
    return store
}

@Composable
internal fun <T : ViewModel> retainedViewModel(modelClass: KClass<T>, key: String? = null, create: () -> T): T {
    val store = LocalViewModelStore.current
    return remember(store, modelClass, key) {
        store.getOrCreate(modelClass, key, create)
    }
}

@Composable
internal fun <T : ViewModel> rememberViewModel(key: Any?, create: () -> T): T {
    val viewModel = remember(key) { create() }
    DisposableEffect(viewModel) {
        onDispose { viewModel.clear() }
    }
    return viewModel
}
