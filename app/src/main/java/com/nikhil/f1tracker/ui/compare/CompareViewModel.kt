package com.nikhil.f1tracker.ui.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.domain.model.lastFourSeasons
import com.nikhil.f1tracker.ui.common.ChartPoint
import com.nikhil.f1tracker.ui.common.syncSeasonsCurrentFirst
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
class CompareViewModel @Inject constructor(
    private val f1Repository: F1Repository,
) : ViewModel() {

    private val currentSeason = Year.now().value
    private val seasons = lastFourSeasons(currentSeason)

    private val isLoading = MutableStateFlow(true)
    private val loadErrorMessage = MutableStateFlow<String?>(null)
    private val mode = MutableStateFlow(CompareMode.DRIVERS)
    private val selectedFirstId = MutableStateFlow<String?>(null)
    private val selectedSecondId = MutableStateFlow<String?>(null)

    private val roster = combine(
        f1Repository.getAllDrivers(),
        f1Repository.getAllConstructors(),
    ) { drivers, teams -> Roster(drivers, teams) }

    private val firstTrend = trendFor(selectedFirstId)
    private val secondTrend = trendFor(selectedSecondId)

    private val selection = combine(mode, selectedFirstId, selectedSecondId) { m, first, second ->
        Selection(m, first, second)
    }

    private val syncStatus = combine(isLoading, loadErrorMessage) { l, e -> l to e }

    val uiState: StateFlow<CompareUiState> = combine(
        roster,
        selection,
        firstTrend,
        secondTrend,
        syncStatus,
    ) { roster, selection, first, second, status ->
        val (loading, error) = status
        CompareUiState(
            isLoading = loading,
            loadErrorMessage = error,
            mode = selection.mode,
            availableDrivers = roster.drivers.sortedBy { it.familyName },
            availableTeams = roster.teams.sortedBy { it.name },
            selectedFirstId = selection.first,
            selectedSecondId = selection.second,
            firstName = selection.first?.let { nameFor(it, selection.mode, roster) },
            secondName = selection.second?.let { nameFor(it, selection.mode, roster) },
            firstTrend = first,
            secondTrend = second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CompareUiState())

    init {
        viewModelScope.launch {
            runSync {
                f1Repository.syncDriverRoster(currentSeason)
                f1Repository.syncConstructorRoster(currentSeason)
                syncSeasonsCurrentFirst(seasons, currentSeason) { year ->
                    f1Repository.syncDriverStandings(year)
                    f1Repository.syncConstructorStandings(year)
                }
            }
        }
    }

    fun setMode(newMode: CompareMode) {
        mode.value = newMode
        selectedFirstId.value = null
        selectedSecondId.value = null
    }

    fun select(id: String) {
        when {
            id == selectedFirstId.value -> selectedFirstId.value = null
            id == selectedSecondId.value -> selectedSecondId.value = null
            selectedFirstId.value == null -> selectedFirstId.value = id
            selectedSecondId.value == null -> selectedSecondId.value = id
            else -> selectedSecondId.value = id
        }
    }

    fun retry() {
        viewModelScope.launch {
            runSync {
                syncSeasonsCurrentFirst(seasons, currentSeason) { year ->
                    f1Repository.syncDriverStandings(year)
                    f1Repository.syncConstructorStandings(year)
                }
            }
        }
    }

    private fun trendFor(idFlow: MutableStateFlow<String?>): Flow<List<ChartPoint>> =
        combine(mode, idFlow) { m, id -> m to id }.flatMapLatest { (m, id) ->
            if (id == null) {
                flowOf(emptyList())
            } else {
                when (m) {
                    CompareMode.DRIVERS -> f1Repository.getDriverStandingsAcrossSeasons(id, seasons)
                        .map { standings -> toChartPoints(standings.associate { it.season to it.points }) }
                    CompareMode.TEAMS -> f1Repository.getConstructorStandingsAcrossSeasons(id, seasons)
                        .map { standings -> toChartPoints(standings.associate { it.season to it.points }) }
                }
            }
        }

    private fun toChartPoints(pointsBySeason: Map<Int, Double>): List<ChartPoint> =
        seasons.map { year -> ChartPoint(year.toString(), pointsBySeason[year]?.toFloat() ?: 0f) }

    private fun nameFor(id: String, mode: CompareMode, roster: Roster): String = when (mode) {
        CompareMode.DRIVERS -> roster.drivers.find { it.driverId == id }
            ?.let { "${it.givenName} ${it.familyName}" }.orEmpty()
        CompareMode.TEAMS -> roster.teams.find { it.constructorId == id }?.name.orEmpty()
    }

    private suspend fun runSync(block: suspend () -> Unit) {
        isLoading.value = true
        loadErrorMessage.value = null
        try {
            block()
        } catch (e: IOException) {
            loadErrorMessage.value = "Couldn't load comparison data. Check your connection and try again."
        } catch (e: HttpException) {
            loadErrorMessage.value = "Couldn't load comparison data. Check your connection and try again."
        } finally {
            isLoading.value = false
        }
    }

    private data class Roster(val drivers: List<DriverEntity>, val teams: List<ConstructorEntity>)
    private data class Selection(val mode: CompareMode, val first: String?, val second: String?)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
