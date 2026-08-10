package com.nikhil.f1tracker.domain.model

const val MAX_FAVORITE_DRIVERS = 4
const val MAX_FAVORITE_TEAMS = 2

data class FavoriteSelection(
    val driverIds: Set<String> = emptySet(),
    val teamIds: Set<String> = emptySet(),
)

sealed interface FavoriteToggleResult {
    data class Updated(val selection: FavoriteSelection) : FavoriteToggleResult
    data object LimitReached : FavoriteToggleResult
}

fun FavoriteSelection.toggleDriver(driverId: String): FavoriteToggleResult = when {
    driverId in driverIds -> FavoriteToggleResult.Updated(copy(driverIds = driverIds - driverId))
    driverIds.size >= MAX_FAVORITE_DRIVERS -> FavoriteToggleResult.LimitReached
    else -> FavoriteToggleResult.Updated(copy(driverIds = driverIds + driverId))
}

fun FavoriteSelection.toggleTeam(teamId: String): FavoriteToggleResult = when {
    teamId in teamIds -> FavoriteToggleResult.Updated(copy(teamIds = teamIds - teamId))
    teamIds.size >= MAX_FAVORITE_TEAMS -> FavoriteToggleResult.LimitReached
    else -> FavoriteToggleResult.Updated(copy(teamIds = teamIds + teamId))
}
