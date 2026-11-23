package peekniko.internal.formatting

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

internal class TimeFormatterPlatformTest : FunSpec({
    val instant = Instant.parse("2024-05-05T12:34:56.789Z")

    test("formats a same-day time without milliseconds") {
        instant.toReadableTime(now = instant) shouldMatchRegex TIME_WITHOUT_MILLIS
    }

    test("formats a same-day time with milliseconds") {
        instant.toReadableTime(includeMillis = true, now = instant) shouldMatchRegex TIME_WITH_MILLIS
    }

    test("includes the date for a different day") {
        instant.toReadableTime(includeMillis = true, now = instant + 2.days) shouldMatchRegex DATE_AND_TIME_WITH_MILLIS
    }
})

private infix fun String.shouldMatchRegex(pattern: Regex) {
    pattern.matches(this) shouldBe true
}

private val TIME_WITHOUT_MILLIS = Regex("\\d{2}:\\d{2}:\\d{2}")
private val TIME_WITH_MILLIS = Regex("\\d{2}:\\d{2}:\\d{2}\\.\\d{3}")
private val DATE_AND_TIME_WITH_MILLIS = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}")
