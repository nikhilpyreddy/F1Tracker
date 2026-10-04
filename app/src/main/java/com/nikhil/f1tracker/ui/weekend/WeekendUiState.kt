package com.nikhil.f1tracker.ui.weekend

import com.nikhil.f1tracker.domain.stats.CircuitStat
import com.nikhil.f1tracker.domain.stats.CircuitStats
import com.nikhil.f1tracker.domain.stats.StatDetailRace
import com.nikhil.f1tracker.domain.stats.RaceOutcome
import com.nikhil.f1tracker.domain.stats.Rate

enum class WeekendTab(val label: String) { CIRCUIT("Circuit"), QUALIFYING("Qualifying"), TRACK("Track"), STRATEGY("Strategy"), FORM("Form") }

data class WeekendUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val season: Int = 0,
    val raceName: String = "",
    val date: String = "",
    val circuitId: String = "",
    val circuitName: String = "",
    val selectedTab: WeekendTab = WeekendTab.CIRCUIT,
    val circuitStats: CircuitStats? = null,
    val driverForms: List<DriverFormRow> = emptyList(),
    val headToHeads: List<HeadToHeadRow> = emptyList(),
    val statSheet: StatSheet? = null,
    /** Podium of the most recent race at this circuit. */
    val lastPodium: List<ClassificationRow> = emptyList(),
    val lastRaceSeason: Int? = null,
    val resultsSheet: ResultsSheet? = null,
    /** This season's drivers, for picking whose history at this circuit to show. */
    val historyDrivers: List<HistoryDriver> = emptyList(),
    val selectedHistoryDriverId: String? = null,
    /** The selected driver's results here, newest first. */
    val driverHistory: List<DriverCircuitResult> = emptyList(),
)

data class HistoryDriver(val driverId: String, val name: String)

data class DriverCircuitResult(
    val season: Int,
    val constructorId: String,
    val positionText: String,
    val grid: Int,
    val points: Double,
    val status: String,
)

/** One driver's line in a race classification. */
data class ClassificationRow(
    val driverId: String,
    val constructorId: String,
    val driverName: String,
    val teamName: String,
    val positionText: String,
    val grid: Int,
    val points: Double,
    val status: String,
)

/** Full classification of this circuit's race in [season], switchable across [seasons]. */
data class ResultsSheet(val season: Int, val seasons: List<Int>, val rows: List<ClassificationRow>)

/** An opened circuit stat: the races behind it, with names for the drivers involved. */
data class StatSheet(
    val stat: CircuitStat,
    val races: List<StatDetailRace>,
    val driverNames: Map<String, String>,
)

data class DriverFormRow(
    val driverId: String,
    val driverName: String,
    val constructorId: String,
    val teamName: String,
    val seasonPoints: Double,
    /** Newest first. */
    val recent: List<RaceOutcome>,
    val retirements: Rate,
)

data class HeadToHeadRow(
    val constructorId: String,
    val teamName: String,
    val firstDriverId: String,
    val firstDriverName: String,
    val secondDriverId: String,
    val secondDriverName: String,
    val gridWins: Pair<Int, Int>,
    val finishWins: Pair<Int, Int>,
)
