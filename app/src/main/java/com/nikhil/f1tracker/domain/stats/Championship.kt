package com.nikhil.f1tracker.domain.stats

import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.domain.model.isOver
import com.nikhil.f1tracker.domain.model.sessionStart
import java.time.Instant

// Current points system (2025 onwards): race 25-18-15-12-10-8-6-4-2-1, sprint 8-7-…-1, no
// fastest-lap point. Not available from any API, so update here if the rules change.
private const val RACE_WIN_POINTS = 25
private const val RACE_SECOND_POINTS = 18
private const val SPRINT_WIN_POINTS = 8
private const val SPRINT_SECOND_POINTS = 7

/** What's still to be raced for in a season. */
data class SeasonRemaining(val races: Int, val sprints: Int) {
    /** Most one driver can still score: win everything. */
    val maxDriverPoints: Int get() = races * RACE_WIN_POINTS + sprints * SPRINT_WIN_POINTS

    /** Most one team can still score: a 1-2 everywhere. */
    val maxTeamPoints: Int get() =
        races * (RACE_WIN_POINTS + RACE_SECOND_POINTS) + sprints * (SPRINT_WIN_POINTS + SPRINT_SECOND_POINTS)
}

fun seasonRemaining(races: List<RaceEntity>, now: Instant): SeasonRemaining = SeasonRemaining(
    races = races.count { !it.isOver(now) },
    sprints = races.count { race ->
        val sprint = sessionStart(race.sprintDate, race.sprintTime)
        if (sprint != null) sprint > now else race.sprintDate != null && !race.isOver(now)
    },
)

/** A contender's title maths. "Alive" means they can at least tie the leader by winning everything left. */
data class TitleOutlook(val gapToLeader: Double, val maxPossible: Double, val isAlive: Boolean)

fun titleOutlook(points: Double, leaderPoints: Double, maxStillAvailable: Int): TitleOutlook {
    val maxPossible = points + maxStillAvailable
    return TitleOutlook(gapToLeader = leaderPoints - points, maxPossible = maxPossible, isAlive = maxPossible >= leaderPoints)
}
