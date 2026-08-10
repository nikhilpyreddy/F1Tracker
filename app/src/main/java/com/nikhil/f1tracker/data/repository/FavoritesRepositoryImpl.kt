package com.nikhil.f1tracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.nikhil.f1tracker.domain.model.FavoriteSelection
import com.nikhil.f1tracker.domain.model.FavoriteToggleResult
import com.nikhil.f1tracker.domain.model.toggleDriver
import com.nikhil.f1tracker.domain.model.toggleTeam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val DRIVER_IDS_KEY = stringSetPreferencesKey("favorite_driver_ids")
private val TEAM_IDS_KEY = stringSetPreferencesKey("favorite_team_ids")

class FavoritesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : FavoritesRepository {

    override val favoriteSelection: Flow<FavoriteSelection> = dataStore.data.map { it.toSelection() }

    // The read-modify-write happens inside a single edit{} transaction so a rapid
    // double-toggle can't race and silently drop one of the updates.
    override suspend fun toggleDriver(driverId: String): FavoriteToggleResult {
        var result: FavoriteToggleResult = FavoriteToggleResult.LimitReached
        dataStore.edit { prefs ->
            val toggled = prefs.toSelection().toggleDriver(driverId)
            result = toggled
            if (toggled is FavoriteToggleResult.Updated) {
                prefs[DRIVER_IDS_KEY] = toggled.selection.driverIds
            }
        }
        return result
    }

    override suspend fun toggleTeam(teamId: String): FavoriteToggleResult {
        var result: FavoriteToggleResult = FavoriteToggleResult.LimitReached
        dataStore.edit { prefs ->
            val toggled = prefs.toSelection().toggleTeam(teamId)
            result = toggled
            if (toggled is FavoriteToggleResult.Updated) {
                prefs[TEAM_IDS_KEY] = toggled.selection.teamIds
            }
        }
        return result
    }

    private fun Preferences.toSelection() = FavoriteSelection(
        driverIds = this[DRIVER_IDS_KEY] ?: emptySet(),
        teamIds = this[TEAM_IDS_KEY] ?: emptySet(),
    )
}
