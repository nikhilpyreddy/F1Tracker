package com.nikhil.f1tracker.ui.weekend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.domain.model.CIRCUIT_HISTORY_SEASONS
import com.nikhil.f1tracker.domain.model.formatRaceWhen
import com.nikhil.f1tracker.domain.model.isOver
import com.nikhil.f1tracker.domain.model.sessionStart
import java.time.Clock
import java.time.Duration
import java.time.Instant
import com.nikhil.f1tracker.domain.stats.CircuitStat
import com.nikhil.f1tracker.domain.stats.DriverForm
import com.nikhil.f1tracker.domain.stats.TeammateHeadToHead
import com.nikhil.f1tracker.domain.stats.circuitStatDetails
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
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {

    private val season: Int = checkNotNull(savedStateHandle["season"])
    private val round: Int = checkNotNull(savedStateHandle["round"])
    private val circuitId: String = checkNotNull(savedStateHandle["circuitId"])
    private val historySeasons = (season - CIRCUIT_HISTORY_SEASONS)..season

    private val isLoading = MutableStateFlow(true)
    private val loadErrorMessage = MutableStateFlow<String?>(null)
    /** Null until the user picks a tab; until then the tab follows the weekend (see [defaultTab]). */
    private val selectedTab = MutableStateFlow<WeekendTab?>(null)
    private val openStat = MutableStateFlow<CircuitStat?>(null)
    private val resultsSeason = MutableStateFlow<Int?>(null)
    private val historyDriverId = MutableStateFlow<String?>(null)

    private val historyPicker = combine(f1Repository.getSeasonDrivers(season), historyDriverId) { drivers, selected ->
        drivers.sortedBy { it.familyName }.map { HistoryDriver(it.driverId, "${it.givenName} ${it.familyName}") } to selected
    }

    private val header = combine(
        f1Repository.getRacesForSeason(season).map { races -> races.find { it.round == round } },
        f1Repository.getCircuit(circuitId),
    ) { race, circuit ->
        Header(
            race?.raceName.orEmpty(),
            race?.let { formatRaceWhen(it.date, it.time) }.orEmpty(),
            circuit?.circuitName.orEmpty(),
            race?.let(::defaultTab) ?: WeekendTab.CIRCUIT,
        )
    }

    private val names = combine(f1Repository.getAllDrivers(), f1Repository.getAllConstructors()) { drivers, teams ->
        Names(drivers.associateBy { it.driverId }, teams.associateBy { it.constructorId })
    }

    private val history = f1Repository.getResultsAtCircuit(circuitId).map { results ->
        results.filter { it.season in historySeasons }
    }

    private val data = combine(history, f1Repository.getResultsForSeason(season), names) { atCircuit, seasonResults, n ->
        Data(atCircuit, seasonResults, n)
    }

    private val status = combine(isLoading, loadErrorMessage, selectedTab, openStat, resultsSeason) { loading, error, tab, stat, season ->
        Status(loading, error, tab, stat, season)
    }

    val uiState: StateFlow<WeekendUiState> = combine(header, data, status, historyPicker) { header, data, status, (pickerDrivers, selectedDriver) ->
        WeekendUiState(
            isLoading = status.isLoading,
            loadErrorMessage = status.loadErrorMessage,
            season = season,
            raceName = header.raceName,
            date = header.date,
            circuitId = circuitId,
            circuitName = header.circuitName,
            selectedTab = status.selectedTab ?: header.defaultTab,
            circuitStats = data.atCircuit.takeIf { it.isNotEmpty() }?.let(::circuitStats),
            driverForms = driverForms(data.seasonResults, RECENT_RACES).map { it.toRow(data.names) },
            headToHeads = teammateHeadToHeads(data.seasonResults).map { it.toRow(data.names) },
            statSheet = status.openStat?.let { stat ->
                val races = circuitStatDetails(stat, data.atCircuit)
                val driverIds = races.flatMap { race -> race.lines.map { it.driverId } }.toSet()
                StatSheet(stat, races, driverIds.associateWith { data.names.driver(it) })
            },
            lastPodium = classification(data, latestSeason(data))?.take(PODIUM_PLACES).orEmpty(),
            lastRaceSeason = latestSeason(data),
            historyDrivers = pickerDrivers,
            selectedHistoryDriverId = selectedDriver,
            driverHistory = selectedDriver?.let { driverId ->
                data.atCircuit.filter { it.driverId == driverId }
                    .sortedWith(compareByDescending<ResultEntity> { it.season }.thenByDescending { it.round })
                    .map { DriverCircuitResult(it.season, it.constructorId, it.positionText, it.grid, it.points, it.status) }
            }.orEmpty(),
            resultsSheet = status.resultsSeason?.let { season ->
                ResultsSheet(season, data.atCircuit.map { it.season }.distinct().sortedDescending(), classification(data, season).orEmpty())
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WeekendUiState())

    init {
        sync()
    }

    fun selectTab(tab: WeekendTab) {
        selectedTab.value = tab
    }

    fun openStat(stat: CircuitStat) {
        openStat.value = stat
    }

    fun closeStat() {
        openStat.value = null
    }

    /** Opens the full classification; defaults to the most recent race here. */
    fun openResults(season: Int? = null) {
        resultsSeason.value = season ?: uiState.value.lastRaceSeason
    }

    fun selectHistoryDriver(driverId: String) {
        historyDriverId.value = driverId.takeIf { it != historyDriverId.value }
    }

    fun closeResults() {
        resultsSeason.value = null
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

    private data class Header(val raceName: String, val date: String, val circuitName: String, val defaultTab: WeekendTab)

    /** The tab most useful right now: Strategy once the race is done, Qualifying once it has run, else Circuit. */
    private fun defaultTab(race: RaceEntity): WeekendTab {
        val now = Instant.now(clock)
        val qualifyingDone = sessionStart(race.qualifyingDate, race.qualifyingTime)?.plus(QUALIFYING_DURATION)?.let { it < now } ?: false
        return when {
            race.isOver(now) -> WeekendTab.STRATEGY
            qualifyingDone -> WeekendTab.QUALIFYING
            else -> WeekendTab.CIRCUIT
        }
    }
    private data class Names(val drivers: Map<String, DriverEntity>, val teams: Map<String, ConstructorEntity>) {
        fun driver(id: String) = drivers[id]?.let { "${it.givenName} ${it.familyName}" } ?: id
        fun team(id: String) = teams[id]?.name ?: id
    }
    private data class Data(val atCircuit: List<ResultEntity>, val seasonResults: List<ResultEntity>, val names: Names)
    private data class Status(
        val isLoading: Boolean,
        val loadErrorMessage: String?,
        val selectedTab: WeekendTab?,
        val openStat: CircuitStat?,
        val resultsSeason: Int?,
    )

    private fun latestSeason(data: Data): Int? = data.atCircuit.maxOfOrNull { it.season }

    /** Official order for [season]'s race here (latest round if a season had two). */
    private fun classification(data: Data, season: Int?): List<ClassificationRow>? {
        val race = data.atCircuit.filter { it.season == season }
        val round = race.maxOfOrNull { it.round } ?: return null
        return race.filter { it.round == round }
            .sortedBy { it.position ?: Int.MAX_VALUE }
            .map {
                ClassificationRow(
                    it.driverId, it.constructorId, data.names.driver(it.driverId), data.names.team(it.constructorId),
                    it.positionText, it.grid, it.points, it.status,
                )
            }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val RECENT_RACES = 5
        val QUALIFYING_DURATION: Duration = Duration.ofMinutes(90)
        const val PODIUM_PLACES = 3

        fun DriverForm.toRow(names: Names) = DriverFormRow(
            driverId = driverId,
            driverName = names.driver(driverId),
            constructorId = constructorId,
            teamName = names.team(constructorId),
            seasonPoints = seasonPoints,
            recent = recent,
            retirements = retirements,
        )

        fun TeammateHeadToHead.toRow(names: Names) = HeadToHeadRow(
            constructorId = constructorId,
            teamName = names.team(constructorId),
            firstDriverId = firstDriverId,
            firstDriverName = names.driver(firstDriverId),
            secondDriverId = secondDriverId,
            secondDriverName = names.driver(secondDriverId),
            gridWins = gridWins,
            finishWins = finishWins,
        )
    }
}
