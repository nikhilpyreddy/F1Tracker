package com.nikhil.f1tracker.ui.grandprix

data class GrandPrixDetailUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val circuitId: String = "",
    val circuitName: String = "",
    val otherGrandPrix: List<GrandPrixOption> = emptyList(),
    val drivers: List<DriverOption> = emptyList(),
    val selectedDriverId: String? = null,
    val selectedDriverName: String? = null,
    val history: List<GrandPrixResultRow> = emptyList(),
)

data class GrandPrixOption(
    val circuitId: String,
    val raceName: String,
)

data class DriverOption(
    val driverId: String,
    val driverName: String,
)

data class GrandPrixResultRow(
    val season: Int,
    val positionText: String,
    val points: Double,
    val status: String,
)
