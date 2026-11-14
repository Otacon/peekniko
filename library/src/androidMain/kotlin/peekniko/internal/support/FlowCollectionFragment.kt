package peekniko.internal.support

import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class FlowCollectionFragment(
    contentLayoutId: Int,
    private val resumedAndVisibleOnly: Boolean = false,
) : Fragment(contentLayoutId) {
    private var started = false
    private var resumed = false
    private var collectionScope: CoroutineScope? = null
    private var collectionBlock: (suspend CoroutineScope.() -> Unit)? = null
    private var collectionJob: Job? = null
    private val collectionMutex = Mutex()

    protected fun collectInView(block: suspend CoroutineScope.() -> Unit) {
        check(collectionScope == null) { "Collection already registered for this view" }
        collectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        collectionBlock = block
        updateCollection()
    }

    override fun onStart() {
        super.onStart()
        started = true
        updateCollection()
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        updateCollection()
    }

    override fun onPause() {
        resumed = false
        updateCollection()
        super.onPause()
    }

    override fun onStop() {
        started = false
        updateCollection()
        super.onStop()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        updateCollection()
    }

    override fun onDestroyView() {
        collectionScope?.cancel()
        collectionScope = null
        collectionJob = null
        collectionBlock = null
        super.onDestroyView()
    }

    private fun updateCollection() {
        val active = started && (!resumedAndVisibleOnly || (resumed && !isHidden))
        if (!active) {
            collectionJob?.cancel()
            collectionJob = null
            return
        }
        if (collectionJob != null) return
        val block = collectionBlock ?: return
        collectionJob = collectionScope?.launch {
            collectionMutex.withLock { coroutineScope { block() } }
        }
    }
}
