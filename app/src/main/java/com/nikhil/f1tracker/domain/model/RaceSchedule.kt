package com.nikhil.f1tracker.domain.model

import com.nikhil.f1tracker.data.local.entity.RaceEntity
import java.time.Instant
import java.time.LocalDate

enum class RaceStatus { COMPLETED, NEXT, UPCOMING }

/**
 * Over once the race has had time to finish. Without a published start time, fall back to the
 * date: over from the next day in app time. An unparseable date counts as over rather than hiding
 * the calendar.
 */
fun RaceEntity.isOver(now: Instant): Boolean {
    sessionStart(date, time)?.let { return it.plus(RACE_DURATION) < now }
    val today = now.atZone(APP_ZONE).toLocalDate()
    return runCatching { LocalDate.parse(date) < today }.getOrDefault(true)
}

/** Each race of a season in round order with its status; the first unfinished race is NEXT. */
fun raceStatuses(races: List<RaceEntity>, now: Instant): List<Pair<RaceEntity, RaceStatus>> {
    val sorted = races.sortedBy { it.round }
    val nextRound = sorted.firstOrNull { !it.isOver(now) }?.round
    return sorted.map { race ->
        race to when {
            race.round == nextRound -> RaceStatus.NEXT
            !race.isOver(now) -> RaceStatus.UPCOMING
            else -> RaceStatus.COMPLETED
        }
    }
}
