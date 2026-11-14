package peekniko.storage.sqldelight

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.db.SqlDriver
import peekniko.NetworkCall
import peekniko.NetworkHeader
import peekniko.NetworkMethod
import peekniko.NetworkRepository
import peekniko.NetworkRequest
import peekniko.NetworkResponse
import peekniko.Peekniko
import peekniko.storage.sqldelight.db.Network_call
import peekniko.storage.sqldelight.db.PeeknikoDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Instant

internal expect fun Peekniko.Configuration.sqlDelightStorage(name: String = "peekniko.db")

internal class SqlDelightNetworkRepository(
    driver: SqlDriver,
    private val databaseDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) : NetworkRepository {
    private val database = PeeknikoDatabase(driver)
    private val callsQueries = database.networkCallsQueries
    private val headersQueries = database.headersQueries
    private val asyncScope = CoroutineScope(SupervisorJob() + databaseDispatcher)

    init {
        driver.execute(null, "PRAGMA foreign_keys=ON", 0)
    }

    override val networkCalls: Flow<List<NetworkCall>> = callsQueries.selectAll().observeCalls()

    override fun observeCallUpdates(limit: Int): Flow<List<NetworkCall>> {
        require(limit > 0) { "Limit must be positive" }
        return callsQueries.selectLatest(limit.toLong()).observeCalls()
    }

    private fun Query<Network_call>.observeCalls(): Flow<List<NetworkCall>> = asFlow()
        .map { query -> database.transactionWithResult { query.executeAsList().map(::toCall) } }
        .distinctUntilChanged { old, new ->
            old.size == new.size && old.indices.all { old[it].sameContent(new[it]) }
        }
        .map { calls -> calls.map(NetworkCall::snapshot) }
        .flowOn(databaseDispatcher)

    override fun observeById(id: String): Flow<NetworkCall?> = callsQueries.selectById(id)
        .observeCalls()
        .map { it.singleOrNull() }

    override fun insert(id: String, request: NetworkRequest): Deferred<Boolean> {
        val snapshot = request.snapshot()
        return asyncScope.async {
            transaction {
                val current = load(id)
                val call = current?.copy(request = snapshot) ?: NetworkCall(id, snapshot)
                write(call, inserting = current == null)
                true
            }
        }
    }

    override fun insert(id: String, response: NetworkResponse): Deferred<Boolean> {
        val snapshot = response.snapshot()
        return asyncScope.async {
            transaction {
                val current = load(id) ?: return@transaction false
                write(current.copy(response = snapshot), inserting = false)
                true
            }
        }
    }

    override suspend fun remove(id: String): Boolean = transaction {
        val exists = callsQueries.selectById(id).executeAsOneOrNull() != null
        if (exists) callsQueries.deleteCall(id)
        exists
    }

    override suspend fun getById(id: String): NetworkCall? = transaction {
        load(id)
    }

    override suspend fun getAll(): List<NetworkCall> = transaction {
        callsQueries.selectAll().executeAsList().map(::toCall)
    }

    override suspend fun clear() = transaction {
        callsQueries.clearCalls()
        Unit
    }

    private suspend fun <T> transaction(block: () -> T): T {
        return withContext(databaseDispatcher) {
            database.transactionWithResult { block() }
        }
    }

    private fun load(id: String): NetworkCall? = callsQueries.selectById(id)
        .executeAsOneOrNull()
        ?.let(::toCall)

    private fun write(call: NetworkCall, inserting: Boolean) {
        val request = call.request
        val response = call.response
        val receivedResponse = response as? NetworkResponse.Response
        val errorResponse = response as? NetworkResponse.Error
        if (inserting) {
            callsQueries.insertCall(
                id = call.id,
                scheme = request.scheme,
                host = request.host,
                port = request.port?.toLong(),
                endpoint = request.endpoint,
                query_params = request.queryParams,
                method = request.method.toDatabaseValue(),
                request_body = request.body,
                response_body = receivedResponse?.body,
                response_code = receivedResponse?.code?.toLong(),
                exception_message = errorResponse?.exceptionMessage,
                request_timestamp = request.timestamp.toEpochMilliseconds(),
                response_timestamp = response?.timestamp?.toEpochMilliseconds(),
                stack_trace = errorResponse?.stackTrace,
            )
        }
        else {
            callsQueries.updateCall(
                scheme = request.scheme,
                host = request.host,
                port = request.port?.toLong(),
                endpoint = request.endpoint,
                query_params = request.queryParams,
                method = request.method.toDatabaseValue(),
                request_body = request.body,
                response_body = receivedResponse?.body,
                response_code = receivedResponse?.code?.toLong(),
                exception_message = errorResponse?.exceptionMessage,
                request_timestamp = request.timestamp.toEpochMilliseconds(),
                response_timestamp = response?.timestamp?.toEpochMilliseconds(),
                stack_trace = errorResponse?.stackTrace,
                id = call.id
            )
        }
        headersQueries.deleteHeaders(call.id)
        request.headers.forEachIndexed { index, header ->
            headersQueries.insertHeader(
                call_id = call.id,
                is_response = 0L,
                ordinal = index.toLong(),
                name = header.name,
                value_ = header.value
            )
        }
        receivedResponse?.headers.orEmpty().forEachIndexed { index, header ->
            headersQueries.insertHeader(
                call_id = call.id,
                is_response = 1L,
                ordinal = index.toLong(),
                name = header.name,
                value_ = header.value
            )
        }
    }

    private fun toCall(row: Network_call): NetworkCall {
        val headers = headersQueries.selectHeaders(row.id).executeAsList()
        val requestHeaders = headers
            .filter { it.is_response == 0L }
            .map { NetworkHeader(it.name, it.value_) }
        val responseHeaders = headers
            .filter { it.is_response == 1L }
            .map { NetworkHeader(it.name, it.value_) }
        val request = NetworkRequest(
            scheme = row.scheme,
            host = row.host,
            port = row.port?.toInt(),
            endpoint = row.endpoint,
            queryParams = row.query_params,
            method = row.method.toNetworkMethod(),
            body = row.request_body,
            headers = requestHeaders,
            timestamp = Instant.fromEpochMilliseconds(row.request_timestamp),
        )
        val responseTimestamp = row.response_timestamp?.let(Instant::fromEpochMilliseconds)
        val response = when {
            row.response_code != null && responseTimestamp != null -> NetworkResponse.Response(
                body = row.response_body,
                headers = responseHeaders,
                code = row.response_code.toInt(),
                timestamp = responseTimestamp,
            )
            row.exception_message != null && responseTimestamp != null -> NetworkResponse.Error(
                exceptionMessage = row.exception_message,
                stackTrace = row.stack_trace.orEmpty(),
                timestamp = responseTimestamp,
            )
            else -> null
        }
        return NetworkCall(
            id = row.id,
            request = request,
            response = response,
        )
    }
}

