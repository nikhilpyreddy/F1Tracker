package com.nikhil.f1tracker.ui.weekend

import com.nikhil.f1tracker.domain.stats.CircuitStat
import com.nikhil.f1tracker.domain.stats.CircuitStats
import com.nikhil.f1tracker.domain.stats.StatDetailRace
import com.nikhil.f1tracker.domain.stats.RaceOutcome
import com.nikhil.f1tracker.domain.stats.Rate

enum class WeekendTab(val label: String) { CIRCUIT("Circuit"), TRACK("Track"), FORM("Form") }

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
)

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
