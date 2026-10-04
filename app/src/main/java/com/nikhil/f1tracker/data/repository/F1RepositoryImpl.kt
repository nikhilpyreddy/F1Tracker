package com.nikhil.f1tracker.data.repository

import com.nikhil.f1tracker.data.local.dao.CircuitDao
import com.nikhil.f1tracker.data.local.dao.ConstructorDao
import com.nikhil.f1tracker.data.local.dao.ConstructorStandingDao
import com.nikhil.f1tracker.data.local.dao.DriverDao
import com.nikhil.f1tracker.data.local.dao.DriverStandingDao
import com.nikhil.f1tracker.data.local.dao.RaceDao
import com.nikhil.f1tracker.data.local.dao.ResultDao
import com.nikhil.f1tracker.data.mapper.toEntity
import com.nikhil.f1tracker.data.remote.JOLPICA_MAX_PAGE_SIZE
import com.nikhil.f1tracker.data.remote.JolpicaApiService
import com.nikhil.f1tracker.data.remote.dto.RaceDto
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.Year
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
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
            fetchAndCacheSchedule(season)
            syncSeasonResults(season)
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

    // One request per 100 result rows (~5 per season) rather than one per round (~25), which is
    // what kept multi-season syncs bumping into Jolpica's 500 requests/hour limit.
    private suspend fun syncSeasonResults(season: Int) {
        var offset = 0
        do {
            val page = api.getSeasonResults(season, offset).mrData
            cacheRaceResults(page.raceTable.races)
            offset += JOLPICA_MAX_PAGE_SIZE
        } while (offset < page.total.toInt())
    }

    override suspend fun syncCircuitHistory(circuitId: String, sinceSeason: Int) =
        syncIfNeeded("circuitHistory:$circuitId", forceRefresh = false) {
            val currentSeason = Year.now(clock).value
            val hostedSeasons = api.getCircuitSeasons(circuitId).mrData.seasonTable.seasons
                .mapNotNull { it.season.toIntOrNull() }
                .filter { it in sinceSeason until currentSeason }
            hostedSeasons
                .filter { season -> resultDao.countAtCircuitInSeason(season, circuitId) == 0 }
                .forEach { season ->
                    val races = api.getCircuitResults(season, circuitId).mrData.raceTable.races
                    circuitDao.upsertAll(races.map { it.circuit.toEntity() }.distinctBy { it.circuitId })
                    raceDao.upsertAll(races.map { it.toEntity() })
                    cacheRaceResults(races)
                }
        }

    // A results page can end part-way through a race, so each race's rows are keyed by its own
    // season/round rather than assuming one race per response.
    private suspend fun cacheRaceResults(races: List<RaceDto>) {
        val results = races.flatMap { race -> race.results.map { race to it } }
        if (results.isEmpty()) return
        driverDao.upsertAll(results.map { (_, result) -> result.driver.toEntity() }.distinctBy { it.driverId })
        constructorDao.upsertAll(
            results.map { (_, result) -> result.constructor.toEntity() }.distinctBy { it.constructorId },
        )
        resultDao.upsertAll(results.map { (race, result) -> result.toEntity(race.season.toInt(), race.round.toInt()) })
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

    override fun getSeasonDrivers(season: Int) =
        combine(driverDao.getBySeason(season), driverDao.getBySeason(season - 1)) { current, previous ->
            current.ifEmpty { previous }
        }

    override fun getSeasonConstructors(season: Int) =
        combine(constructorDao.getBySeason(season), constructorDao.getBySeason(season - 1)) { current, previous ->
            current.ifEmpty { previous }
        }

    override fun getRacesForSeason(season: Int) = raceDao.getBySeason(season)

    override fun getCircuit(circuitId: String) = circuitDao.getById(circuitId)

    override fun getResultsForSeason(season: Int) = resultDao.getBySeason(season)

    override fun getResultsForDriver(driverId: String) = resultDao.getByDriver(driverId)

    override fun getResultsForDriverAndSeason(driverId: String, season: Int) =
        resultDao.getByDriverAndSeason(driverId, season)

    override fun getResultsForConstructorAndSeason(constructorId: String, season: Int) =
        resultDao.getByConstructorAndSeason(constructorId, season)

    override fun getResultsForDriverAtCircuit(driverId: String, circuitId: String) =
        resultDao.getByDriverAndCircuit(driverId, circuitId)

    override fun getResultsAtCircuit(circuitId: String) = resultDao.getByCircuit(circuitId)

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
