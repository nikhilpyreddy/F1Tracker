package com.nikhil.f1tracker.data.repository

import com.nikhil.f1tracker.domain.model.FavoriteSelection
import com.nikhil.f1tracker.domain.model.FavoriteToggleResult
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    val favoriteSelection: Flow<FavoriteSelection>
    suspend fun toggleDriver(driverId: String): FavoriteToggleResult
    suspend fun toggleTeam(teamId: String): FavoriteToggleResult
}