private fun NetworkCall.snapshot(): NetworkCall = copy(
    request = request.copy(
        body = request.body?.copyOf(),
        headers = request.headers.toList(),
    ),
    response = response?.snapshot(),
)

private fun NetworkRequest.snapshot(): NetworkRequest = copy(
    body = body?.copyOf(),
    headers = headers.toList(),
)

private fun NetworkResponse.snapshot(): NetworkResponse = when (this) {
    is NetworkResponse.Response -> copy(
        body = body?.copyOf(),
        headers = headers.toList(),
    )
    is NetworkResponse.Error -> this
}

private fun NetworkMethod.toDatabaseValue(): String = when (this) {
    NetworkMethod.GET -> "GET"
    NetworkMethod.HEAD -> "HEAD"
    NetworkMethod.POST -> "POST"
    NetworkMethod.PUT -> "PUT"
    NetworkMethod.PATCH -> "PATCH"
    NetworkMethod.DELETE -> "DELETE"
    NetworkMethod.OPTIONS -> "OPTIONS"
    NetworkMethod.TRACE -> "TRACE"
    NetworkMethod.CONNECT -> "CONNECT"
    NetworkMethod.UNKNOWN -> "UNKNOWN"
}

private fun String.toNetworkMethod(): NetworkMethod = when (this) {
    "GET" -> NetworkMethod.GET
    "HEAD" -> NetworkMethod.HEAD
    "POST" -> NetworkMethod.POST
    "PUT" -> NetworkMethod.PUT
    "PATCH" -> NetworkMethod.PATCH
    "DELETE" -> NetworkMethod.DELETE
    "OPTIONS" -> NetworkMethod.OPTIONS
    "TRACE" -> NetworkMethod.TRACE
    "CONNECT" -> NetworkMethod.CONNECT
    "UNKNOWN" -> NetworkMethod.UNKNOWN
    else -> NetworkMethod.UNKNOWN
}
