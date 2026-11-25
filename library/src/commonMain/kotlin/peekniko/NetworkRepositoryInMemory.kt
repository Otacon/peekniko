package peekniko

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

public class NetworkRepositoryInMemory internal constructor(
    maxApiCalls: Int,
    private val dispatcher: CoroutineDispatcher,
) : NetworkRepository {
    public constructor(maxApiCalls: Int = 100) : this(maxApiCalls, Dispatchers.Default)

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val commands: Channel<() -> Unit> = Channel(Channel.UNLIMITED)
    private val calls: BoundedSortedMap<String, NetworkCall, Instant> = BoundedSortedMap(maxApiCalls) { it.request.timestamp }
    private val _networkCalls: MutableStateFlow<List<NetworkCall>> = MutableStateFlow(emptyList())

    init {
        scope.launch {
            for (command in commands) {
                command()
                yield()
            }
        }
    }

    public fun close() {
        commands.close()
    }

    public override fun observeCallUpdates(limit: Int): Flow<List<NetworkCall>> {
        require(limit > 0) { "Limit must be positive" }
        return _networkCalls
            .map { it.take(limit) }
            .distinctUntilChanged { old, new ->
                old.size == new.size && old.indices.all { old[it].sameContent(new[it]) }
            }
            .map { calls -> calls.map(NetworkCall::snapshot) }
            .flowOn(dispatcher)
    }

    public override val networkCalls: Flow<List<NetworkCall>> = _networkCalls
        .map { calls -> calls.map(NetworkCall::snapshot) }
        .flowOn(dispatcher)
        .conflate()

    public override fun observeCallSummaries(): Flow<List<NetworkCallSummary>> = _networkCalls
        .map { calls -> calls.map(NetworkCall::toSummary) }
        .distinctUntilChanged()
        .flowOn(dispatcher)
        .conflate()

    public override fun observeById(id: String): Flow<NetworkCall?> = _networkCalls
        .map { calls -> calls.firstOrNull { it.id == id } }
        .distinctUntilChanged { old, new ->
            if (old == null) new == null else new != null && old.sameContent(new)
        }
        .map { it?.snapshot() }
        .flowOn(dispatcher)
        .conflate()

    public override fun insert(id: String, request: NetworkRequest): Deferred<Boolean> {
        val snapshot = request.snapshot()
        return enqueue {
            val current = calls[id]
            calls[id] = current?.copy(request = snapshot) ?: NetworkCall(id, snapshot)
            publishSnapshot()
            true
        }
    }

    public override fun insert(id: String, response: NetworkResponse): Deferred<Boolean> = enqueue {
        val current = calls[id] ?: return@enqueue false
        calls[id] = current.copy(response = response).snapshot()
        publishSnapshot()
        true
    }

    public override suspend fun remove(id: String): Boolean = onActor {
        val removed = calls.remove(id) != null
        if (removed) publishSnapshot()
        removed
    }

    public override suspend fun getById(id: String): NetworkCall? = onActor {
        calls[id]?.snapshot()
    }

    public override suspend fun getAll(): List<NetworkCall> = onActor {
        calls.values.map(NetworkCall::snapshot)
    }

    private fun <T> enqueue(caller: Job? = null, operation: () -> T): Deferred<T> {
        val result = CompletableDeferred<T>()
        val submitted = commands.trySend {
            if (caller != null && !caller.isActive) {
                result.cancel()
            } else if (result.isActive) {
                try {
                    result.complete(operation())
                } catch (failure: Throwable) {
                    result.completeExceptionally(failure)
                }
            }
        }
        if (submitted.isFailure) {
            result.completeExceptionally(IllegalStateException("Repository is closed"))
        }
        return result
    }

    private suspend fun <T> onActor(operation: () -> T): T {
        val result = enqueue(currentCoroutineContext()[Job], operation)
        return try {
            result.await()
        } finally {
            result.cancel()
        }
    }

    override suspend fun clear(): Unit = onActor {
        calls.clear()
        publishSnapshot()
    }

    private fun publishSnapshot() {
        _networkCalls.value = calls.values
    }
}

private class BoundedSortedMap<K, V, S : Comparable<S>>(
    private val maxSize: Int,
    private val sortBy: (V) -> S,
) {

    private class Entry<K, V, S>(
        val key: K,
        val value: V,
        val sortKey: S,
    )

    private val byKey = mutableMapOf<K, Entry<K, V, S>>()
    private val ordered = mutableListOf<Entry<K, V, S>>()
    private var cachedValues: List<V>? = emptyList()

    init {
        require(maxSize > 0)
    }

    // Only accessed by the repository actor. Cached, stable snapshot, newest first.
    val values: List<V>
        get() = cachedValues ?: ordered.map { it.value }
            .also { cachedValues = it }

    operator fun get(key: K): V? = byKey[key]?.value

    operator fun set(key: K, value: V) {
        val sortKey = sortBy(value)
        val previous = byKey[key]

        if (previous != null) {
            ordered.removeAt(
                ordered.indexOfFirst { it === previous }
            )
        }

        val entry = Entry(key, value, sortKey)
        ordered.add(insertionIndex(sortKey), entry)
        byKey[key] = entry

        trim()
        cachedValues = null
    }

    fun containsKey(key: K): Boolean = byKey.containsKey(key)

    fun remove(key: K): V? {
        val previous = byKey.remove(key) ?: return null
        ordered.removeAt(ordered.indexOfFirst { it === previous })
        cachedValues = null
        return previous.value
    }

    fun clear() {
        byKey.clear()
        ordered.clear()
        cachedValues = emptyList()
    }

    private fun insertionIndex(sortKey: S): Int {
        var low = 0
        var high = ordered.size

        while (low < high) {
            val mid = low + (high - low) / 2

            if (sortKey >= ordered[mid].sortKey) {
                high = mid
            } else {
                low = mid + 1
            }
        }

        return low
    }

    private fun trim() {
        while (ordered.size > maxSize) {
            val oldest = ordered.removeAt(ordered.lastIndex)
            byKey.remove(oldest.key)
        }
    }
}

private fun NetworkCall.snapshot(): NetworkCall = copy(
    request = this.request.snapshot(),
    response = this.response.snapshot(),
)

private fun NetworkRequest.snapshot(): NetworkRequest = copy(
    body = body?.copyOf(),
    headers = headers.toList(),
)

private fun NetworkResponse?.snapshot(): NetworkResponse? = when(this) {
    is NetworkResponse.Response -> {
        this.copy(
            body = body?.copyOf(),
            headers = headers.toList()
        )
    }
    is NetworkResponse.Error,
    null -> this
}
