package com.nikhil.f1tracker.ui.driver

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.domain.model.lastFourSeasons
import com.nikhil.f1tracker.ui.common.ChartPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.Year
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DriverDetailViewModel @Inject constructor(
    private val f1Repository: F1Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val driverId: String = checkNotNull(savedStateHandle["driverId"])
    private val currentSeason = Year.now().value
    private val seasons = lastFourSeasons(currentSeason)

    private val isLoading = MutableStateFlow(true)
    private val loadErrorMessage = MutableStateFlow<String?>(null)
    private val selectedSeason = MutableStateFlow(currentSeason)

    private val driverInfo = f1Repository.getAllDrivers()
        .map { drivers -> drivers.find { it.driverId == driverId } }

    private val standingsAcrossSeasons = f1Repository.getDriverStandingsAcrossSeasons(driverId, seasons)

    private val seasonRaceData = selectedSeason.flatMapLatest { season ->
        combine(
            f1Repository.getResultsForDriverAndSeason(driverId, season),
            f1Repository.getRacesForSeason(season),
        ) { results, races -> results to races }
    }

    private val syncStatus = combine(isLoading, loadErrorMessage, selectedSeason) { l, e, s -> SyncStatus(l, e, s) }

    val uiState: StateFlow<DriverDetailUiState> = combine(
        driverInfo,
        standingsAcrossSeasons,
        seasonRaceData,
        syncStatus,
    ) { driver, standings, (results, races), status ->
        val pointsBySeason = standings.associateBy { it.season }
        val racesByRound = races.associateBy { it.round }
        DriverDetailUiState(
            isLoading = status.isLoading,
            loadErrorMessage = status.loadErrorMessage,
            driverName = driver?.let { "${it.givenName} ${it.familyName}" }.orEmpty(),
            driverCode = driver?.code,
            nationality = driver?.nationality,
            pointsTrend = seasons.map { year ->
                ChartPoint(year.toString(), pointsBySeason[year]?.points?.toFloat() ?: 0f)
            },
            availableSeasons = seasons,
            selectedSeason = status.selectedSeason,
            seasonResults = results.sortedBy { it.round }.map { it.toRow(racesByRound) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DriverDetailUiState())

    init {
        viewModelScope.launch {
            runSync {
                seasons.forEach { f1Repository.syncDriverStandings(it) }
                f1Repository.syncSeason(currentSeason)
            }
        }
    }

    fun selectSeason(season: Int) {
        selectedSeason.value = season
        viewModelScope.launch { runSync { f1Repository.syncSeason(season) } }
    }

    fun retry() {
        viewModelScope.launch { runSync { f1Repository.syncSeason(selectedSeason.value) } }
    }

    private suspend fun runSync(block: suspend () -> Unit) {
        isLoading.value = true
        loadErrorMessage.value = null
        try {
            block()
        } catch (e: IOException) {
            loadErrorMessage.value = "Couldn't load driver data. Check your connection and try again."
        } catch (e: HttpException) {
            loadErrorMessage.value = "Couldn't load driver data. Check your connection and try again."
        } finally {
            isLoading.value = false
        }
    }

    private fun ResultEntity.toRow(racesByRound: Map<Int, RaceEntity>): DriverSeasonResultRow {
        val race = racesByRound[round]
        return DriverSeasonResultRow(
            round = round,
            raceName = race?.raceName ?: "Round $round",
            circuitId = race?.circuitId.orEmpty(),
            positionText = positionText,
            points = points,
            status = status,
        )
    }

    private data class SyncStatus(val isLoading: Boolean, val loadErrorMessage: String?, val selectedSeason: Int)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
