package com.nikhil.f1tracker.data.repository

import com.nikhil.f1tracker.data.local.dao.ApiCacheDao
import com.nikhil.f1tracker.data.local.dao.RaceDao
import com.nikhil.f1tracker.data.local.entity.ApiCacheEntity
import com.nikhil.f1tracker.data.remote.openf1.OpenF1ApiService
import com.nikhil.f1tracker.data.remote.openf1.OpenF1CarDataDto
import com.nikhil.f1tracker.data.remote.openf1.OpenF1DriverDto
import com.nikhil.f1tracker.data.remote.openf1.OpenF1LocationDto
import com.nikhil.f1tracker.data.remote.openf1.OpenF1PitDto
import com.nikhil.f1tracker.data.remote.openf1.OpenF1RaceControlDto
import com.nikhil.f1tracker.data.remote.openf1.OpenF1SessionDto
import com.nikhil.f1tracker.data.remote.openf1.OpenF1StintDto
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The fastest lap of a qualifying session's pole-sitter, with its telemetry. */
@Serializable
data class TrackLapData(
    val year: Int,
    val driverCode: String?,
    val lapSeconds: Double,
    val car: List<OpenF1CarDataDto>,
    val location: List<OpenF1LocationDto>,
)

/** Everything the Strategy tab needs from one past race. */
@Serializable
data class RaceSessionData(
    val year: Int,
    val stints: List<OpenF1StintDto>,
    val drivers: List<OpenF1DriverDto>,
    /** Only Safety Car / VSC / red-flag messages. */
    val incidents: List<OpenF1RaceControlDto>,
    val overtakeCount: Int,
    val pitStops: List<OpenF1PitDto>,
)

private const val OPENF1_FIRST_SEASON = 2023
private val SESSION_LIST_TTL: Duration = Duration.ofDays(1)

/**
 * OpenF1 data for a circuit, cached in Room. Finished sessions never change, so their data is
 * kept forever; only the circuit's session list is refreshed (daily), to pick up new races.
 */
@Singleton
class OpenF1Repository @Inject constructor(
    private val api: OpenF1ApiService,
    private val cache: ApiCacheDao,
    private val raceDao: RaceDao,
    private val json: Json,
    private val clock: Clock,
) {

    /** The latest pole lap at [circuitId], or null if OpenF1 has no qualifying there. */
    suspend fun poleLap(circuitId: String): TrackLapData? {
        val session = pastSessions(circuitId).filter { it.sessionName == "Qualifying" }.maxByOrNull { it.dateStart }
            ?: return null
        return cached("poleLap:${session.sessionKey}", ttl = null) { fetchPoleLap(session) }
    }

    /** Past races at [circuitId] since 2023, newest first, emitted as each one loads. */
    fun raceSessions(circuitId: String): Flow<List<RaceSessionData>> = flow {
        val races = pastSessions(circuitId).filter { it.sessionName == "Race" }.sortedByDescending { it.dateStart }
        val loaded = mutableListOf<RaceSessionData>()
        races.forEach { session ->
            loaded += cached("race:${session.sessionKey}", ttl = null) { fetchRace(session) }
            emit(loaded.toList())
        }
        if (races.isEmpty()) emit(emptyList())
    }

    private suspend fun fetchPoleLap(session: OpenF1SessionDto): TrackLapData? {
        val pole = api.getSessionResult(session.sessionKey, position = 1).firstOrNull() ?: return null
        val lap = api.getLaps(session.sessionKey, pole.driverNumber)
            .filter { it.lapDuration != null && it.dateStart != null }
            .minByOrNull { it.lapDuration!! } ?: return null
        val start = OffsetDateTime.parse(lap.dateStart)
        val end = start.plusNanos((lap.lapDuration!! * NANOS_PER_SECOND).toLong())
        val code = sessionDrivers(session.sessionKey).find { it.driverNumber == pole.driverNumber }?.nameAcronym
        return TrackLapData(
            year = session.year,
            driverCode = code,
            lapSeconds = lap.lapDuration,
            car = api.getCarData(session.sessionKey, pole.driverNumber, start.toString(), end.toString()),
            location = api.getLocation(session.sessionKey, pole.driverNumber, start.toString(), end.toString()),
        )
    }

    private suspend fun fetchRace(session: OpenF1SessionDto): RaceSessionData = RaceSessionData(
        year = session.year,
        stints = api.getStints(session.sessionKey),
        drivers = sessionDrivers(session.sessionKey),
        incidents = api.getRaceControl(session.sessionKey).filter { it.isIncident() },
        overtakeCount = api.getOvertakes(session.sessionKey).size,
        pitStops = api.getPitStops(session.sessionKey),
    )

    private suspend fun sessionDrivers(sessionKey: Int): List<OpenF1DriverDto> =
        cached("drivers:$sessionKey", ttl = null) { api.getSessionDrivers(sessionKey) }

    private suspend fun pastSessions(circuitId: String): List<OpenF1SessionDto> {
        val circuitKey = circuitKey(circuitId) ?: return emptyList()
        val now = OffsetDateTime.now(clock)
        return cached("sessions:$circuitKey", ttl = SESSION_LIST_TTL) { api.getSessionsAtCircuit(circuitKey) }
            .filter { !it.isCancelled && it.year >= OPENF1_FIRST_SEASON }
            .filter { session -> session.dateEnd?.let { OffsetDateTime.parse(it) < now } ?: false }
    }

    /** OpenF1 has its own circuit keys; find ours by looking up a past race held there. */
    private suspend fun circuitKey(circuitId: String): Int? {
        val today = LocalDate.now(clock)
        val pastRace = raceDao.getByCircuit(circuitId).first()
            .filter { it.season >= OPENF1_FIRST_SEASON }
            .mapNotNull { race -> runCatching { LocalDate.parse(race.date) }.getOrNull() }
            .filter { it < today }
            .maxOrNull() ?: return null
        return cached("circuitKey:$circuitId", ttl = null) {
            api.getSessionsOnDay(pastRace.toString(), pastRace.plusDays(1).toString())
                .firstOrNull { it.sessionName == "Race" }?.circuitKey
        }
    }

    private suspend inline fun <reified T> cached(key: String, ttl: Duration?, fetch: () -> T): T {
        val nowMillis = clock.millis()
        cache.get(key)
            ?.takeIf { ttl == null || nowMillis - it.fetchedAtMillis < ttl.toMillis() }
            ?.let { return json.decodeFromString<T>(it.body) }
        val value = fetch()
        // Nulls aren't cached: "not found yet" (e.g. data still being published) should retry.
        if (value != null) cache.put(ApiCacheEntity(key, json.encodeToString(value), nowMillis))
        return value
    }

    private fun OpenF1RaceControlDto.isIncident(): Boolean =
        category == "SafetyCar" || (category == "Flag" && flag == "RED")

    private companion object {
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
