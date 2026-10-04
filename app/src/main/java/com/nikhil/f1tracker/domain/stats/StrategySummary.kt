package com.nikhil.f1tracker.domain.stats

import com.nikhil.f1tracker.data.repository.RaceSessionData

/** A stint on one compound, by lap range. */
data class Stint(val compound: String, val laps: Int)

/** One driver's race: finishing order (from Jolpica), code, and tyre stints. */
data class DriverStrategy(
    val driverId: String?,
    val code: String,
    val positionText: String?,
    val stints: List<Stint>,
) {
    val stops: Int get() = (stints.size - 1).coerceAtLeast(0)
    val sequence: String get() = stints.joinToString("–") { it.compound.take(1) }
}

data class RaceStrategy(
    val year: Int,
    val drivers: List<DriverStrategy>,
    val safetyCars: Int,
    val virtualSafetyCars: Int,
    val redFlags: Int,
    val overtakes: Int,
    val medianPitLaneSeconds: Double?,
)

data class CommonStrategy(val sequence: String, val stops: Int, val drivers: Int, val winners: Int)

data class CompoundStints(val compound: String, val averageLaps: Double, val longestLaps: Int, val stints: Int)

data class StrategySummary(
    val races: List<RaceStrategy>,
    val commonStrategies: List<CommonStrategy>,
    /** Share of finishers by number of stops, e.g. 1 -> 0.6. */
    val stopShares: Map<Int, Double>,
    val compoundStints: List<CompoundStints>,
    val medianFirstStopLap: Int?,
    val racesWithSafetyCar: Rate,
    val racesWithVirtualSafetyCar: Rate,
    val racesWithRedFlag: Rate,
    val averageOvertakes: Double?,
)

private const val COMMON_STRATEGIES_SHOWN = 6
private val COMPOUND_ORDER = listOf("SOFT", "MEDIUM", "HARD", "INTERMEDIATE", "WET")

/**
 * @param positionsByYear finishing position text per (year -> driver code), from Jolpica results,
 *   used to order the field and pick out winners.
 * @param driverIdsByYear Jolpica driverId per (year -> driver code), for team colours.
 */
fun strategySummary(
    sessions: List<RaceSessionData>,
    positionsByYear: Map<Int, Map<String, String>>,
    driverIdsByYear: Map<Int, Map<String, String>>,
): StrategySummary {
    val races = sessions.map { raceStrategy(it, positionsByYear[it.year].orEmpty(), driverIdsByYear[it.year].orEmpty()) }
    val finishers = races.flatMap { race -> race.drivers.filter { it.positionText?.toIntOrNull() != null && it.stints.isNotEmpty() } }
    val allStints = races.flatMap { race -> race.drivers.flatMap { it.stints } }
    return StrategySummary(
        races = races,
        commonStrategies = finishers.groupBy { it.sequence }
            .map { (sequence, group) ->
                CommonStrategy(sequence, group.first().stops, group.size, group.count { it.positionText == "1" })
            }
            .sortedByDescending { it.drivers }
            .take(COMMON_STRATEGIES_SHOWN),
        stopShares = finishers.groupingBy { it.stops }.eachCount()
            .mapValues { (_, count) -> count.toDouble() / finishers.size }
            .toSortedMap(),
        compoundStints = allStints.groupBy { it.compound }
            .map { (compound, stints) -> CompoundStints(compound, stints.map { it.laps }.average(), stints.maxOf { it.laps }, stints.size) }
            .sortedBy { COMPOUND_ORDER.indexOf(it.compound).let { i -> if (i < 0) Int.MAX_VALUE else i } },
        medianFirstStopLap = finishers.filter { it.stops > 0 }.map { it.stints.first().laps }.median()?.toInt(),
        racesWithSafetyCar = races.rateOf { it.safetyCars > 0 },
        racesWithVirtualSafetyCar = races.rateOf { it.virtualSafetyCars > 0 },
        racesWithRedFlag = races.rateOf { it.redFlags > 0 },
        averageOvertakes = races.takeIf { it.isNotEmpty() }?.map { it.overtakes }?.average(),
    )
}

private fun raceStrategy(session: RaceSessionData, positions: Map<String, String>, driverIds: Map<String, String>): RaceStrategy {
    val codesByNumber = session.drivers.associate { it.driverNumber to it.nameAcronym }
    val drivers = session.stints.groupBy { it.driverNumber }.mapNotNull { (number, stints) ->
        val code = codesByNumber[number] ?: return@mapNotNull null
        DriverStrategy(
            driverId = driverIds[code],
            code = code,
            positionText = positions[code],
            stints = stints.sortedBy { it.stintNumber }.mapNotNull { stint ->
                val start = stint.lapStart ?: return@mapNotNull null
                val end = stint.lapEnd ?: return@mapNotNull null
                Stint(stint.compound ?: "UNKNOWN", end - start + 1)
            },
        )
    }
    val messages = session.incidents.mapNotNull { it.message?.uppercase() }
    return RaceStrategy(
        year = session.year,
        drivers = drivers.sortedBy { it.positionText?.toIntOrNull() ?: Int.MAX_VALUE },
        safetyCars = messages.count { it.startsWith("SAFETY CAR DEPLOYED") },
        virtualSafetyCars = messages.count { it.startsWith("VIRTUAL SAFETY CAR DEPLOYED") },
        redFlags = session.incidents.count { it.flag == "RED" },
        overtakes = session.overtakeCount,
        medianPitLaneSeconds = session.pitStops.mapNotNull { it.laneDuration }.median(),
    )
}

private fun List<Number>.median(): Double? {
    if (isEmpty()) return null
    val sorted = map { it.toDouble() }.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
}
