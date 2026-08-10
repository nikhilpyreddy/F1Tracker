package com.nikhil.f1tracker.ui.favorites

import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.domain.model.FavoriteSelection

data class FavoritesUiState(
    val isLoading: Boolean = true,
    val loadErrorMessage: String? = null,
    val drivers: List<DriverEntity> = emptyList(),
    val teams: List<ConstructorEntity> = emptyList(),
    val selection: FavoriteSelection = FavoriteSelection(),
    val limitReachedMessage: String? = null,
)
