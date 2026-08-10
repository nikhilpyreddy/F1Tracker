package com.nikhil.f1tracker.ui.team

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.domain.model.lastFourSeasons
import com.nikhil.f1tracker.ui.common.ChartPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.Year
import javax.inject.Inject

@HiltViewModel
class TeamDetailViewModel @Inject constructor(
    private val f1Repository: F1Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val constructorId: String = checkNotNull(savedStateHandle["constructorId"])
    private val currentSeason = Year.now().value
    private val seasons = lastFourSeasons(currentSeason)

    private val isLoading = MutableStateFlow(true)
    private val loadErrorMessage = MutableStateFlow<String?>(null)

    private val teamInfo = f1Repository.getAllConstructors()
        .map { constructors -> constructors.find { it.constructorId == constructorId } }

    private val standingsAcrossSeasons = f1Repository.getConstructorStandingsAcrossSeasons(constructorId, seasons)

    private val syncStatus = combine(isLoading, loadErrorMessage) { l, e -> l to e }

    val uiState: StateFlow<TeamDetailUiState> = combine(
        teamInfo,
        standingsAcrossSeasons,
        syncStatus,
    ) { team, standings, status ->
        val (loading, error) = status
        val bySeasonYear = standings.associateBy { it.season }
        TeamDetailUiState(
            isLoading = loading,
            loadErrorMessage = error,
            teamName = team?.name.orEmpty(),
            nationality = team?.nationality,
            pointsTrend = seasons.map { year -> ChartPoint(year.toString(), bySeasonYear[year]?.points?.toFloat() ?: 0f) },
            seasonStandings = standings.sortedBy { it.season }.map {
                TeamSeasonStanding(it.season, it.position, it.points, it.wins)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TeamDetailUiState())

    init {
        viewModelScope.launch { runSync { seasons.forEach { f1Repository.syncConstructorStandings(it) } } }
    }

    fun retry() {
        viewModelScope.launch { runSync { seasons.forEach { f1Repository.syncConstructorStandings(it) } } }
    }

    private suspend fun runSync(block: suspend () -> Unit) {
        isLoading.value = true
        loadErrorMessage.value = null
        try {
            block()
        } catch (e: IOException) {
            loadErrorMessage.value = "Couldn't load team data. Check your connection and try again."
        } catch (e: HttpException) {
            loadErrorMessage.value = "Couldn't load team data. Check your connection and try again."
        } finally {
            isLoading.value = false
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
