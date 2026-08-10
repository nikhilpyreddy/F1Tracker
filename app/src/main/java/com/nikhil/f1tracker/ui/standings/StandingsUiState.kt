package com.nikhil.f1tracker.ui.standings

enum class StandingsMode { DRIVERS, TEAMS }

data class StandingsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val loadErrorMessage: String? = null,
    val mode: StandingsMode = StandingsMode.DRIVERS,
    val driverStandings: List<DriverStandingRow> = emptyList(),
    val constructorStandings: List<ConstructorStandingRow> = emptyList(),
)

data class DriverStandingRow(
    val driverId: String,
    val position: Int,
    val driverName: String,
    val teamName: String?,
    val points: Double,
    val wins: Int,
)

data class ConstructorStandingRow(
    val constructorId: String,
    val position: Int,
    val teamName: String,
    val points: Double,
    val wins: Int,
)
