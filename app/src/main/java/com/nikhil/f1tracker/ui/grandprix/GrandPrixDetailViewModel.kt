package com.nikhil.f1tracker.ui.grandprix

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.domain.model.GRAND_PRIX_HISTORY_YEARS
import com.nikhil.f1tracker.domain.model.lastNSeasons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.Year
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GrandPrixDetailViewModel @Inject constructor(
    private val f1Repository: F1Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val currentSeason = Year.now().value
    private val historySeasons = lastNSeasons(currentSeason, GRAND_PRIX_HISTORY_YEARS)

    private val isLoading = MutableStateFlow(true)
    private val loadErrorMessage = MutableStateFlow<String?>(null)
    private val selectedCircuitId = MutableStateFlow(checkNotNull(savedStateHandle["circuitId"] as String?))
    private val selectedDriverId = MutableStateFlow(savedStateHandle["driverId"] as String?)

    private val circuitName = selectedCircuitId.flatMapLatest { circuitId ->
        f1Repository.getCircuit(circuitId).map { it?.circuitName.orEmpty() }
    }

    private val otherGrandPrix = f1Repository.getRacesForSeason(currentSeason).map { races ->
        races.sortedBy { it.round }.map { GrandPrixOption(it.circuitId, it.raceName) }.distinctBy { it.circuitId }
    }

    private val drivers = f1Repository.getAllDrivers().map { list ->
        list.sortedBy { it.familyName }.map { DriverOption(it.driverId, "${it.givenName} ${it.familyName}") }
    }

    private val history = combine(selectedDriverId, selectedCircuitId) { driverId, circuitId -> driverId to circuitId }
        .flatMapLatest { (driverId, circuitId) ->
            if (driverId == null) {
                flowOf(emptyList())
            } else {
                f1Repository.getResultsForDriverAtCircuit(driverId, circuitId).map { results ->
                    results.filter { it.season in historySeasons }
                        .sortedByDescending { it.season }
                        .map { GrandPrixResultRow(it.season, it.positionText, it.points, it.status) }
                }
            }
        }

    private val grandPrixData = combine(selectedCircuitId, circuitName, otherGrandPrix) { id, name, options ->
        GrandPrixData(id, name, options)
    }

    private val driverData = combine(drivers, selectedDriverId, history) { driverOptions, driverId, results ->
        DriverData(driverOptions, driverId, results)
    }

    private val syncStatus = combine(isLoading, loadErrorMessage) { l, e -> l to e }

    val uiState: StateFlow<GrandPrixDetailUiState> = combine(
        grandPrixData,
        driverData,
        syncStatus,
    ) { grandPrix, driver, status ->
        val (loading, error) = status
        GrandPrixDetailUiState(
            isLoading = loading,
            loadErrorMessage = error,
            circuitId = grandPrix.circuitId,
            circuitName = grandPrix.circuitName,
            otherGrandPrix = grandPrix.otherGrandPrix,
            drivers = driver.drivers,
            selectedDriverId = driver.selectedDriverId,
            selectedDriverName = driver.selectedDriverId?.let { id ->
                driver.drivers.find { it.driverId == id }?.driverName
            },
            history = driver.history,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), GrandPrixDetailUiState())

    init {
        sync()
    }

    fun selectGrandPrix(circuitId: String) {
        selectedCircuitId.value = circuitId
    }

    fun selectDriver(driverId: String) {
        selectedDriverId.value = driverId
    }

    fun retry() {
        sync()
    }

    private fun sync() {
        viewModelScope.launch {
            isLoading.value = true
            loadErrorMessage.value = null
            try {
                f1Repository.syncDriverRoster(currentSeason)
                historySeasons.forEach { f1Repository.syncSeason(it) }
            } catch (e: IOException) {
                loadErrorMessage.value = "Couldn't load race history. Check your connection and try again."
            } catch (e: HttpException) {
                loadErrorMessage.value = "Couldn't load race history. Check your connection and try again."
            } finally {
                isLoading.value = false
            }
        }
    }

    private data class GrandPrixData(
        val circuitId: String,
        val circuitName: String,
        val otherGrandPrix: List<GrandPrixOption>,
    )

    private data class DriverData(
        val drivers: List<DriverOption>,
        val selectedDriverId: String?,
        val history: List<GrandPrixResultRow>,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
