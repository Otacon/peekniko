package peekniko.internal.support

import kotlin.reflect.KClass

internal class ViewModelStore {
    private val viewModels: MutableMap<KClass<out ViewModel>, ViewModel> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    internal fun <T : ViewModel> getOrCreate(
        modelClass: KClass<T>,
        factory: ViewModelFactory,
    ): T {
        val existing = viewModels[modelClass]
        if (existing != null) return existing as T

        return factory.create(modelClass).also { viewModels[modelClass] = it }
    }

    internal fun clear() {
        val storedViewModels = viewModels.values.toList()
        viewModels.clear()
        storedViewModels.forEach(ViewModel::clear)
    }
}
