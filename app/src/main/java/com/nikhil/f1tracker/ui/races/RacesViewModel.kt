package com.nikhil.f1tracker.ui.races

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.domain.model.APP_ZONE
import com.nikhil.f1tracker.domain.model.RaceStatus
import com.nikhil.f1tracker.domain.model.raceStatuses
import com.nikhil.f1tracker.ui.common.syncCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.Year
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A podium finisher, enough to draw a team-coloured code badge. */
data class PodiumEntry(val driverId: String, val constructorId: String)

data class RaceRow(
    val season: Int,
    val round: Int,
    val raceName: String,
    val circuitId: String,
    val date: String,
    val time: String?,
    val status: RaceStatus,
    val podium: List<PodiumEntry>,
)

data class RacesUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val season: Int = 0,
    val seasons: List<Int> = emptyList(),
    val races: List<RaceRow> = emptyList(),
)

private const val SEASONS_OFFERED = 10
private const val PODIUM_PLACES = 3

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RacesViewModel @Inject constructor(
    private val f1Repository: F1Repository,
    private val clock: Clock,
) : ViewModel() {

    private val currentSeason = Year.now(clock.withZone(APP_ZONE)).value
    private val seasons = (currentSeason downTo currentSeason - SEASONS_OFFERED + 1).toList()
    private val selectedSeason = MutableStateFlow(currentSeason)
    private val isLoading = MutableStateFlow(true)
    private val loadErrorMessage = MutableStateFlow<String?>(null)

    private val rows = selectedSeason.flatMapLatest { season ->
        combine(f1Repository.getRacesForSeason(season), f1Repository.getResultsForSeason(season)) { races, results ->
            val podiums = results.filter { it.positionText.toIntOrNull() in 1..PODIUM_PLACES }
                .groupBy { it.round }
                .mapValues { (_, rows) -> rows.sortedBy { it.position }.map { PodiumEntry(it.driverId, it.constructorId) } }
            raceStatuses(races, Instant.now(clock)).map { (race, status) ->
                RaceRow(
                    race.season, race.round, race.raceName, race.circuitId, race.date, race.time, status,
                    podiums[race.round].orEmpty(),
                )
            }
        }
    }

    val uiState: StateFlow<RacesUiState> = combine(rows, selectedSeason, isLoading, loadErrorMessage) { rows, season, loading, error ->
        RacesUiState(loading, error, season, seasons, rows)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RacesUiState(season = currentSeason, seasons = seasons))

    init {
        sync(currentSeason)
    }

    fun selectSeason(season: Int) {
        selectedSeason.value = season
        sync(season)
    }

    fun retry() = sync(selectedSeason.value)

    private fun sync(season: Int) {
        viewModelScope.launch {
            isLoading.value = true
            loadErrorMessage.value = null
            syncCatching { f1Repository.syncSeason(season) }
                .onFailure { loadErrorMessage.value = "Couldn't load the $season calendar. Check your connection and try again." }
            isLoading.value = false
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
