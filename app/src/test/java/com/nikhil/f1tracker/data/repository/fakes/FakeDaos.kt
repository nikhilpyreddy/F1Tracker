package com.nikhil.f1tracker.data.repository.fakes

import com.nikhil.f1tracker.data.local.dao.CircuitDao
import com.nikhil.f1tracker.data.local.dao.ConstructorDao
import com.nikhil.f1tracker.data.local.dao.ConstructorStandingDao
import com.nikhil.f1tracker.data.local.dao.DriverDao
import com.nikhil.f1tracker.data.local.dao.DriverStandingDao
import com.nikhil.f1tracker.data.local.dao.RaceDao
import com.nikhil.f1tracker.data.local.dao.ResultDao
import com.nikhil.f1tracker.data.local.entity.CircuitEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorStandingEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.DriverStandingEntity
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeDriverDao : DriverDao {
    val upserted = mutableMapOf<String, DriverEntity>()
    override suspend fun upsertAll(drivers: List<DriverEntity>) {
        drivers.forEach { upserted[it.driverId] = it }
    }
    override fun getAll(): Flow<List<DriverEntity>> = flowOf(upserted.values.toList())
    override fun getById(driverId: String): Flow<DriverEntity?> = flowOf(upserted[driverId])
    override fun getByIds(driverIds: List<String>): Flow<List<DriverEntity>> =
        flowOf(driverIds.mapNotNull { upserted[it] })
}

class FakeConstructorDao : ConstructorDao {
    val upserted = mutableMapOf<String, ConstructorEntity>()
    override suspend fun upsertAll(constructors: List<ConstructorEntity>) {
        constructors.forEach { upserted[it.constructorId] = it }
    }
    override fun getAll(): Flow<List<ConstructorEntity>> = flowOf(upserted.values.toList())
    override fun getById(constructorId: String): Flow<ConstructorEntity?> = flowOf(upserted[constructorId])
    override fun getByIds(constructorIds: List<String>): Flow<List<ConstructorEntity>> =
        flowOf(constructorIds.mapNotNull { upserted[it] })
}

class FakeCircuitDao : CircuitDao {
    val upserted = mutableMapOf<String, CircuitEntity>()
    override suspend fun upsertAll(circuits: List<CircuitEntity>) {
        circuits.forEach { upserted[it.circuitId] = it }
    }
    override fun getAll(): Flow<List<CircuitEntity>> = flowOf(upserted.values.toList())
    override fun getById(circuitId: String): Flow<CircuitEntity?> = flowOf(upserted[circuitId])
}

class FakeRaceDao : RaceDao {
    val upserted = mutableListOf<RaceEntity>()
    override suspend fun upsertAll(races: List<RaceEntity>) {
        upserted += races
    }
    override fun getBySeason(season: Int): Flow<List<RaceEntity>> =
        flowOf(upserted.filter { it.season == season })
    override fun getBySeasonAndRound(season: Int, round: Int): Flow<RaceEntity?> =
        flowOf(upserted.find { it.season == season && it.round == round })
    override fun getByCircuit(circuitId: String): Flow<List<RaceEntity>> =
        flowOf(upserted.filter { it.circuitId == circuitId })
}

class FakeResultDao(private val raceDao: FakeRaceDao) : ResultDao {
    val upserted = mutableListOf<ResultEntity>()
    override suspend fun upsertAll(results: List<ResultEntity>) {
        upserted += results
    }
    override fun getByRace(season: Int, round: Int): Flow<List<ResultEntity>> =
        flowOf(upserted.filter { it.season == season && it.round == round })
    override fun getByDriver(driverId: String): Flow<List<ResultEntity>> =
        flowOf(upserted.filter { it.driverId == driverId }.sortedWith(seasonRoundDescending))
    override fun getByDriverAndSeason(driverId: String, season: Int): Flow<List<ResultEntity>> =
        flowOf(upserted.filter { it.driverId == driverId && it.season == season })
    override fun getByConstructorAndSeason(constructorId: String, season: Int): Flow<List<ResultEntity>> =
        flowOf(upserted.filter { it.constructorId == constructorId && it.season == season })
    override fun getByDriverAndCircuit(driverId: String, circuitId: String): Flow<List<ResultEntity>> {
        val roundsAtCircuit = raceDao.upserted
            .filter { it.circuitId == circuitId }
            .map { it.season to it.round }
            .toSet()
        return flowOf(
            upserted.filter { it.driverId == driverId && (it.season to it.round) in roundsAtCircuit }
                .sortedByDescending { it.season },
        )
    }

    private companion object {
        val seasonRoundDescending = compareByDescending<ResultEntity> { it.season }.thenByDescending { it.round }
    }
}

class FakeDriverStandingDao : DriverStandingDao {
    val upserted = mutableListOf<DriverStandingEntity>()
    override suspend fun upsertAll(standings: List<DriverStandingEntity>) {
        upserted += standings
    }
    override fun getBySeason(season: Int): Flow<List<DriverStandingEntity>> =
        flowOf(upserted.filter { it.season == season })
    override fun getByDriverAndSeasons(driverId: String, seasons: List<Int>): Flow<List<DriverStandingEntity>> =
        flowOf(upserted.filter { it.driverId == driverId && it.season in seasons })
}

class FakeConstructorStandingDao : ConstructorStandingDao {
    val upserted = mutableListOf<ConstructorStandingEntity>()
    override suspend fun upsertAll(standings: List<ConstructorStandingEntity>) {
        upserted += standings
    }
    override fun getBySeason(season: Int): Flow<List<ConstructorStandingEntity>> =
        flowOf(upserted.filter { it.season == season })
    override fun getByConstructorAndSeasons(
        constructorId: String,
        seasons: List<Int>,
    ): Flow<List<ConstructorStandingEntity>> =
        flowOf(upserted.filter { it.constructorId == constructorId && it.season in seasons })
}
