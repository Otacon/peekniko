package peekniko.internal.support

import androidx.fragment.app.Fragment

internal inline fun <reified T : ViewModel> Fragment.viewModels(
    noinline viewModelProducer: () -> T,
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
    FragmentViewModelStore(this).get(T::class, viewModelProducer)
}
