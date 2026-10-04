package com.nikhil.f1tracker.domain.stats

import com.nikhil.f1tracker.data.local.entity.ResultEntity

data class RaceOutcome(val round: Int, val grid: Int, val positionText: String)

data class DriverForm(
    val driverId: String,
    val constructorId: String,
    val seasonPoints: Double,
    /** Newest first. */
    val recent: List<RaceOutcome>,
    val retirements: Rate,
)

/** Season form per driver, ordered by points. [seasonResults] should be a single season. */
fun driverForms(seasonResults: List<ResultEntity>, recentRaces: Int): List<DriverForm> =
    seasonResults.groupBy { it.driverId }
        .map { (driverId, results) ->
            val newestFirst = results.sortedByDescending { it.round }
            DriverForm(
                driverId = driverId,
                constructorId = newestFirst.first().constructorId,
                seasonPoints = results.sumOf { it.points },
                recent = newestFirst.take(recentRaces).map { RaceOutcome(it.round, it.grid, it.positionText) },
                retirements = results.filter { it.didStart }.rateOf { it.isRetirement },
            )
        }
        .sortedByDescending { it.seasonPoints }

/** Wins are (first driver, second driver); drivers in a pair are ordered by id. */
data class TeammateHeadToHead(
    val constructorId: String,
    val firstDriverId: String,
    val secondDriverId: String,
    val gridWins: Pair<Int, Int>,
    val finishWins: Pair<Int, Int>,
)

/**
 * Race-by-race teammate comparisons. "Finish" follows the official classified order, which already
 * ranks retirements behind finishers. Races where a team ran other than two cars are skipped.
 */
fun teammateHeadToHeads(seasonResults: List<ResultEntity>): List<TeammateHeadToHead> =
    seasonResults.groupBy { Triple(it.season, it.round, it.constructorId) }
        .values
        .filter { it.size == 2 }
        .map { pair -> pair.sortedBy { it.driverId } }
        .groupBy { (first, second) -> Triple(first.constructorId, first.driverId, second.driverId) }
        .map { (key, races) ->
            TeammateHeadToHead(
                constructorId = key.first,
                firstDriverId = key.second,
                secondDriverId = key.third,
                gridWins = races.tally { it.effectiveGrid },
                finishWins = races.tally { it.classifiedOrder },
            )
        }
        .sortedBy { it.constructorId }

/** Counts races where each driver had the lower (better) value; ties count for neither. */
private fun List<List<ResultEntity>>.tally(rank: (ResultEntity) -> Int): Pair<Int, Int> {
    val firstWins = count { (first, second) -> rank(first) < rank(second) }
    val secondWins = count { (first, second) -> rank(second) < rank(first) }
    return firstWins to secondWins
}
