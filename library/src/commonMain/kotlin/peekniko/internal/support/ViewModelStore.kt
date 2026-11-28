package peekniko.internal.support

import kotlin.reflect.KClass

internal class ViewModelStore {
    private val viewModels: MutableMap<Pair<KClass<out ViewModel>, String?>, ViewModel> = mutableMapOf()

    /**
     * Returns the [modelClass] instance stored under [key], creating it with [create] if missing.
     * Use distinct keys to keep several instances of the same class apart.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <T : ViewModel> getOrCreate(
        modelClass: KClass<T>,
        key: String? = null,
        create: () -> T,
    ): T = viewModels.getOrPut(modelClass to key, create) as T

    internal fun clear() {
        val storedViewModels = viewModels.values.toList()
        viewModels.clear()
        storedViewModels.forEach(ViewModel::clear)
    }
}
