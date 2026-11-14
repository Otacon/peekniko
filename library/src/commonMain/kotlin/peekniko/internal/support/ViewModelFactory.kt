package peekniko.internal.support

import kotlin.reflect.KClass

internal class ViewModelFactory internal constructor(
    private val initializers: Map<KClass<out ViewModel>, () -> ViewModel>,
) {
    @Suppress("UNCHECKED_CAST")
    internal fun <T : ViewModel> create(modelClass: KClass<T>): T =
        (initializers[modelClass]
            ?: error("No ViewModel initializer registered for ${modelClass.simpleName}"))() as T
}

internal class ViewModelFactoryBuilder internal constructor() {
    private val initializers: MutableMap<KClass<out ViewModel>, () -> ViewModel> = mutableMapOf()

    internal fun <T : ViewModel> addInitializer(modelClass: KClass<T>, initializer: () -> T) {
        require(modelClass !in initializers) {
            "A ViewModel initializer is already registered for ${modelClass.simpleName}"
        }
        initializers[modelClass] = initializer
    }

    internal fun build(): ViewModelFactory = ViewModelFactory(initializers.toMap())
}

internal fun viewModelFactory(builder: ViewModelFactoryBuilder.() -> Unit): ViewModelFactory =
    ViewModelFactoryBuilder().apply(builder).build()

internal inline fun <reified T : ViewModel> ViewModelFactoryBuilder.initializer(
    noinline initializer: () -> T,
): Unit = addInitializer(T::class, initializer)
