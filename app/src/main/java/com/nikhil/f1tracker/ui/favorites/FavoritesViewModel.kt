package com.nikhil.f1tracker.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.data.repository.FavoritesRepository
import com.nikhil.f1tracker.domain.model.FavoriteToggleResult
import com.nikhil.f1tracker.domain.model.MAX_FAVORITE_DRIVERS
import com.nikhil.f1tracker.domain.model.MAX_FAVORITE_TEAMS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
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
class FavoritesViewModel @Inject constructor(
    private val f1Repository: F1Repository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    private val currentSeason = Year.now().value
    private val isLoading = MutableStateFlow(true)
    private val loadErrorMessage = MutableStateFlow<String?>(null)
    private val limitReachedMessage = MutableStateFlow<String?>(null)

    private val syncStatus: Flow<SyncStatus> = combine(
        isLoading,
        loadErrorMessage,
        limitReachedMessage,
    ) { loading, loadError, limitMessage -> SyncStatus(loading, loadError, limitMessage) }

    val uiState: StateFlow<FavoritesUiState> = combine(
        f1Repository.getAllDrivers(),
        f1Repository.getAllConstructors(),
        favoritesRepository.favoriteSelection,
        syncStatus,
    ) { drivers, teams, selection, status ->
        FavoritesUiState(
            isLoading = status.isLoading,
            loadErrorMessage = status.loadErrorMessage,
            drivers = drivers.sortedBy { it.familyName },
            teams = teams.sortedBy { it.name },
            selection = selection,
            limitReachedMessage = status.limitReachedMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FavoritesUiState())

    init {
        syncRoster()
    }

    fun retryRosterSync() {
        syncRoster()
    }

    fun toggleDriver(driverId: String) {
        viewModelScope.launch {
            val result = favoritesRepository.toggleDriver(driverId)
            if (result is FavoriteToggleResult.LimitReached) {
                limitReachedMessage.value = "You can only favorite up to $MAX_FAVORITE_DRIVERS drivers"
            }
        }
    }

    fun toggleTeam(teamId: String) {
        viewModelScope.launch {
            val result = favoritesRepository.toggleTeam(teamId)
            if (result is FavoriteToggleResult.LimitReached) {
                limitReachedMessage.value = "You can only favorite up to $MAX_FAVORITE_TEAMS teams"
            }
        }
    }

    fun limitMessageShown() {
        limitReachedMessage.value = null
    }

    private fun syncRoster() {
        isLoading.value = true
        loadErrorMessage.value = null
        viewModelScope.launch {
            try {
                f1Repository.syncDriverRoster(currentSeason)
                f1Repository.syncConstructorRoster(currentSeason)
            } catch (e: IOException) {
                loadErrorMessage.value = "Couldn't load drivers and teams. Check your connection and try again."
            } catch (e: HttpException) {
                loadErrorMessage.value = "Couldn't load drivers and teams. Check your connection and try again."
            } finally {
                isLoading.value = false
            }
        }
    }

    private data class SyncStatus(
        val isLoading: Boolean,
        val loadErrorMessage: String?,
        val limitReachedMessage: String?,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
