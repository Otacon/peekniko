package peekniko.internal.support

import androidx.fragment.app.Fragment
import kotlin.reflect.KClass

internal class FragmentViewModelStore(private val fragment: Fragment) {
    private val store: ViewModelStore
        get() {
            val manager = fragment.childFragmentManager
            val owner = manager.findFragmentByTag(STORE_TAG) as? FragmentRetainedViewModelStore
                ?: FragmentRetainedViewModelStore().also {
                    manager.beginTransaction().add(it, STORE_TAG).commitNow()
                }
            return owner.store
        }

    internal fun <T : ViewModel> get(modelClass: KClass<T>, factory: ViewModelFactory): T =
        store.getOrCreate(modelClass, factory)

    private companion object {
        const val STORE_TAG = "peekniko-model-store"
    }
}

@Suppress("DEPRECATION")
internal class FragmentRetainedViewModelStore : Fragment() {
    init {
        retainInstance = true
    }

    val store = ViewModelStore()

    override fun onDestroy() {
        try {
            store.clear()
        } finally {
            super.onDestroy()
        }
    }
}
