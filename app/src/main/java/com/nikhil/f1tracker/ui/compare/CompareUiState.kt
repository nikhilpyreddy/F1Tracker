package com.nikhil.f1tracker.ui.compare

import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.ui.common.ChartPoint

enum class CompareMode { DRIVERS, TEAMS }

data class CompareUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val mode: CompareMode = CompareMode.DRIVERS,
    val availableDrivers: List<DriverEntity> = emptyList(),
    val availableTeams: List<ConstructorEntity> = emptyList(),
    val selectedFirstId: String? = null,
    val selectedSecondId: String? = null,
    val firstName: String? = null,
    val secondName: String? = null,
    val firstTrend: List<ChartPoint> = emptyList(),
    val secondTrend: List<ChartPoint> = emptyList(),
)
