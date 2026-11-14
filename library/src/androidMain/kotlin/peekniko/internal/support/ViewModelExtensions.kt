package peekniko.internal.support

import androidx.fragment.app.Fragment

internal inline fun <reified T : ViewModel> Fragment.viewModels(
    noinline viewModelProducer: () -> T,
): Lazy<T> = lazy(LazyThreadSafetyMode.NONE) {
    viewModels(T::class, viewModelFactory { initializer(viewModelProducer) })
}

internal fun <T : ViewModel> Fragment.viewModels(
    modelClass: kotlin.reflect.KClass<T>,
    factory: ViewModelFactory,
): T = FragmentViewModelStore(this).get(modelClass, factory)
