package peekniko.playground

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

internal class OkHttpPlaygroundClient(private val client: OkHttpClient) : PlaygroundHttpClient {

    override suspend fun execute(baseUrl: String, request: PlaygroundRequest): PlaygroundResponse =
        withContext(Dispatchers.IO) {
            val contentType = request.headers
                .firstOrNull { it.name.equals("Content-Type", ignoreCase = true) }
                ?.value
                ?.toMediaTypeOrNull()
            val requestBody = request.body?.toRequestBody(contentType)
            val httpRequest = Request.Builder()
                .url(baseUrl.removeSuffix("/") + request.path)
                .apply { request.headers.forEach { header(it.name, it.value) } }
                .method(request.method.name, requestBody)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                PlaygroundResponse(
                    statusCode = response.code,
                    headers = response.headers.map { (name, value) -> PlaygroundHeader(name, value) },
                    body = response.body.string(),
                )
            }
        }

    override fun close() {
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }
}
