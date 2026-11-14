package peekniko.internal.formatting

import kotlin.time.Clock
import kotlin.time.Instant

internal expect fun Instant.toReadableTime(includeMillis: Boolean = false, now: Instant = Clock.System.now()): String
