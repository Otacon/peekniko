package peekniko.internal.formatting

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import kotlin.time.Instant

internal actual fun Instant.toReadableTime(includeMillis: Boolean, now: Instant): String {
    fun components(instant: Instant) = NSCalendar.currentCalendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or
            NSCalendarUnitMinute or NSCalendarUnitSecond,
        fromDate = NSDate(timeIntervalSinceReferenceDate = instant.epochSeconds.toDouble() + instant.nanosecondsOfSecond / 1_000_000_000.0 - 978_307_200.0),
    )
    val value = components(this)
    val today = components(now)
    val time = buildString {
        append(value.hour.toInt().toString().padStart(2, '0'))
        append(':'); append(value.minute.toInt().toString().padStart(2, '0'))
        append(':'); append(value.second.toInt().toString().padStart(2, '0'))
        if (includeMillis) {
            append('.'); append((nanosecondsOfSecond / 1_000_000).toString().padStart(3, '0'))
        }
    }
    return if (value.year == today.year && value.month == today.month && value.day == today.day) time
    else "${value.year.toInt().toString().padStart(4, '0')}-${value.month.toInt().toString().padStart(2, '0')}-${value.day.toInt().toString().padStart(2, '0')} $time"
}
