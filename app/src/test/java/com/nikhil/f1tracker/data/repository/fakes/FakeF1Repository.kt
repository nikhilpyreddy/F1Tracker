package com.nikhil.f1tracker.data.repository.fakes

import com.nikhil.f1tracker.data.local.entity.CircuitEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorStandingEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.DriverStandingEntity
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity
import com.nikhil.f1tracker.data.repository.F1Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FakeF1Repository : F1Repository {
    val allDrivers = MutableStateFlow<List<DriverEntity>>(emptyList())
    val allConstructors = MutableStateFlow<List<ConstructorEntity>>(emptyList())
    val allCircuits = MutableStateFlow<List<CircuitEntity>>(emptyList())
    val allRaces = MutableStateFlow<List<RaceEntity>>(emptyList())
    val allResults = MutableStateFlow<List<ResultEntity>>(emptyList())
    val allDriverStandings = MutableStateFlow<List<DriverStandingEntity>>(emptyList())
    val allConstructorStandings = MutableStateFlow<List<ConstructorStandingEntity>>(emptyList())

    val syncedSeasons = mutableListOf<Int>()
    val syncedDriverStandingSeasons = mutableListOf<Int>()
    val syncedConstructorStandingSeasons = mutableListOf<Int>()

    override suspend fun syncSeason(season: Int, forceRefresh: Boolean) {
        syncedSeasons += season
    }

    override suspend fun syncSchedule(season: Int, forceRefresh: Boolean) = Unit

    override suspend fun syncDriverStandings(season: Int, forceRefresh: Boolean) {
        syncedDriverStandingSeasons += season
    }

    override suspend fun syncConstructorStandings(season: Int, forceRefresh: Boolean) {
        syncedConstructorStandingSeasons += season
    }

    override suspend fun syncDriverRoster(season: Int) = Unit

    override suspend fun syncConstructorRoster(season: Int) = Unit

    override fun getAllDrivers(): Flow<List<DriverEntity>> = allDrivers

    override fun getAllConstructors(): Flow<List<ConstructorEntity>> = allConstructors

    override fun getRacesForSeason(season: Int): Flow<List<RaceEntity>> =
        allRaces.map { races -> races.filter { it.season == season } }

    override fun getCircuit(circuitId: String): Flow<CircuitEntity?> =
        allCircuits.map { circuits -> circuits.find { it.circuitId == circuitId } }

    override fun getResultsForDriver(driverId: String): Flow<List<ResultEntity>> =
        allResults.map { results -> results.filter { it.driverId == driverId } }

    override fun getResultsForDriverAndSeason(driverId: String, season: Int): Flow<List<ResultEntity>> =
        allResults.map { results -> results.filter { it.driverId == driverId && it.season == season } }

    override fun getResultsForConstructorAndSeason(constructorId: String, season: Int): Flow<List<ResultEntity>> =
        allResults.map { results -> results.filter { it.constructorId == constructorId && it.season == season } }

    override fun getResultsForDriverAtCircuit(driverId: String, circuitId: String): Flow<List<ResultEntity>> =
        combine(allResults, allRaces) { results, races ->
            val roundsAtCircuit = races.filter { it.circuitId == circuitId }.map { it.season to it.round }.toSet()
            results.filter { it.driverId == driverId && (it.season to it.round) in roundsAtCircuit }
        }

    override fun getDriverStandingsForSeason(season: Int): Flow<List<DriverStandingEntity>> =
        allDriverStandings.map { standings -> standings.filter { it.season == season } }

    override fun getConstructorStandingsForSeason(season: Int): Flow<List<ConstructorStandingEntity>> =
        allConstructorStandings.map { standings -> standings.filter { it.season == season } }

    override fun getDriverStandingsAcrossSeasons(driverId: String, seasons: List<Int>): Flow<List<DriverStandingEntity>> =
        allDriverStandings.map { standings -> standings.filter { it.driverId == driverId && it.season in seasons } }

    override fun getConstructorStandingsAcrossSeasons(
        constructorId: String,
        seasons: List<Int>,
    ): Flow<List<ConstructorStandingEntity>> =
        allConstructorStandings.map { standings ->
            standings.filter { it.constructorId == constructorId && it.season in seasons }
        }
}
