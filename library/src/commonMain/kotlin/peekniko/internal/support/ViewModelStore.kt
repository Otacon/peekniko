package peekniko.internal.support

import kotlin.reflect.KClass

internal class ViewModelStore {
    private val viewModels: MutableMap<Pair<KClass<out ViewModel>, String?>, ViewModel> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    /**
     * Returns the [modelClass] instance stored under [key], creating it with [factory] if missing.
     * Use distinct keys to keep several instances of the same class apart.
     */
    internal fun <T : ViewModel> getOrCreate(
        modelClass: KClass<T>,
        factory: ViewModelFactory,
        key: String? = null,
    ): T {
        val storeKey = modelClass to key
        val existing = viewModels[storeKey]
        if (existing != null) return existing as T

        return factory.create(modelClass).also { viewModels[storeKey] = it }
    }

    internal fun clear() {
        val storedViewModels = viewModels.values.toList()
        viewModels.clear()
        storedViewModels.forEach(ViewModel::clear)
    }
}
