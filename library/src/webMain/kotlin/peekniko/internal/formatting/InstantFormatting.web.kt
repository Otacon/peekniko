package peekniko.internal.formatting

import kotlin.time.Instant

internal actual fun Instant.toReadableTime(includeMillis: Boolean, now: Instant): String =
    formatReadableTime(toEpochMilliseconds().toDouble(), now.toEpochMilliseconds().toDouble(), includeMillis)

@OptIn(ExperimentalWasmJsInterop::class)
private fun formatReadableTime(millis: Double, nowMillis: Double, includeMillis: Boolean): String = js(
    """{
        const date = new Date(millis);
        const today = new Date(nowMillis);
        const pad = (value, size = 2) => String(value).padStart(size, '0');
        const time = pad(date.getHours()) + ':' + pad(date.getMinutes()) + ':' + pad(date.getSeconds()) +
            (includeMillis ? '.' + pad(date.getMilliseconds(), 3) : '');
        return date.getFullYear() === today.getFullYear() &&
            date.getMonth() === today.getMonth() && date.getDate() === today.getDate()
            ? time
            : pad(date.getFullYear(), 4) + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate()) + ' ' + time;
    }""",
)
