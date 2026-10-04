package com.nikhil.f1tracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorStandingEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.DriverStandingEntity
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.data.repository.FavoritesRepository
import com.nikhil.f1tracker.domain.model.APP_ZONE
import com.nikhil.f1tracker.domain.model.FavoriteSelection
import com.nikhil.f1tracker.domain.model.RACE_DURATION
import com.nikhil.f1tracker.domain.model.sessionStart
import com.nikhil.f1tracker.ui.common.syncCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val f1Repository: F1Repository,
    favoritesRepository: FavoritesRepository,
    private val clock: Clock,
) : ViewModel() {

    private val currentSeason = Year.now(clock).value
    private val isLoading = MutableStateFlow(true)
    private val isRefreshing = MutableStateFlow(false)
    private val loadErrorMessage = MutableStateFlow<String?>(null)

    private val raceData = combine(
        f1Repository.getRacesForSeason(currentSeason),
        f1Repository.getDriverStandingsForSeason(currentSeason),
        f1Repository.getConstructorStandingsForSeason(currentSeason),
    ) { races, driverStandings, constructorStandings -> RaceData(races, driverStandings, constructorStandings) }

    private val entityData = combine(
        f1Repository.getAllDrivers(),
        f1Repository.getAllConstructors(),
        favoritesRepository.favoriteSelection,
    ) { drivers, constructors, selection -> EntityData(drivers, constructors, selection) }

    private val syncStatus = combine(isLoading, isRefreshing, loadErrorMessage) { loading, refreshing, error ->
        SyncStatus(loading, refreshing, error)
    }

    val uiState: StateFlow<HomeUiState> = combine(raceData, entityData, syncStatus) { races, entities, status ->
        buildUiState(races, entities, status)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    init {
        syncCurrentSeason(forceRefresh = false)
    }

    fun retrySync() {
        syncCurrentSeason(forceRefresh = false)
    }

    fun refresh() {
        syncCurrentSeason(forceRefresh = true)
    }

    private fun buildUiState(races: RaceData, entities: EntityData, status: SyncStatus): HomeUiState {
        val driverNamesById = entities.drivers.associateBy { it.driverId }
        val teamNamesById = entities.constructors.associateBy { it.constructorId }
        val calendar = buildCalendar(races.races)
        return HomeUiState(
            isLoading = status.isLoading,
            isRefreshing = status.isRefreshing,
            loadErrorMessage = status.loadErrorMessage,
            nextRace = calendar.find { it.status == RaceStatus.NEXT }
                ?.let { next -> races.races.find { it.round == next.round }?.toUpcomingRace() },
            calendar = calendar,
            favoriteDrivers = races.driverStandings
                .filter { it.driverId in entities.selection.driverIds }
                .sortedBy { it.position }
                .map { it.toFavoriteDriverStanding(driverNamesById, teamNamesById) },
            favoriteTeams = races.constructorStandings
                .filter { it.constructorId in entities.selection.teamIds }
                .sortedBy { it.position }
                .map { it.toFavoriteTeamStanding(teamNamesById) },
            driverLeaderPoints = races.driverStandings.maxOfOrNull { it.points } ?: 0.0,
            teamLeaderPoints = races.constructorStandings.maxOfOrNull { it.points } ?: 0.0,
        )
    }

    private fun buildCalendar(races: List<RaceEntity>): List<CalendarRace> {
        val sorted = races.sortedBy { it.round }
        val nextRound = sorted.firstOrNull { !it.isOver() }?.round
        return sorted.map { race ->
            val status = when {
                race.round == nextRound -> RaceStatus.NEXT
                !race.isOver() -> RaceStatus.UPCOMING
                else -> RaceStatus.COMPLETED
            }
            CalendarRace(race.round, race.raceName, race.date, race.circuitId, status, race.season, race.time)
        }
    }

    /**
     * Over once the race has had time to finish. Without a published start time, fall back to the
     * date: over from the next day (in app time). An unparseable date counts as over rather than
     * hiding the whole calendar.
     */
    private fun RaceEntity.isOver(): Boolean {
        sessionStart(date, time)?.let { return it.plus(RACE_DURATION) < Instant.now(clock) }
        return runCatching { LocalDate.parse(date) < LocalDate.now(clock.withZone(APP_ZONE)) }.getOrDefault(true)
    }

    private fun RaceEntity.toUpcomingRace(): UpcomingRace {
        return UpcomingRace(
            raceName = raceName,
            date = date,
            round = round,
            circuitId = circuitId,
            season = season,
            raceStart = sessionStart(date, time),
            qualifyingStart = sessionStart(qualifyingDate, qualifyingTime),
            sprintStart = sessionStart(sprintDate, sprintTime),
        )
    }

    private fun syncCurrentSeason(forceRefresh: Boolean) {
        if (forceRefresh) isRefreshing.value = true else isLoading.value = true
        loadErrorMessage.value = null
        viewModelScope.launch {
            syncCatching {
                f1Repository.syncSchedule(currentSeason, forceRefresh)
                f1Repository.syncDriverStandings(currentSeason, forceRefresh)
                f1Repository.syncConstructorStandings(currentSeason, forceRefresh)
            }.onFailure {
                loadErrorMessage.value = "Couldn't load the latest F1 data. Check your connection and try again."
            }
            isLoading.value = false
            isRefreshing.value = false
        }
    }

    private data class RaceData(
        val races: List<RaceEntity>,
        val driverStandings: List<DriverStandingEntity>,
        val constructorStandings: List<ConstructorStandingEntity>,
    )

    private data class EntityData(
        val drivers: List<DriverEntity>,
        val constructors: List<ConstructorEntity>,
        val selection: FavoriteSelection,
    )

    private data class SyncStatus(val isLoading: Boolean, val isRefreshing: Boolean, val loadErrorMessage: String?)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        fun DriverStandingEntity.toFavoriteDriverStanding(
            driversById: Map<String, DriverEntity>,
            teamsById: Map<String, ConstructorEntity>,
        ) = FavoriteDriverStanding(
            driverId = driverId,
            driverName = driversById[driverId]?.let { "${it.givenName} ${it.familyName}" } ?: driverId,
            teamName = constructorId?.let { teamsById[it]?.name },
            constructorId = constructorId,
            position = position,
            points = points,
        )

        fun ConstructorStandingEntity.toFavoriteTeamStanding(teamsById: Map<String, ConstructorEntity>) =
            FavoriteTeamStanding(
                teamId = constructorId,
                teamName = teamsById[constructorId]?.name ?: constructorId,
                position = position,
                points = points,
            )
    }
}
