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
import com.nikhil.f1tracker.domain.model.FavoriteSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.Year
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val f1Repository: F1Repository,
    favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val currentSeason = Year.now().value
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
        return HomeUiState(
            isLoading = status.isLoading,
            isRefreshing = status.isRefreshing,
            loadErrorMessage = status.loadErrorMessage,
            nextRace = findNextRace(races.races),
            favoriteDrivers = races.driverStandings
                .filter { it.driverId in entities.selection.driverIds }
                .sortedBy { it.position }
                .map { it.toFavoriteDriverStanding(driverNamesById, teamNamesById) },
            favoriteTeams = races.constructorStandings
                .filter { it.constructorId in entities.selection.teamIds }
                .sortedBy { it.position }
                .map { it.toFavoriteTeamStanding(teamNamesById) },
        )
    }

    private fun findNextRace(races: List<RaceEntity>): UpcomingRace? {
        val today = LocalDate.now()
        return races
            .filter { runCatching { LocalDate.parse(it.date) >= today }.getOrDefault(false) }
            .minByOrNull { it.round }
            ?.let { UpcomingRace(raceName = it.raceName, date = it.date, round = it.round, circuitId = it.circuitId) }
    }

    private fun syncCurrentSeason(forceRefresh: Boolean) {
        if (forceRefresh) isRefreshing.value = true else isLoading.value = true
        loadErrorMessage.value = null
        viewModelScope.launch {
            try {
                f1Repository.syncSchedule(currentSeason, forceRefresh)
                f1Repository.syncDriverStandings(currentSeason, forceRefresh)
                f1Repository.syncConstructorStandings(currentSeason, forceRefresh)
            } catch (e: IOException) {
                loadErrorMessage.value = "Couldn't load the latest F1 data. Check your connection and try again."
            } catch (e: HttpException) {
                loadErrorMessage.value = "Couldn't load the latest F1 data. Check your connection and try again."
            } finally {
                isLoading.value = false
                isRefreshing.value = false
            }
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
