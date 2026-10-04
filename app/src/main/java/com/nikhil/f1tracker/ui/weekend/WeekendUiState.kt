package com.nikhil.f1tracker.ui.weekend

import com.nikhil.f1tracker.domain.stats.CircuitStats
import com.nikhil.f1tracker.domain.stats.RaceOutcome
import com.nikhil.f1tracker.domain.stats.Rate

enum class WeekendTab(val label: String) { CIRCUIT("Circuit"), FORM("Form") }

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
)

data class DriverFormRow(
    val driverId: String,
    val driverName: String,
    val teamName: String,
    val seasonPoints: Double,
    /** Newest first. */
    val recent: List<RaceOutcome>,
    val retirements: Rate,
)

data class HeadToHeadRow(
    val teamName: String,
    val firstDriverName: String,
    val secondDriverName: String,
    val gridWins: Pair<Int, Int>,
    val finishWins: Pair<Int, Int>,
)
