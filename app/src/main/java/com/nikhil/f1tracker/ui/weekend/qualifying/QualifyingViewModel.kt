package com.nikhil.f1tracker.ui.weekend.qualifying

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.data.repository.QualifyingRepository
import com.nikhil.f1tracker.domain.model.CIRCUIT_HISTORY_SEASONS
import com.nikhil.f1tracker.domain.stats.PoleRecord
import com.nikhil.f1tracker.domain.stats.QualifyingForm
import com.nikhil.f1tracker.domain.stats.QualifyingLine
import com.nikhil.f1tracker.domain.stats.Rate
import com.nikhil.f1tracker.domain.stats.TeammateHeadToHead
import com.nikhil.f1tracker.domain.stats.poleHistory
import com.nikhil.f1tracker.domain.stats.qualifyingClassification
import com.nikhil.f1tracker.domain.stats.qualifyingForms
import com.nikhil.f1tracker.domain.stats.qualifyingHeadToHeads
import com.nikhil.f1tracker.ui.common.syncCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QualifyingUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val season: Int = 0,
    /** This weekend's session; empty until it has run. */
    val weekend: List<QualifyingLine> = emptyList(),
    val poles: List<PoleRecord> = emptyList(),
    /** Of poles where the race result is known, how many converted to a win. */
    val poleToWin: Rate = Rate(0, 0),
    val forms: List<QualifyingForm> = emptyList(),
    val headToHeads: List<TeammateHeadToHead> = emptyList(),
    val driverNames: Map<String, String> = emptyMap(),
    val teamNames: Map<String, String> = emptyMap(),
)

@HiltViewModel
class QualifyingViewModel @Inject constructor(
    f1Repository: F1Repository,
    private val qualifyingRepository: QualifyingRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val season: Int = checkNotNull(savedStateHandle["season"])
    private val round: Int = checkNotNull(savedStateHandle["round"])
    private val circuitId: String = checkNotNull(savedStateHandle["circuitId"])
    private val sinceSeason = season - CIRCUIT_HISTORY_SEASONS

    private val isLoading = MutableStateFlow(true)
    private val loadErrorMessage = MutableStateFlow<String?>(null)

    private val names = combine(f1Repository.getAllDrivers(), f1Repository.getAllConstructors()) { drivers, teams ->
        drivers.associate { it.driverId to "${it.givenName} ${it.familyName}" } to teams.associate { it.constructorId to it.name }
    }

    private val status = combine(isLoading, loadErrorMessage) { loading, error -> loading to error }

    val uiState: StateFlow<QualifyingUiState> = combine(
        qualifyingRepository.getSeason(season),
        qualifyingRepository.getAtCircuit(circuitId),
        f1Repository.getResultsAtCircuit(circuitId),
        names,
        status,
    ) { seasonRows, circuitRows, circuitResults, (driverNames, teamNames), (loading, error) ->
        val poles = poleHistory(circuitRows.filter { it.season >= sinceSeason }, circuitResults)
        val decided = poles.filter { it.racePositionText != null }
        QualifyingUiState(
            isLoading = loading,
            loadErrorMessage = error,
            season = season,
            weekend = qualifyingClassification(seasonRows.filter { it.round == round }),
            poles = poles,
            poleToWin = decided.let { list -> Rate(list.count { it.racePositionText == "1" }, list.size) },
            forms = qualifyingForms(seasonRows, RECENT_SESSIONS),
            headToHeads = qualifyingHeadToHeads(seasonRows),
            driverNames = driverNames,
            teamNames = teamNames,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), QualifyingUiState())

    init {
        load(forceRefresh = false)
    }

    /** Force-refresh so a session that just finished shows up. */
    fun refresh() = load(forceRefresh = true)

    private fun load(forceRefresh: Boolean) {
        viewModelScope.launch {
            isLoading.value = true
            loadErrorMessage.value = null
            syncCatching {
                qualifyingRepository.syncSeason(season, forceRefresh)
                qualifyingRepository.syncCircuit(circuitId, sinceSeason)
            }.onFailure {
                loadErrorMessage.value = "Couldn't load qualifying data. Check your connection and try again."
            }
            isLoading.value = false
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val RECENT_SESSIONS = 5
    }
}
