package peekniko

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

public interface NetworkRepository {

    public val networkCalls: Flow<List<NetworkCall>>

    public fun observeCallSummaries(): Flow<List<NetworkCallSummary>>

    public fun observeCallUpdates(limit: Int): Flow<List<NetworkCall>>

    public fun observeById(id: String): Flow<NetworkCall?>

    public fun insert(id: String, request: NetworkRequest): Deferred<Boolean>

    public fun insert(id: String, response: NetworkResponse): Deferred<Boolean>

    public suspend fun remove(id: String): Boolean

    public suspend fun getById(id: String): NetworkCall?

    public suspend fun getAll(): List<NetworkCall>

    public suspend fun clear()
}
