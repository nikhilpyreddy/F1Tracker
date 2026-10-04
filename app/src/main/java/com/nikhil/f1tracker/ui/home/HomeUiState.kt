package com.nikhil.f1tracker.ui.home

import java.time.Instant

data class HomeUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val loadErrorMessage: String? = null,
    val nextRace: UpcomingRace? = null,
    val calendar: List<CalendarRace> = emptyList(),
    val favoriteDrivers: List<FavoriteDriverStanding> = emptyList(),
    val favoriteTeams: List<FavoriteTeamStanding> = emptyList(),
    val driverLeaderPoints: Double = 0.0,
    val teamLeaderPoints: Double = 0.0,
)

data class UpcomingRace(
    val raceName: String,
    val date: String,
    val round: Int,
    val circuitId: String,
    val season: Int,
    /** Session starts (UTC instants; the UI shows them in US Central). Null if not published. */
    val raceStart: Instant? = null,
    val qualifyingStart: Instant? = null,
    val sprintStart: Instant? = null,
)

enum class RaceStatus { COMPLETED, NEXT, UPCOMING }

data class CalendarRace(
    val round: Int,
    val raceName: String,
    val date: String,
    val circuitId: String,
    val status: RaceStatus,
    val season: Int,
    val time: String? = null,
)

data class FavoriteDriverStanding(
    val driverId: String,
    val driverName: String,
    val teamName: String?,
    val constructorId: String?,
    val position: Int,
    val points: Double,
)

data class FavoriteTeamStanding(
    val teamId: String,
    val teamName: String,
    val position: Int,
    val points: Double,
)
