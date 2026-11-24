package peekniko.internal.formatting

import java.time.Instant as JavaInstant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

internal actual fun Instant.toReadableTime(includeMillis: Boolean, now: Instant): String =
    formatReadableTime(includeMillis, now)

private fun Instant.formatReadableTime(includeMillis: Boolean, now: Instant): String {
    val zone = ZoneId.systemDefault()
    val dateTime = JavaInstant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong()).atZone(zone)
    val today = JavaInstant.ofEpochSecond(now.epochSeconds, now.nanosecondsOfSecond.toLong()).atZone(zone).toLocalDate()
    val pattern = if (includeMillis) "HH:mm:ss.SSS" else "HH:mm:ss"
    val time = dateTime.format(DateTimeFormatter.ofPattern(pattern))
    return if (dateTime.toLocalDate() == today) time else "${dateTime.toLocalDate()} $time"
}
