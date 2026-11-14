package peekniko.internal.formatting

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Instant

internal actual fun Instant.toReadableTime(includeMillis: Boolean, now: Instant): String {
    val value = this.asCalendar()
    val time = if (includeMillis) {
        value.formatTimeUpToMillis()
    } else {
        value.formatTimeUpToSeconds()
    }

    val today = now.asCalendar()
    val isToday = value.isSameDay(today)
    if (isToday) {
        return time
    }
    return today.formatDateWithTime(time)
}

private fun Calendar.formatDateWithTime(time: String) = String.format(
    Locale.ROOT, "%04d-%02d-%02d %s",
    get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH), time,
)

private fun Calendar.formatTimeUpToMillis() = String.format(
    Locale.ROOT, "%02d:%02d:%02d.%03d",
    get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), get(Calendar.SECOND), get(Calendar.MILLISECOND),
)

private fun Calendar.formatTimeUpToSeconds() = String.format(
    Locale.ROOT, "%02d:%02d:%02d",
    get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), get(Calendar.SECOND),
)

private fun Calendar.isSameDay(other: Calendar): Boolean {
    return this.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            this.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

private fun Instant.asCalendar() = Calendar.getInstance(TimeZone.getDefault(), Locale.ROOT)
    .apply { timeInMillis = toEpochMilliseconds() }
