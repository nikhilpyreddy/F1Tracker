package com.nikhil.f1tracker.ui.weekend.strategy

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.data.repository.OpenF1Repository
import com.nikhil.f1tracker.data.repository.RaceSessionData
import com.nikhil.f1tracker.domain.stats.StrategySummary
import com.nikhil.f1tracker.domain.stats.strategySummary
import com.nikhil.f1tracker.ui.common.syncCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StrategyUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val summary: StrategySummary? = null,
)

@HiltViewModel
class StrategyViewModel @Inject constructor(
    f1Repository: F1Repository,
    private val openF1Repository: OpenF1Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val circuitId: String = checkNotNull(savedStateHandle["circuitId"])
    private val sessions = MutableStateFlow<List<RaceSessionData>>(emptyList())
    private val isLoading = MutableStateFlow(true)
    private val loadErrorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<StrategyUiState> = combine(
        sessions,
        f1Repository.getResultsAtCircuit(circuitId),
        f1Repository.getAllDrivers(),
        isLoading,
        loadErrorMessage,
    ) { sessions, results, drivers, loading, error ->
        StrategyUiState(
            isLoading = loading,
            loadErrorMessage = error,
            summary = sessions.takeIf { it.isNotEmpty() }?.let { summarize(it, results, drivers) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StrategyUiState())

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            isLoading.value = true
            loadErrorMessage.value = null
            syncCatching {
                openF1Repository.raceSessions(circuitId).collect { sessions.value = it }
            }.onFailure {
                loadErrorMessage.value = "Couldn't load tyre strategy data. Check your connection and try again."
            }
            isLoading.value = false
        }
    }

    /** OpenF1 knows drivers by number/code; Jolpica has finishing order, so join on the 3-letter code. */
    private fun summarize(
        sessions: List<RaceSessionData>,
        results: List<ResultEntity>,
        drivers: List<DriverEntity>,
    ): StrategySummary {
        val codes = drivers.associate { it.driverId to it.code }
        val resultsByYear = results.groupBy { it.season }
        val positions = resultsByYear.mapValues { (_, rows) ->
            rows.mapNotNull { row -> codes[row.driverId]?.let { it to row.positionText } }.toMap()
        }
        val driverIds = resultsByYear.mapValues { (_, rows) ->
            rows.mapNotNull { row -> codes[row.driverId]?.let { it to row.driverId } }.toMap()
        }
        return strategySummary(sessions, positions, driverIds)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
