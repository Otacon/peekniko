package peekniko.playground

internal val connectionFailureRequest: PlaygroundRequest = PlaygroundRequest(
    method = PlaygroundMethod.GET,
    path = "/connection-failure",
    headers = listOf(PlaygroundHeader("X-Request-Id", "connection-failure")),
)

internal val playgroundRequests: List<PlaygroundRequest> = listOf(
    PlaygroundRequest(
        method = PlaygroundMethod.GET,
        path = "/get?scenario=list-items",
        headers = listOf(
            PlaygroundHeader("Accept", "application/json"),
            PlaygroundHeader("X-Request-Id", "list-items"),
        ),
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.POST,
        path = "/post",
        headers = listOf(
            PlaygroundHeader("Accept", "application/json"),
            PlaygroundHeader("Content-Type", "application/json; charset=utf-8"),
            PlaygroundHeader("X-Request-Id", "create-item"),
        ),
        body = """{"name":"created from playground","active":true}""",
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.PATCH,
        path = "/patch",
        headers = listOf(
            PlaygroundHeader("Accept", "application/json"),
            PlaygroundHeader("Content-Type", "application/json; charset=utf-8"),
            PlaygroundHeader("X-Request-Id", "patch-item"),
        ),
        body = """{"name":"updated from playground"}""",
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.PUT,
        path = "/put",
        headers = listOf(
            PlaygroundHeader("Accept", "application/json"),
            PlaygroundHeader("Content-Type", "application/x-www-form-urlencoded"),
            PlaygroundHeader("X-Request-Id", "update-form"),
        ),
        body = "name=playground+item&source=android-app",
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.POST,
        path = "/post",
        headers = listOf(
            PlaygroundHeader("Accept", "application/json"),
            PlaygroundHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8"),
            PlaygroundHeader("X-Request-Id", "form-echo"),
        ),
        body = "name=playground+user&status=ready",
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.GET,
        path = "/status/500",
        headers = listOf(
            PlaygroundHeader("Accept", "application/json"),
            PlaygroundHeader("X-Request-Id", "server-error"),
        ),
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.POST,
        path = "/status/422",
        headers = listOf(
            PlaygroundHeader("Accept", "application/json"),
            PlaygroundHeader("Content-Type", "application/json; charset=utf-8"),
            PlaygroundHeader("X-Request-Id", "validation-error"),
        ),
        body = """{"name":""}""",
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.GET,
        path = "/xml",
        headers = listOf(
            PlaygroundHeader("Accept", "application/xml"),
            PlaygroundHeader("X-Request-Id", "xml-catalog"),
        ),
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.GET,
        path = "/html",
        headers = listOf(
            PlaygroundHeader("Accept", "text/html"),
            PlaygroundHeader("X-Request-Id", "html-page"),
        ),
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.GET,
        path = "/robots.txt",
        headers = listOf(
            PlaygroundHeader("Accept", "text/plain"),
            PlaygroundHeader("X-Request-Id", "plain-text"),
        ),
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.GET,
        path = "/image/png",
        headers = listOf(
            PlaygroundHeader("Accept", "image/png"),
            PlaygroundHeader("X-Request-Id", "image-png"),
        ),
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.GET,
        path = "/image/jpeg",
        headers = listOf(
            PlaygroundHeader("Accept", "image/jpeg"),
            PlaygroundHeader("X-Request-Id", "image-jpeg"),
        ),
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.GET,
        path = "/image/webp",
        headers = listOf(
            PlaygroundHeader("Accept", "image/webp"),
            PlaygroundHeader("X-Request-Id", "image-webp"),
        ),
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.GET,
        path = "/image/svg",
        headers = listOf(
            PlaygroundHeader("Accept", "image/svg+xml"),
            PlaygroundHeader("X-Request-Id", "image-svg"),
        ),
    ),
    PlaygroundRequest(
        method = PlaygroundMethod.GET,
        path = "/delay/1",
        headers = listOf(
            PlaygroundHeader("Accept", "application/json"),
            PlaygroundHeader("X-Request-Id", "delayed-response"),
        ),
    ),
)
