package com.nikhil.f1tracker.ui.home

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val loadErrorMessage: String? = null,
    val nextRace: UpcomingRace? = null,
    val favoriteDrivers: List<FavoriteDriverStanding> = emptyList(),
    val favoriteTeams: List<FavoriteTeamStanding> = emptyList(),
)

data class UpcomingRace(
    val raceName: String,
    val date: String,
    val round: Int,
    val circuitId: String,
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
