package com.nikhil.f1tracker.domain.stats

import com.nikhil.f1tracker.data.local.entity.ResultEntity

private const val TOP_TEN = 10
private const val PODIUM_PLACES = 3

/** The circuit stats that can be tapped open to see the races behind them. */
enum class CircuitStat(val title: String) {
    WINS_FROM_POLE("Won from pole"),
    WINS_FROM_FRONT_ROW("Won from the front row"),
    WINS_FROM_TOP_THREE("Won from the top 3"),
    WINNER_GRID("Winner's average grid"),
    PODIUM_STARTS("Podium finishers who started top 3"),
    TOP_TEN_FROM_OUTSIDE("Top-10 finishers from outside the top-10 grid"),
    RETIREMENTS("Retirements"),
    FASTEST_LAP_WINNER("Fastest lap set by the winner"),
    FASTEST_LAP_PODIUM("Fastest lap set by a podium finisher"),
}

/** One driver's line in a race breakdown. [counts] marks whether this line supports the stat. */
data class StatDetailLine(
    val driverId: String,
    val constructorId: String,
    val grid: Int,
    val positionText: String,
    val status: String,
    val counts: Boolean?,
)

/** One race in a breakdown. [counts] is set for per-race rates (did this race count as a hit?). */
data class StatDetailRace(
    val season: Int,
    val round: Int,
    val counts: Boolean?,
    val lines: List<StatDetailLine>,
)

/** The races behind [stat], newest first. */
fun circuitStatDetails(stat: CircuitStat, results: List<ResultEntity>): List<StatDetailRace> =
    results.groupBy { it.season to it.round }
        .toSortedMap(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
        .map { (key, race) -> raceDetail(stat, key.first, key.second, race) }

private fun raceDetail(stat: CircuitStat, season: Int, round: Int, race: List<ResultEntity>): StatDetailRace {
    val winner = race.firstOrNull { it.isWinner }
    return when (stat) {
        CircuitStat.WINS_FROM_POLE -> winnerVsPole(season, round, race, winner, maxGrid = 1)
        CircuitStat.WINS_FROM_FRONT_ROW -> winnerVsPole(season, round, race, winner, maxGrid = 2)
        CircuitStat.WINS_FROM_TOP_THREE -> winnerVsPole(season, round, race, winner, maxGrid = PODIUM_PLACES)
        CircuitStat.WINNER_GRID -> StatDetailRace(season, round, null, listOfNotNull(winner).map { it.line(null) })
        CircuitStat.PODIUM_STARTS -> StatDetailRace(
            season, round, null,
            race.filter { it.positionText.toIntOrNull() in 1..PODIUM_PLACES }
                .sortedBy { it.classifiedOrder }
                .map { it.line(counts = it.grid in 1..PODIUM_PLACES) },
        )
        CircuitStat.TOP_TEN_FROM_OUTSIDE -> StatDetailRace(
            season, round, null,
            race.filter { it.positionText.toIntOrNull() in 1..TOP_TEN && it.effectiveGrid > TOP_TEN }
                .sortedBy { it.classifiedOrder }
                .map { it.line(counts = true) },
        )
        CircuitStat.FASTEST_LAP_WINNER, CircuitStat.FASTEST_LAP_PODIUM -> {
            val setter = race.firstOrNull { it.fastestLapRank == 1 }
            val counts = setter?.let {
                if (stat == CircuitStat.FASTEST_LAP_WINNER) it.isWinner else it.positionText.toIntOrNull() in 1..PODIUM_PLACES
            }
            StatDetailRace(season, round, counts, listOfNotNull(setter?.line(counts), winner?.takeIf { it != setter }?.line(null)))
        }
        CircuitStat.RETIREMENTS -> StatDetailRace(
            season, round, null,
            race.filter { it.isRetirement }.sortedBy { it.classifiedOrder }.map { it.line(counts = true) },
        )
    }
}

/** The winner, plus the pole-sitter when they didn't win, so a miss shows who lost it from pole. */
private fun winnerVsPole(
    season: Int,
    round: Int,
    race: List<ResultEntity>,
    winner: ResultEntity?,
    maxGrid: Int,
): StatDetailRace {
    val poleSitter = race.firstOrNull { it.grid == 1 }?.takeIf { it != winner }
    val counts = winner?.let { it.grid in 1..maxGrid }
    return StatDetailRace(
        season, round, counts,
        listOfNotNull(winner?.line(counts), poleSitter?.line(null)),
    )
}

private fun ResultEntity.line(counts: Boolean?) =
    StatDetailLine(driverId, constructorId, grid, positionText, status, counts)
