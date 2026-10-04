package com.nikhil.f1tracker.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Every date and time in the app is shown in US Central time. This is a zone, not a fixed offset,
 * so it follows daylight saving (CDT in summer, CST in winter) and the label says which.
 */
val APP_ZONE: ZoneId = ZoneId.of("America/Chicago")

/** Races rarely run past two hours; after this a started race counts as done. */
val RACE_DURATION: Duration = Duration.ofHours(3)

private val DATE_TIME = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a zzz", Locale.US).withZone(APP_ZONE)
private val SHORT_DATE = DateTimeFormatter.ofPattern("MMM d", Locale.US).withZone(APP_ZONE)
private val LONG_DATE = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US).withZone(APP_ZONE)

/** A session start from Jolpica's UTC date ("2026-10-11") and time ("12:00:00Z"). Null if no time. */
fun sessionStart(date: String?, time: String?): Instant? {
    if (date.isNullOrBlank() || time.isNullOrBlank()) return null
    return runCatching { Instant.parse("${date}T$time".let { if (it.endsWith("Z")) it else "${it}Z" }) }.getOrNull()
}

/** "Sun, Oct 11 · 7:00 AM CDT" */
fun formatDateTime(instant: Instant): String = DATE_TIME.format(instant)

/** "Oct 11", in Central time when the start time is known (a late-evening UTC start can be the day before). */
fun formatShortDate(date: String, time: String?): String =
    sessionStart(date, time)?.let { SHORT_DATE.format(it) }
        ?: runCatching { LocalDate.parse(date).format(SHORT_DATE) }.getOrDefault(date)

/** "Sun, Oct 11 · 7:00 AM CDT", or "Sun, Oct 11, 2026" when only the date is known. */
fun formatRaceWhen(date: String, time: String?): String =
    sessionStart(date, time)?.let(::formatDateTime)
        ?: runCatching { LocalDate.parse(date).format(LONG_DATE) }.getOrDefault(date)

/** "in 2d 5h", "in 3h 20m", "now". */
fun formatCountdown(from: Instant, to: Instant): String {
    val duration = Duration.between(from, to)
    if (duration.isNegative || duration.isZero) return "now"
    return when {
        duration.toDays() > 0 -> "in ${duration.toDays()}d ${duration.toHoursPart()}h"
        duration.toHours() > 0 -> "in ${duration.toHours()}h ${duration.toMinutesPart()}m"
        else -> "in ${duration.toMinutes()}m"
    }
}
