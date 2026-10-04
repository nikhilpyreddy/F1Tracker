package com.nikhil.f1tracker.domain.stats

import com.nikhil.f1tracker.data.local.entity.ResultEntity

private const val TOP_TEN = 10
private const val PODIUM_PLACES = 3

/**
 * Historical base rates at one circuit, chosen to line up with the prediction markets they inform:
 * winner (by grid slot), podium, top 10, and retirements.
 */
data class CircuitStats(
    val raceCount: Int,
    val seasons: List<Int>,
    val winsFromPole: Rate,
    val winsFromFrontRow: Rate,
    val winsFromTopThree: Rate,
    val winnerAverageGrid: Double?,
    val podiumsFromTopThreeGrid: Rate,
    val topTenFinishersFromOutsideTopTenPerRace: Double?,
    val retirements: Rate,
    /** Over races with fastest-lap data: was it set by the winner / a podium finisher? */
    val fastestLapByWinner: Rate,
    val fastestLapByPodium: Rate,
)

fun circuitStats(results: List<ResultEntity>): CircuitStats {
    val races = results.groupBy { it.season to it.round }.values.toList()
    val winners = races.mapNotNull { race -> race.firstOrNull { it.isWinner } }
    val podiumFinishers = results.filter { it.positionText.toIntOrNull() in 1..PODIUM_PLACES }
    val starters = results.filter { it.didStart }
    val winnerGrids = winners.filter { it.grid > 0 }.map { it.grid }
    val fastestLaps = results.filter { it.fastestLapRank == 1 }
    return CircuitStats(
        raceCount = races.size,
        seasons = races.map { it.first().season }.distinct().sorted(),
        winsFromPole = winners.rateOf { it.grid == 1 },
        winsFromFrontRow = winners.rateOf { it.grid in 1..2 },
        winsFromTopThree = winners.rateOf { it.grid in 1..PODIUM_PLACES },
        winnerAverageGrid = winnerGrids.takeIf { it.isNotEmpty() }?.average(),
        podiumsFromTopThreeGrid = podiumFinishers.rateOf { it.grid in 1..PODIUM_PLACES },
        topTenFinishersFromOutsideTopTenPerRace = races.takeIf { it.isNotEmpty() }?.let {
            results.count { it.positionText.toIntOrNull() in 1..TOP_TEN && it.effectiveGrid > TOP_TEN }
                .toDouble() / it.size
        },
        retirements = starters.rateOf { it.isRetirement },
        fastestLapByWinner = fastestLaps.rateOf { it.isWinner },
        fastestLapByPodium = fastestLaps.rateOf { it.positionText.toIntOrNull() in 1..PODIUM_PLACES },
    )
}
