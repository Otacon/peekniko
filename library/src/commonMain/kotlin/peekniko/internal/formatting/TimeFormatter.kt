package peekniko.internal.formatting

import kotlin.time.Clock
import kotlin.time.Instant

internal interface TimeFormatter {
    fun format(instant: Instant): String
}

internal class ReadableTimeFormatter(private val includeMillis: Boolean = false) : TimeFormatter {
    override fun format(instant: Instant): String = instant.toReadableTime(includeMillis = includeMillis)
}

internal expect fun Instant.toReadableTime(includeMillis: Boolean = false, now: Instant = Clock.System.now()): String
