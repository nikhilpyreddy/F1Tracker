package com.nikhil.f1tracker.data.repository

import com.nikhil.f1tracker.data.local.dao.CircuitDao
import com.nikhil.f1tracker.data.local.dao.ConstructorDao
import com.nikhil.f1tracker.data.local.dao.ConstructorStandingDao
import com.nikhil.f1tracker.data.local.dao.DriverDao
import com.nikhil.f1tracker.data.local.dao.DriverStandingDao
import com.nikhil.f1tracker.data.local.dao.RaceDao
import com.nikhil.f1tracker.data.local.dao.ResultDao
import com.nikhil.f1tracker.data.mapper.toEntity
import com.nikhil.f1tracker.data.remote.JolpicaApiService
import com.nikhil.f1tracker.data.remote.dto.RaceDto
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.Year
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class F1RepositoryImpl @Inject constructor(
    private val api: JolpicaApiService,
    private val driverDao: DriverDao,
    private val constructorDao: ConstructorDao,
    private val circuitDao: CircuitDao,
    private val raceDao: RaceDao,
    private val resultDao: ResultDao,
    private val driverStandingDao: DriverStandingDao,
    private val constructorStandingDao: ConstructorStandingDao,
    private val clock: Clock,
) : F1Repository {

    // Keyed by sync operation + season. In-memory only: worst case after a
    // process restart is one redundant refresh, which is fine for cached
    // network data that a full sync would repopulate identically anyway.
    private val lastSyncedAt = mutableMapOf<String, Instant>()

    override suspend fun syncSeason(season: Int, forceRefresh: Boolean) {
        if (!forceRefresh && isCompletedSeasonFullyCached(season)) return
        syncIfNeeded("season:$season", forceRefresh) {
            val races = fetchAndCacheSchedule(season)
            races.forEach { race -> syncRaceResults(season, race.round.toInt()) }
            syncDriverStandings(season, forceRefresh = true)
            syncConstructorStandings(season, forceRefresh = true)
        }
    }

    // A past season's results never change once complete, so once every scheduled race has
    // results cached we can skip it for good — this is what keeps a multi-year sync (Grand
    // Prix/Driver detail) from re-fetching ~25 requests per season on every app restart, which
    // is what was tripping Jolpica's rate limit before this year's own data ever got synced.
    private suspend fun isCompletedSeasonFullyCached(season: Int): Boolean {
        if (season >= Year.now(clock).value) return false
        val races = raceDao.getBySeason(season).first()
        if (races.isEmpty()) return false
        val roundsWithResults = resultDao.getRoundsWithResults(season).toSet()
        return races.all { it.round in roundsWithResults }
    }

    override suspend fun syncSchedule(season: Int, forceRefresh: Boolean) =
        syncIfNeeded("schedule:$season", forceRefresh) { fetchAndCacheSchedule(season) }

    private suspend fun fetchAndCacheSchedule(season: Int): List<RaceDto> {
        val schedule = api.getSeasonSchedule(season).mrData.raceTable.races
        circuitDao.upsertAll(schedule.map { it.circuit.toEntity() })
        raceDao.upsertAll(schedule.map { it.toEntity() })
        return schedule
    }

    private suspend fun syncRaceResults(season: Int, round: Int) {
        val race = api.getRaceResults(season, round).mrData.raceTable.races.firstOrNull() ?: return
        val results = race.results
        if (results.isEmpty()) return
        driverDao.upsertAll(results.map { it.driver.toEntity() }.distinctBy { it.driverId })
        constructorDao.upsertAll(results.map { it.constructor.toEntity() }.distinctBy { it.constructorId })
        resultDao.upsertAll(results.map { it.toEntity(season, round) })
    }

    override suspend fun syncDriverStandings(season: Int, forceRefresh: Boolean) =
        syncIfNeeded("driverStandings:$season", forceRefresh) {
            val standingsList = api.getDriverStandings(season).mrData.standingsTable.standingsLists.firstOrNull()
                ?: return@syncIfNeeded
            val standings = standingsList.driverStandings
            driverDao.upsertAll(standings.map { it.driver.toEntity() })
            constructorDao.upsertAll(
                standings.flatMap { it.constructors }.map { it.toEntity() }.distinctBy { it.constructorId },
            )
            driverStandingDao.upsertAll(standings.map { it.toEntity(season) })
        }

    override suspend fun syncConstructorStandings(season: Int, forceRefresh: Boolean) =
        syncIfNeeded("constructorStandings:$season", forceRefresh) {
            val standingsList =
                api.getConstructorStandings(season).mrData.standingsTable.standingsLists.firstOrNull()
                    ?: return@syncIfNeeded
            val standings = standingsList.constructorStandings
            constructorDao.upsertAll(standings.map { it.constructor.toEntity() })
            constructorStandingDao.upsertAll(standings.map { it.toEntity(season) })
        }

    override suspend fun syncDriverRoster(season: Int) =
        syncIfNeeded("driverRoster:$season", forceRefresh = false) {
            val drivers = api.getDrivers(season).mrData.driverTable.drivers
            driverDao.upsertAll(drivers.map { it.toEntity() })
        }

    override suspend fun syncConstructorRoster(season: Int) =
        syncIfNeeded("constructorRoster:$season", forceRefresh = false) {
            val constructors = api.getConstructors(season).mrData.constructorTable.constructors
            constructorDao.upsertAll(constructors.map { it.toEntity() })
        }

    private suspend fun syncIfNeeded(key: String, forceRefresh: Boolean, block: suspend () -> Unit) {
        if (!forceRefresh && isFresh(key)) return
        block()
        lastSyncedAt[key] = Instant.now(clock)
    }

    private fun isFresh(key: String): Boolean {
        val syncedAt = lastSyncedAt[key] ?: return false
        return Duration.between(syncedAt, Instant.now(clock)) < SYNC_TTL
    }

    override fun getAllDrivers() = driverDao.getAll()

    override fun getAllConstructors() = constructorDao.getAll()

    override fun getRacesForSeason(season: Int) = raceDao.getBySeason(season)

    override fun getCircuit(circuitId: String) = circuitDao.getById(circuitId)

    override fun getResultsForDriver(driverId: String) = resultDao.getByDriver(driverId)

    override fun getResultsForDriverAndSeason(driverId: String, season: Int) =
        resultDao.getByDriverAndSeason(driverId, season)

    override fun getResultsForConstructorAndSeason(constructorId: String, season: Int) =
        resultDao.getByConstructorAndSeason(constructorId, season)

    override fun getResultsForDriverAtCircuit(driverId: String, circuitId: String) =
        resultDao.getByDriverAndCircuit(driverId, circuitId)

    override fun getDriverStandingsForSeason(season: Int) = driverStandingDao.getBySeason(season)

    override fun getConstructorStandingsForSeason(season: Int) = constructorStandingDao.getBySeason(season)

    override fun getDriverStandingsAcrossSeasons(driverId: String, seasons: List<Int>) =
        driverStandingDao.getByDriverAndSeasons(driverId, seasons)

    override fun getConstructorStandingsAcrossSeasons(constructorId: String, seasons: List<Int>) =
        constructorStandingDao.getByConstructorAndSeasons(constructorId, seasons)

    private companion object {
        val SYNC_TTL: Duration = Duration.ofMinutes(15)
    }
}
