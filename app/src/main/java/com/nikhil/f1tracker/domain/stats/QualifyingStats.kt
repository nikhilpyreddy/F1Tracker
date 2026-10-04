package com.nikhil.f1tracker.domain.stats

import com.nikhil.f1tracker.data.local.entity.QualifyingEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity

private const val Q3_CUTOFF = 10
private const val SECONDS_PER_MINUTE = 60

/** One line of a qualifying classification, with its time in the segment the driver reached. */
data class QualifyingLine(
    val driverId: String,
    val constructorId: String,
    val position: Int,
    val segment: String,
    val time: String?,
    /** Seconds behind the pole-sitter's time in the same segment. */
    val gapToPole: Double?,
)

fun qualifyingClassification(rows: List<QualifyingEntity>): List<QualifyingLine> {
    val sorted = rows.sortedBy { it.position }
    val pole = sorted.firstOrNull() ?: return emptyList()
    return sorted.map { row ->
        val (segment, time, poleTime) = when {
            row.q3 != null -> Triple("Q3", row.q3, pole.q3)
            row.q2 != null -> Triple("Q2", row.q2, pole.q2)
            else -> Triple("Q1", row.q1, pole.q1)
        }
        val gap = lapSeconds(time)?.let { mine -> lapSeconds(poleTime)?.let { mine - it } }
        QualifyingLine(row.driverId, row.constructorId, row.position, segment, time, gap)
    }
}

/** "1:35.130" -> 95.13. */
fun lapSeconds(time: String?): Double? {
    if (time.isNullOrBlank()) return null
    val parts = time.split(":")
    return when (parts.size) {
        1 -> parts[0].toDoubleOrNull()
        2 -> parts[0].toIntOrNull()?.let { minutes -> parts[1].toDoubleOrNull()?.let { minutes * SECONDS_PER_MINUTE + it } }
        else -> null
    }
}

data class QualifyingForm(
    val driverId: String,
    val constructorId: String,
    val averagePosition: Double,
    val poles: Int,
    val frontRows: Int,
    val q3Appearances: Rate,
    /** Newest first. */
    val recentPositions: List<Int>,
)

/** Season qualifying form per driver, best average position first. */
fun qualifyingForms(seasonRows: List<QualifyingEntity>, recentSessions: Int): List<QualifyingForm> =
    seasonRows.groupBy { it.driverId }
        .map { (driverId, rows) ->
            val newestFirst = rows.sortedByDescending { it.round }
            QualifyingForm(
                driverId = driverId,
                constructorId = newestFirst.first().constructorId,
                averagePosition = rows.map { it.position }.average(),
                poles = rows.count { it.position == 1 },
                frontRows = rows.count { it.position <= 2 },
                q3Appearances = rows.rateOf { it.position <= Q3_CUTOFF },
                recentPositions = newestFirst.take(recentSessions).map { it.position },
            )
        }
        .sortedBy { it.averagePosition }

/** Teammate qualifying battles on actual qualifying position (not grid, which includes penalties). */
fun qualifyingHeadToHeads(seasonRows: List<QualifyingEntity>): List<TeammateHeadToHead> =
    seasonRows.groupBy { Triple(it.season, it.round, it.constructorId) }
        .values
        .filter { it.size == 2 }
        .map { pair -> pair.sortedBy { it.driverId } }
        .groupBy { (first, second) -> Triple(first.constructorId, first.driverId, second.driverId) }
        .map { (key, sessions) ->
            val firstWins = sessions.count { (a, b) -> a.position < b.position }
            TeammateHeadToHead(key.first, key.second, key.third, firstWins to sessions.size - firstWins, 0 to 0)
        }
        .sortedBy { it.constructorId }

data class PoleRecord(
    val season: Int,
    val driverId: String,
    val constructorId: String,
    val poleTime: String?,
    /** How the pole-sitter finished the race (null if the race isn't cached). */
    val racePositionText: String?,
)

/** Pole-sitters at a circuit, newest first, joined to how they finished the race. */
fun poleHistory(qualifying: List<QualifyingEntity>, results: List<ResultEntity>): List<PoleRecord> {
    val finishes = results.associateBy { Triple(it.season, it.round, it.driverId) }
    return qualifying.filter { it.position == 1 }
        .sortedWith(compareByDescending<QualifyingEntity> { it.season }.thenByDescending { it.round })
        .map { pole ->
            PoleRecord(
                season = pole.season,
                driverId = pole.driverId,
                constructorId = pole.constructorId,
                poleTime = pole.q3 ?: pole.q2 ?: pole.q1,
                racePositionText = finishes[Triple(pole.season, pole.round, pole.driverId)]?.positionText,
            )
        }
}
