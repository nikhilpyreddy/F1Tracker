package com.nikhil.f1tracker.ui.driver

import com.nikhil.f1tracker.ui.common.ChartPoint

data class DriverDetailUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val driverName: String = "",
    val driverCode: String? = null,
    val nationality: String? = null,
    val pointsTrend: List<ChartPoint> = emptyList(),
    val availableSeasons: List<Int> = emptyList(),
    val selectedSeason: Int? = null,
    val seasonResults: List<DriverSeasonResultRow> = emptyList(),
)

data class DriverSeasonResultRow(
    val round: Int,
    val raceName: String,
    val circuitId: String,
    val positionText: String,
    val points: Double,
    val status: String,
)
