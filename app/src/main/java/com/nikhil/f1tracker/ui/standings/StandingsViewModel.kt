package com.nikhil.f1tracker.ui.standings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.repository.F1Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.Year
import javax.inject.Inject

@HiltViewModel
class StandingsViewModel @Inject constructor(
    private val f1Repository: F1Repository,
) : ViewModel() {

    private val currentSeason = Year.now().value

    private val isLoading = MutableStateFlow(true)
    private val isRefreshing = MutableStateFlow(false)
    private val loadErrorMessage = MutableStateFlow<String?>(null)
    private val mode = MutableStateFlow(StandingsMode.DRIVERS)

    private val driverRows = combine(
        f1Repository.getDriverStandingsForSeason(currentSeason),
        f1Repository.getAllDrivers(),
        f1Repository.getAllConstructors(),
    ) { standings, drivers, constructors ->
        val driversById = drivers.associateBy { it.driverId }
        val teamsById = constructors.associateBy { it.constructorId }
        standings.sortedBy { it.position }.map { standing ->
            DriverStandingRow(
                driverId = standing.driverId,
                position = standing.position,
                driverName = driversById[standing.driverId]
                    ?.let { "${it.givenName} ${it.familyName}" }
                    ?: standing.driverId,
                teamName = standing.constructorId?.let { teamsById[it]?.name },
                points = standing.points,
                wins = standing.wins,
            )
        }
    }

    private val constructorRows = combine(
        f1Repository.getConstructorStandingsForSeason(currentSeason),
        f1Repository.getAllConstructors(),
    ) { standings, constructors ->
        val teamsById = constructors.associateBy { it.constructorId }
        standings.sortedBy { it.position }.map { standing ->
            ConstructorStandingRow(
                constructorId = standing.constructorId,
                position = standing.position,
                teamName = teamsById[standing.constructorId]?.name ?: standing.constructorId,
                points = standing.points,
                wins = standing.wins,
            )
        }
    }

    private val syncStatus = combine(isLoading, isRefreshing, loadErrorMessage, mode) { loading, refreshing, error, m ->
        SyncStatus(loading, refreshing, error, m)
    }

    val uiState: StateFlow<StandingsUiState> = combine(
        driverRows,
        constructorRows,
        syncStatus,
    ) { drivers, constructors, status ->
        StandingsUiState(
            isLoading = status.isLoading,
            isRefreshing = status.isRefreshing,
            loadErrorMessage = status.loadErrorMessage,
            mode = status.mode,
            driverStandings = drivers,
            constructorStandings = constructors,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StandingsUiState())

    init {
        sync(forceRefresh = false)
    }

    fun setMode(newMode: StandingsMode) {
        mode.value = newMode
    }

    fun refresh() {
        sync(forceRefresh = true)
    }

    fun retry() {
        sync(forceRefresh = false)
    }

    private fun sync(forceRefresh: Boolean) {
        viewModelScope.launch {
            if (forceRefresh) isRefreshing.value = true else isLoading.value = true
            loadErrorMessage.value = null
            try {
                f1Repository.syncDriverStandings(currentSeason, forceRefresh)
                f1Repository.syncConstructorStandings(currentSeason, forceRefresh)
            } catch (e: IOException) {
                loadErrorMessage.value = "Couldn't load standings. Check your connection and try again."
            } catch (e: HttpException) {
                loadErrorMessage.value = "Couldn't load standings. Check your connection and try again."
            } finally {
                isLoading.value = false
                isRefreshing.value = false
            }
        }
    }

    private data class SyncStatus(
        val isLoading: Boolean,
        val isRefreshing: Boolean,
        val loadErrorMessage: String?,
        val mode: StandingsMode,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
