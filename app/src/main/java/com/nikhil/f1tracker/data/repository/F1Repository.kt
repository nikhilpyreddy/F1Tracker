package com.nikhil.f1tracker.data.repository

import com.nikhil.f1tracker.data.local.entity.CircuitEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorStandingEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.DriverStandingEntity
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity
import kotlinx.coroutines.flow.Flow

interface F1Repository {

    suspend fun syncSeason(season: Int, forceRefresh: Boolean = false)
    suspend fun syncSchedule(season: Int, forceRefresh: Boolean = false)
    suspend fun syncDriverStandings(season: Int, forceRefresh: Boolean = false)
    suspend fun syncConstructorStandings(season: Int, forceRefresh: Boolean = false)
    suspend fun syncDriverRoster(season: Int)
    suspend fun syncConstructorRoster(season: Int)

    /** Caches results of every completed season since [sinceSeason] in which [circuitId] hosted a race. */
    suspend fun syncCircuitHistory(circuitId: String, sinceSeason: Int)

    fun getAllDrivers(): Flow<List<DriverEntity>>
    fun getAllConstructors(): Flow<List<ConstructorEntity>>

    /** Drivers/teams racing in [season]; before its first race, last season's grid instead. */
    fun getSeasonDrivers(season: Int): Flow<List<DriverEntity>>
    fun getSeasonConstructors(season: Int): Flow<List<ConstructorEntity>>

    fun getRacesForSeason(season: Int): Flow<List<RaceEntity>>
    fun getCircuit(circuitId: String): Flow<CircuitEntity?>

    fun getResultsForSeason(season: Int): Flow<List<ResultEntity>>
    fun getResultsForDriver(driverId: String): Flow<List<ResultEntity>>
    fun getResultsForDriverAndSeason(driverId: String, season: Int): Flow<List<ResultEntity>>
    fun getResultsForConstructorAndSeason(constructorId: String, season: Int): Flow<List<ResultEntity>>
    fun getResultsForDriverAtCircuit(driverId: String, circuitId: String): Flow<List<ResultEntity>>
    fun getResultsAtCircuit(circuitId: String): Flow<List<ResultEntity>>

    fun getDriverStandingsForSeason(season: Int): Flow<List<DriverStandingEntity>>
    fun getConstructorStandingsForSeason(season: Int): Flow<List<ConstructorStandingEntity>>
    fun getDriverStandingsAcrossSeasons(driverId: String, seasons: List<Int>): Flow<List<DriverStandingEntity>>
    fun getConstructorStandingsAcrossSeasons(
        constructorId: String,
        seasons: List<Int>,
    ): Flow<List<ConstructorStandingEntity>>
}
