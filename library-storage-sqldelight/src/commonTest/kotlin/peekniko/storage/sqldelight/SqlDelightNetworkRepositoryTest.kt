package peekniko.storage.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.turbine.test
import peekniko.NetworkCall
import peekniko.NetworkHeader
import peekniko.NetworkMethod
import peekniko.NetworkRequest
import peekniko.NetworkResponse
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlin.time.Instant

internal expect fun createTestSqlDriver(name: String): SqlDriver

internal class SqlDelightNetworkRepositoryTest : FunSpec({
    lateinit var driver: SqlDriver
    lateinit var repository: SqlDelightNetworkRepository
    var driverIndex = 0

    beforeTest {
        driver = createTestSqlDriver("repository-test-${driverIndex++}.db")
        repository = SqlDelightNetworkRepository(driver)
    }

    afterTest {
        driver.close()
    }

    context("request insertion") {
        context("stored request fields") {
            beforeTest {
                repository.insert(
                    "request-fields",
                    sampleRequest(
                        timestamp = 123,
                        scheme = "https",
                        host = "api.example.com",
                        port = 8443,
                        endpoint = "/v2/items",
                        queryParams = "page=2&sort=name",
                        method = NetworkMethod.PATCH,
                        body = "request body".encodeToByteArray(),
                        headers = listOf(NetworkHeader("X-Request", "one"), NetworkHeader("X-Request", "two")),
                    ),
                ).await()
            }

            test("persists request scheme") {
                repository.getById("request-fields")!!.request.scheme shouldBe "https"
            }

            test("persists request host") {
                repository.getById("request-fields")!!.request.host shouldBe "api.example.com"
            }

            test("persists a non-default request port") {
                repository.getById("request-fields")!!.request.port shouldBe 8443
            }

            test("persists request endpoint") {
                repository.getById("request-fields")!!.request.endpoint shouldBe "/v2/items"
            }

            test("persists request query parameters") {
                repository.getById("request-fields")!!.request.queryParams shouldBe "page=2&sort=name"
            }

            test("persists request method") {
                repository.getById("request-fields")!!.request.method shouldBe NetworkMethod.PATCH
            }

            test("persists request body") {
                repository.getById("request-fields")!!.request.body?.decodeToString() shouldBe "request body"
            }

            test("persists repeated request headers in order") {
                repository.getById("request-fields")!!.request.headers shouldContainExactly
                        listOf(NetworkHeader("X-Request", "one"), NetworkHeader("X-Request", "two"))
            }

            test("persists request timestamp") {
                repository.getById("request-fields")!!.request.timestamp shouldBe Instant.fromEpochMilliseconds(123)
            }
        }

        test("copies the request body on insertion and retrieval") {
            val body = byteArrayOf(1, 2, 3)
            repository.insert("request-copy", sampleRequest(body = body)).await()
            body[0] = 9

            val firstRead = repository.getById("request-copy")!!.request.body!!
            firstRead[1] = 8
            repository.getById("request-copy")!!.request.body!!.toList() shouldContainExactly listOf(1, 2, 3)
        }

        test("replaces an existing request instead of adding a second call") {
            repository.insert("replace-request", sampleRequest(timestamp = 1)).await()
            repository.insert(
                "replace-request",
                sampleRequest(timestamp = 2, endpoint = "/updated", headers = listOf(NetworkHeader("Updated", "yes"))),
            ).await()

            repository.getAll().size shouldBe 1
        }

        test("replaces request endpoint for an existing ID") {
            repository.insert("replace-request", sampleRequest(timestamp = 1)).await()
            repository.insert("replace-request", sampleRequest(timestamp = 2, endpoint = "/updated")).await()

            repository.getById("replace-request")!!.request.endpoint shouldBe "/updated"
        }

        test("replaces request timestamp for an existing ID") {
            repository.insert("replace-request", sampleRequest(timestamp = 1)).await()
            repository.insert("replace-request", sampleRequest(timestamp = 2)).await()

            repository.getById("replace-request")!!.request.timestamp shouldBe Instant.fromEpochMilliseconds(2)
        }

        test("request updates preserve an existing response") {
            repository.insert("preserve-response", sampleRequest()).await()
            val response = sampleResponse(code = 204)
            repository.insert("preserve-response", response).await()
            repository.insert("preserve-response", sampleRequest(endpoint = "/changed")).await()

            repository.getById("preserve-response")!!.response shouldBe response
        }

        test("maps unknown stored methods to UNKNOWN") {
            repository.insert("unknown-method", sampleRequest(method = NetworkMethod.UNKNOWN)).await()

            repository.getById("unknown-method")!!.request.method shouldBe NetworkMethod.UNKNOWN
        }
    }

    context("response insertion") {
        context("stored response fields") {
            beforeTest {
                repository.insert("response-fields", sampleRequest()).await()
                repository.insert(
                    "response-fields",
                    sampleResponse(
                        body = "response body".encodeToByteArray(),
                        headers = listOf(NetworkHeader("X-Result", "first"), NetworkHeader("X-Result", "second")),
                        code = 201,
                        timestamp = 456,
                    ),
                ).await()
            }

            test("persists response status code") {
                (repository.getById("response-fields")!!.response as NetworkResponse.Response).code shouldBe 201
            }

            test("persists response body") {
                (repository.getById("response-fields")!!.response as NetworkResponse.Response)
                    .body?.decodeToString() shouldBe "response body"
            }

            test("persists repeated response headers in order") {
                (repository.getById("response-fields")!!.response as NetworkResponse.Response).headers shouldContainExactly
                        listOf(NetworkHeader("X-Result", "first"), NetworkHeader("X-Result", "second"))
            }

            test("persists response timestamp") {
                (repository.getById("response-fields")!!.response as NetworkResponse.Response).timestamp shouldBe
                        Instant.fromEpochMilliseconds(456)
            }
        }

        test("copies the response body on insertion and retrieval") {
            repository.insert("response-copy", sampleRequest()).await()
            val body = byteArrayOf(4, 5, 6)
            repository.insert("response-copy", sampleResponse(body = body)).await()
            body[0] = 0

            val firstRead = (repository.getById("response-copy")!!.response as NetworkResponse.Response).body!!
            firstRead[1] = 0
            (repository.getById("response-copy")!!.response as NetworkResponse.Response).body!!.toList() shouldContainExactly
                    listOf(4, 5, 6)
        }

        test("rejects a response without a matching request") {
            repository.insert("orphan-response", sampleResponse()).await() shouldBe false
        }

        test("does not create a call when response has no matching request") {
            repository.insert("orphan-response", sampleResponse()).await()
            repository.getById("orphan-response") shouldBe null
        }

        test("leaves the call list empty when response has no matching request") {
            repository.insert("orphan-response", sampleResponse()).await()
            repository.getAll() shouldBe emptyList()
        }

        test("accepts a response update for an existing request") {
            repository.insert("response-update", sampleRequest()).await()

            repository.insert("response-update", sampleResponse(code = 202)).await() shouldBe true
        }

        test("response update does not add another call") {
            repository.insert("response-update", sampleRequest()).await()
            repository.insert("response-update", sampleResponse(code = 202)).await()

            repository.getAll().size shouldBe 1
        }

        test("stores the response on the existing call") {
            repository.insert("response-update", sampleRequest()).await()
            repository.insert("response-update", sampleResponse(code = 202)).await()

            (repository.getById("response-update")!!.response as NetworkResponse.Response).code shouldBe 202
        }
    }

    context("error insertion") {
        context("stored error fields") {
            beforeTest {
                repository.insert("error-fields", sampleRequest()).await()
                repository.insert(
                    "error-fields",
                    NetworkResponse.Error(
                        "connection failed",
                        "trace line 1\ntrace line 2",
                        Instant.fromEpochMilliseconds(789),
                    ),
                ).await()
            }

            test("round-trips the exception message") {
                (repository.getById("error-fields")!!.response as NetworkResponse.Error).exceptionMessage shouldBe
                        "connection failed"
            }

            test("round-trips the stack trace") {
                (repository.getById("error-fields")!!.response as NetworkResponse.Error).stackTrace shouldBe
                        "trace line 1\ntrace line 2"
            }

            test("round-trips the error timestamp") {
                (repository.getById("error-fields")!!.response as NetworkResponse.Error).timestamp shouldBe
                        Instant.fromEpochMilliseconds(789)
            }
        }

        test("rejects an error response without a matching request") {
            val error = NetworkResponse.Error("offline", "trace", Instant.fromEpochMilliseconds(5))

            repository.insert("missing-error-call", error).await() shouldBe false
        }

        test("does not create a call for an error without a matching request") {
            val error = NetworkResponse.Error("offline", "trace", Instant.fromEpochMilliseconds(5))
            repository.insert("missing-error-call", error).await()
            repository.getById("missing-error-call") shouldBe null
        }

        test("replaces an existing response with an error") {
            repository.insert("replace-response", sampleRequest()).await()
            repository.insert("replace-response", sampleResponse()).await()
            val error = NetworkResponse.Error("failed", "stack", Instant.fromEpochMilliseconds(10))
            repository.insert("replace-response", error).await()

            repository.getById("replace-response")!!.response shouldBe error
        }
    }

    context("lookup and ordering") {
        test("returns null for an unknown ID") {
            repository.getById("absent") shouldBe null
        }

        test("returns a call for a known ID") {
            repository.insert("known", sampleRequest()).await()

            repository.getById("known")!!.id shouldBe "known"
        }

        test("returns all calls in descending request timestamp order") {
            listOf(30L, 10L, 20L).forEach { timestamp ->
                repository.insert(timestamp.toString(), sampleRequest(timestamp = timestamp)).await()
            }

            repository.getAll().map(NetworkCall::id) shouldContainExactly listOf("30", "20", "10")
        }

        test("limits observed calls to the newest request timestamps") {
            listOf(10L, 30L, 20L).forEach { timestamp ->
                repository.insert(timestamp.toString(), sampleRequest(timestamp = timestamp)).await()
            }

            repository.observeCallUpdates(2).first().map(NetworkCall::id) shouldContainExactly listOf("30", "20")
        }

        test("rejects a non-positive observation limit") {
            shouldThrow<IllegalArgumentException> { repository.observeCallUpdates(0) }
        }

        test("rejects a negative observation limit") {
            shouldThrow<IllegalArgumentException> { repository.observeCallUpdates(-1) }
        }
    }

    context("observation") {
        test("networkCalls emits an initial empty snapshot") {
            repository.networkCalls.test {
                awaitItem() shouldBe emptyList()
                cancelAndIgnoreRemainingEvents()
            }
        }

        test("networkCalls emits inserted requests") {
            repository.networkCalls.test {
                awaitItem()
                repository.insert("observed", sampleRequest()).await()
                awaitItem().map(NetworkCall::id) shouldContainExactly listOf("observed")
                cancelAndIgnoreRemainingEvents()
            }
        }

        test("networkCalls emits inserted responses") {
            repository.insert("observed", sampleRequest()).await()
            repository.networkCalls.test {
                awaitItem()
                repository.insert("observed", sampleResponse(code = 201)).await()
                (awaitItem().single().response as NetworkResponse.Response).code shouldBe 201
                cancelAndIgnoreRemainingEvents()
            }
        }

        test("observeById emits the current call") {
            repository.insert("observed-id", sampleRequest()).await()
            repository.observeById("observed-id").test {
                awaitItem()!!.request.endpoint shouldBe "/0"
                cancelAndIgnoreRemainingEvents()
            }
        }

        test("observeById emits updated request contents") {
            repository.insert("observed-id", sampleRequest()).await()
            repository.observeById("observed-id").test {
                awaitItem()
                repository.insert("observed-id", sampleRequest(endpoint = "/changed")).await()
                awaitItem()!!.request.endpoint shouldBe "/changed"
                cancelAndIgnoreRemainingEvents()
            }
        }

        test("observeById emits null for an unknown ID") {
            repository.observeById("not-found").test {
                awaitItem() shouldBe null
                cancelAndIgnoreRemainingEvents()
            }
        }

        test("emits snapshots that do not share mutable body arrays") {
            val body = byteArrayOf(1, 2)
            repository.networkCalls.test {
                awaitItem() shouldBe emptyList()
                repository.insert("snapshot", sampleRequest(body = body)).await()
                val observedBody = awaitItem().single().request.body!!
                observedBody[0] = 9
                body[1] = 8

                repository.getById("snapshot")!!.request.body!!.toList() shouldContainExactly listOf(1, 2)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    context("removal") {
        test("remove returns false when the ID does not exist") {
            repository.remove("missing") shouldBe false
        }

        test("remove returns true for an existing call") {
            repository.insert("remove-me", sampleRequest()).await()

            repository.remove("remove-me") shouldBe true
        }

        test("remove deletes the call") {
            repository.insert("remove-me", sampleRequest()).await()
            repository.remove("remove-me")

            repository.getById("remove-me") shouldBe null
        }

        test("remove leaves no calls after deleting the only call") {
            repository.insert("remove-me", sampleRequest()).await()
            repository.remove("remove-me")

            repository.getAll() shouldBe emptyList()
        }

        test("remove cascades to request and response headers") {
            repository.insert("remove-with-headers", sampleRequest()).await()
            repository.insert("remove-with-headers", sampleResponse()).await()
            repository.remove("remove-with-headers")

            repository.insert("remove-with-headers", sampleRequest()).await() shouldBe true
        }
    }

    context("clearing") {
        test("clear empties getAll") {
            repository.insert("first", sampleRequest(timestamp = 1)).await()
            repository.insert("second", sampleRequest(timestamp = 2)).await()

            repository.clear()

            repository.getAll() shouldBe emptyList()
        }

        test("clear removes previously stored IDs") {
            repository.insert("first", sampleRequest(timestamp = 1)).await()
            repository.insert("second", sampleRequest(timestamp = 2)).await()
            repository.clear()

            repository.getById("first") shouldBe null
        }

        test("clear removes every previously stored ID") {
            repository.insert("first", sampleRequest(timestamp = 1)).await()
            repository.insert("second", sampleRequest(timestamp = 2)).await()
            repository.clear()

            repository.getById("second") shouldBe null
        }
    }
})

private fun sampleRequest(
    timestamp: Long = 0,
    scheme: String = "https",
    host: String = "example.com",
    port: Int? = null,
    endpoint: String = "/$timestamp",
    queryParams: String = "",
    method: NetworkMethod = NetworkMethod.POST,
    body: ByteArray? = null,
    headers: List<NetworkHeader> = listOf(NetworkHeader("Request", "header")),
) = NetworkRequest(
    scheme = scheme,
    host = host,
    port = port,
    endpoint = endpoint,
    queryParams = queryParams,
    method = method,
    body = body,
    headers = headers,
    timestamp = Instant.fromEpochMilliseconds(timestamp),
)

private fun sampleResponse(
    body: ByteArray? = null,
    headers: List<NetworkHeader> = listOf(NetworkHeader("Response", "header")),
    code: Int = 201,
    timestamp: Long = 11,
) = NetworkResponse.Response(
    body = body,
    headers = headers,
    code = code,
    timestamp = Instant.fromEpochMilliseconds(timestamp),
)
