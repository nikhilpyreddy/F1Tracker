package com.nikhil.f1tracker.ui.weekend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.domain.model.CIRCUIT_HISTORY_SEASONS
import com.nikhil.f1tracker.domain.stats.DriverForm
import com.nikhil.f1tracker.domain.stats.TeammateHeadToHead
import com.nikhil.f1tracker.domain.stats.circuitStats
import com.nikhil.f1tracker.domain.stats.driverForms
import com.nikhil.f1tracker.domain.stats.teammateHeadToHeads
import com.nikhil.f1tracker.ui.common.syncCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeekendViewModel @Inject constructor(
    private val f1Repository: F1Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val season: Int = checkNotNull(savedStateHandle["season"])
    private val round: Int = checkNotNull(savedStateHandle["round"])
    private val circuitId: String = checkNotNull(savedStateHandle["circuitId"])
    private val historySeasons = (season - CIRCUIT_HISTORY_SEASONS)..season

    private val isLoading = MutableStateFlow(true)
    private val loadErrorMessage = MutableStateFlow<String?>(null)
    private val selectedTab = MutableStateFlow(WeekendTab.CIRCUIT)

    private val header = combine(
        f1Repository.getRacesForSeason(season).map { races -> races.find { it.round == round } },
        f1Repository.getCircuit(circuitId),
    ) { race, circuit -> Header(race?.raceName.orEmpty(), race?.date.orEmpty(), circuit?.circuitName.orEmpty()) }

    private val names = combine(f1Repository.getAllDrivers(), f1Repository.getAllConstructors()) { drivers, teams ->
        Names(drivers.associateBy { it.driverId }, teams.associateBy { it.constructorId })
    }

    private val history = f1Repository.getResultsAtCircuit(circuitId).map { results ->
        results.filter { it.season in historySeasons }
    }

    private val data = combine(history, f1Repository.getResultsForSeason(season), names) { atCircuit, seasonResults, n ->
        Data(atCircuit, seasonResults, n)
    }

    private val status = combine(isLoading, loadErrorMessage, selectedTab) { loading, error, tab ->
        Status(loading, error, tab)
    }

    val uiState: StateFlow<WeekendUiState> = combine(header, data, status) { header, data, status ->
        WeekendUiState(
            isLoading = status.isLoading,
            loadErrorMessage = status.loadErrorMessage,
            season = season,
            raceName = header.raceName,
            date = header.date,
            circuitId = circuitId,
            circuitName = header.circuitName,
            selectedTab = status.selectedTab,
            circuitStats = data.atCircuit.takeIf { it.isNotEmpty() }?.let(::circuitStats),
            driverForms = driverForms(data.seasonResults, RECENT_RACES).map { it.toRow(data.names) },
            headToHeads = teammateHeadToHeads(data.seasonResults).map { it.toRow(data.names) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WeekendUiState())

    init {
        sync()
    }

    fun selectTab(tab: WeekendTab) {
        selectedTab.value = tab
    }

    fun retry() {
        sync()
    }

    private fun sync() {
        viewModelScope.launch {
            isLoading.value = true
            loadErrorMessage.value = null
            syncCatching {
                f1Repository.syncSeason(season)
                f1Repository.syncCircuitHistory(circuitId, sinceSeason = historySeasons.first)
            }.onFailure {
                loadErrorMessage.value = "Couldn't load weekend data. Check your connection and try again."
            }
            isLoading.value = false
        }
    }

    private data class Header(val raceName: String, val date: String, val circuitName: String)
    private data class Names(val drivers: Map<String, DriverEntity>, val teams: Map<String, ConstructorEntity>) {
        fun driver(id: String) = drivers[id]?.let { "${it.givenName} ${it.familyName}" } ?: id
        fun team(id: String) = teams[id]?.name ?: id
    }
    private data class Data(val atCircuit: List<ResultEntity>, val seasonResults: List<ResultEntity>, val names: Names)
    private data class Status(val isLoading: Boolean, val loadErrorMessage: String?, val selectedTab: WeekendTab)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val RECENT_RACES = 5

        fun DriverForm.toRow(names: Names) = DriverFormRow(
            driverId = driverId,
            driverName = names.driver(driverId),
            teamName = names.team(constructorId),
            seasonPoints = seasonPoints,
            recent = recent,
            retirements = retirements,
        )

        fun TeammateHeadToHead.toRow(names: Names) = HeadToHeadRow(
            teamName = names.team(constructorId),
            firstDriverName = names.driver(firstDriverId),
            secondDriverName = names.driver(secondDriverId),
            gridWins = gridWins,
            finishWins = finishWins,
        )
    }
}
