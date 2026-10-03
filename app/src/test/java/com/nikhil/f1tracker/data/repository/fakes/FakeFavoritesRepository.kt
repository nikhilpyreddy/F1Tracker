package com.nikhil.f1tracker.data.repository.fakes

import com.nikhil.f1tracker.data.repository.FavoritesRepository
import com.nikhil.f1tracker.domain.model.FavoriteSelection
import com.nikhil.f1tracker.domain.model.FavoriteToggleResult
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFavoritesRepository(
    initial: FavoriteSelection = FavoriteSelection(),
) : FavoritesRepository {
    override val favoriteSelection = MutableStateFlow(initial)

    override suspend fun toggleDriver(driverId: String): FavoriteToggleResult =
        error("Not used by these tests")

    override suspend fun toggleTeam(teamId: String): FavoriteToggleResult =
        error("Not used by these tests")
}
