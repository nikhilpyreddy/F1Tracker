package com.nikhil.f1tracker.ui.team

import com.nikhil.f1tracker.ui.common.ChartPoint

data class TeamDetailUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val teamName: String = "",
    val nationality: String? = null,
    val pointsTrend: List<ChartPoint> = emptyList(),
    val seasonStandings: List<TeamSeasonStanding> = emptyList(),
)

data class TeamSeasonStanding(
    val season: Int,
    val position: Int,
    val points: Double,
    val wins: Int,
)
