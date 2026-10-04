package com.nikhil.f1tracker.ui.home

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val loadErrorMessage: String? = null,
    val nextRace: UpcomingRace? = null,
    val calendar: List<CalendarRace> = emptyList(),
    val favoriteDrivers: List<FavoriteDriverStanding> = emptyList(),
    val favoriteTeams: List<FavoriteTeamStanding> = emptyList(),
)

data class UpcomingRace(
    val raceName: String,
    val date: String,
    val round: Int,
    val circuitId: String,
    val season: Int,
)

enum class RaceStatus { COMPLETED, NEXT, UPCOMING }

data class CalendarRace(
    val round: Int,
    val raceName: String,
    val date: String,
    val circuitId: String,
    val status: RaceStatus,
    val season: Int,
)

data class FavoriteDriverStanding(
    val driverId: String,
    val driverName: String,
    val teamName: String?,
    val position: Int,
    val points: Double,
)

data class FavoriteTeamStanding(
    val teamId: String,
    val teamName: String,
    val position: Int,
    val points: Double,
)
