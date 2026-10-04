package com.nikhil.f1tracker.data.repository

import com.nikhil.f1tracker.data.local.dao.CircuitDao
import com.nikhil.f1tracker.data.local.dao.QualifyingDao
import com.nikhil.f1tracker.data.local.dao.RaceDao
import com.nikhil.f1tracker.data.local.entity.QualifyingEntity
import com.nikhil.f1tracker.data.mapper.toEntity
import com.nikhil.f1tracker.data.remote.JOLPICA_MAX_PAGE_SIZE
import com.nikhil.f1tracker.data.remote.JolpicaApiService
import com.nikhil.f1tracker.data.remote.dto.RaceDto
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.Year
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

private val SEASON_QUALIFYING_TTL: Duration = Duration.ofMinutes(15)

/** Qualifying classifications from Jolpica, cached in Room. */
@Singleton
class QualifyingRepository @Inject constructor(
    private val api: JolpicaApiService,
    private val qualifyingDao: QualifyingDao,
    private val raceDao: RaceDao,
    private val circuitDao: CircuitDao,
    private val clock: Clock,
) {
    private val seasonSyncedAt = mutableMapOf<Int, Instant>()

    fun getSeason(season: Int): Flow<List<QualifyingEntity>> = qualifyingDao.getBySeason(season)

    fun getAtCircuit(circuitId: String): Flow<List<QualifyingEntity>> = qualifyingDao.getByCircuit(circuitId)

    /** The whole season, ~5 pages. Re-fetched after 15 minutes so a just-finished session shows up. */
    suspend fun syncSeason(season: Int, forceRefresh: Boolean = false) {
        val last = seasonSyncedAt[season]
        if (!forceRefresh && last != null && Duration.between(last, Instant.now(clock)) < SEASON_QUALIFYING_TTL) return
        var offset = 0
        do {
            val page = api.getSeasonQualifying(season, offset).mrData
            cache(page.raceTable.races)
            offset += JOLPICA_MAX_PAGE_SIZE
        } while (offset < page.total.toInt())
        seasonSyncedAt[season] = Instant.now(clock)
    }

    /** Past seasons' qualifying at [circuitId] since [sinceSeason]; already-cached seasons are skipped. */
    suspend fun syncCircuit(circuitId: String, sinceSeason: Int) {
        val currentSeason = Year.now(clock).value
        api.getCircuitSeasons(circuitId).mrData.seasonTable.seasons
            .mapNotNull { it.season.toIntOrNull() }
            .filter { it in sinceSeason until currentSeason }
            .filter { qualifyingDao.countAtCircuitInSeason(it, circuitId) == 0 }
            .forEach { season ->
                val races = api.getCircuitQualifying(season, circuitId).mrData.raceTable.races
                circuitDao.upsertAll(races.map { it.circuit.toEntity() }.distinctBy { it.circuitId })
                raceDao.upsertAll(races.map { it.toEntity() })
                cache(races)
            }
    }

    private suspend fun cache(races: List<RaceDto>) {
        val rows = races.flatMap { race ->
            race.qualifyingResults.mapNotNull { it.toEntity(race.season.toInt(), race.round.toInt()) }
        }
        if (rows.isNotEmpty()) qualifyingDao.upsertAll(rows)
    }
}
