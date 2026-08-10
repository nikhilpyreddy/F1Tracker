package com.nikhil.f1tracker.data.repository

import com.nikhil.f1tracker.data.remote.dto.CircuitDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorStandingDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorStandingsListDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorStandingsMrDataDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorStandingsResponseDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorStandingsTableDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorTableResponseDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorTableMrDataDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorTableDto
import com.nikhil.f1tracker.data.remote.dto.DriverDto
import com.nikhil.f1tracker.data.remote.dto.DriverStandingDto
import com.nikhil.f1tracker.data.remote.dto.DriverStandingsListDto
import com.nikhil.f1tracker.data.remote.dto.DriverStandingsMrDataDto
import com.nikhil.f1tracker.data.remote.dto.DriverStandingsResponseDto
import com.nikhil.f1tracker.data.remote.dto.DriverStandingsTableDto
import com.nikhil.f1tracker.data.remote.dto.DriverTableResponseDto
import com.nikhil.f1tracker.data.remote.dto.DriverTableMrDataDto
import com.nikhil.f1tracker.data.remote.dto.DriverTableDto
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity
import com.nikhil.f1tracker.data.remote.dto.LocationDto
import com.nikhil.f1tracker.data.remote.dto.RaceDto
import com.nikhil.f1tracker.data.remote.dto.RaceMrDataDto
import com.nikhil.f1tracker.data.remote.dto.RaceResponseDto
import com.nikhil.f1tracker.data.remote.dto.RaceTableDto
import com.nikhil.f1tracker.data.remote.dto.ResultDto
import com.nikhil.f1tracker.data.repository.fakes.FakeCircuitDao
import com.nikhil.f1tracker.data.repository.fakes.FakeConstructorDao
import com.nikhil.f1tracker.data.repository.fakes.FakeConstructorStandingDao
import com.nikhil.f1tracker.data.repository.fakes.FakeDriverDao
import com.nikhil.f1tracker.data.repository.fakes.FakeDriverStandingDao
import com.nikhil.f1tracker.data.repository.fakes.FakeJolpicaApiService
import com.nikhil.f1tracker.data.repository.fakes.FakeRaceDao
import com.nikhil.f1tracker.data.repository.fakes.FakeResultDao
import com.nikhil.f1tracker.data.repository.fakes.MutableClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant

class F1RepositoryImplTest {

    private val driver = DriverDto(
        driverId = "max_verstappen",
        givenName = "Max",
        familyName = "Verstappen",
        dateOfBirth = "1997-09-30",
        nationality = "Dutch",
    )
    private val constructor = ConstructorDto(constructorId = "red_bull", name = "Red Bull", nationality = "Austrian")
    private val circuit = CircuitDto(
        circuitId = "bahrain",
        circuitName = "Bahrain International Circuit",
        location = LocationDto(lat = "26.0325", long = "50.5106", locality = "Sakhir", country = "Bahrain"),
    )

    private lateinit var driverDao: FakeDriverDao
    private lateinit var constructorDao: FakeConstructorDao
    private lateinit var circuitDao: FakeCircuitDao
    private lateinit var raceDao: FakeRaceDao
    private lateinit var resultDao: FakeResultDao
    private lateinit var driverStandingDao: FakeDriverStandingDao
    private lateinit var constructorStandingDao: FakeConstructorStandingDao

    @Before
    fun setUp() {
        driverDao = FakeDriverDao()
        constructorDao = FakeConstructorDao()
        circuitDao = FakeCircuitDao()
        raceDao = FakeRaceDao()
        resultDao = FakeResultDao(raceDao)
        driverStandingDao = FakeDriverStandingDao()
        constructorStandingDao = FakeConstructorStandingDao()
    }

    @Test
    fun `syncSeason caches schedule, results and standings from a single race season`() = runTest {
        // Arrange
        val scheduleRace = RaceDto(
            season = "2099",
            round = "1",
            raceName = "Bahrain Grand Prix",
            circuit = circuit,
            date = "2099-03-02",
        )
        val resultsRace = scheduleRace.copy(
            results = listOf(
                ResultDto(
                    position = "1",
                    positionText = "1",
                    points = "26",
                    driver = driver,
                    constructor = constructor,
                    grid = "1",
                    laps = "57",
                    status = "Finished",
                ),
            ),
        )
        val api = FakeJolpicaApiService(
            schedule = RaceResponseDto(
                RaceMrDataDto("30", "0", "1", RaceTableDto(races = listOf(scheduleRace))),
            ),
            resultsByRound = mapOf(
                1 to RaceResponseDto(RaceMrDataDto("30", "0", "1", RaceTableDto(races = listOf(resultsRace)))),
            ),
            driverStandings = DriverStandingsResponseDto(
                DriverStandingsMrDataDto(
                    "30", "0", "1",
                    DriverStandingsTableDto(
                        season = "2099",
                        standingsLists = listOf(
                            DriverStandingsListDto(
                                season = "2099",
                                round = "1",
                                driverStandings = listOf(
                                    DriverStandingDto("1", "1", "26", "1", driver, listOf(constructor)),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            constructorStandings = ConstructorStandingsResponseDto(
                ConstructorStandingsMrDataDto(
                    "30", "0", "1",
                    ConstructorStandingsTableDto(
                        season = "2099",
                        standingsLists = listOf(
                            ConstructorStandingsListDto(
                                season = "2099",
                                round = "1",
                                constructorStandings = listOf(
                                    ConstructorStandingDto("1", "1", "26", "1", constructor),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            drivers = DriverTableResponseDto(DriverTableMrDataDto("30", "0", "0", DriverTableDto())),
            constructors = ConstructorTableResponseDto(ConstructorTableMrDataDto("30", "0", "0", ConstructorTableDto())),
        )
        val repository = buildRepository(api)

        // Act
        repository.syncSeason(2099)

        // Assert
        assertEquals(1, circuitDao.upserted.size)
        assertEquals(1, raceDao.upserted.size)
        assertEquals(2099, raceDao.upserted.single().season)
        assertEquals(1, resultDao.upserted.size)
        assertEquals("max_verstappen", resultDao.upserted.single().driverId)
        assertEquals(1, driverDao.upserted.size)
        assertEquals(1, constructorDao.upserted.size)
        assertEquals(1, driverStandingDao.upserted.size)
        assertEquals(26.0, driverStandingDao.upserted.single().points, 0.0001)
        assertEquals(1, constructorStandingDao.upserted.size)
    }

    @Test
    fun `syncSeason skips a past season that is already fully cached`() = runTest {
        // Arrange
        raceDao.upserted += RaceEntity(
            season = 2023, round = 1, raceName = "Bahrain Grand Prix",
            circuitId = "bahrain", date = "2023-03-05", time = null,
        )
        resultDao.upserted += ResultEntity(
            season = 2023, round = 1, driverId = "max_verstappen", constructorId = "red_bull",
            position = 1, positionText = "1", points = 25.0, grid = 1, laps = 57, status = "Finished",
            finishTimeMillis = null, fastestLapRank = null, fastestLapTime = null,
        )
        val api = FakeJolpicaApiService(
            schedule = RaceResponseDto(RaceMrDataDto("30", "0", "0", RaceTableDto())),
            resultsByRound = emptyMap(),
            driverStandings = DriverStandingsResponseDto(
                DriverStandingsMrDataDto("30", "0", "0", DriverStandingsTableDto(season = "2023")),
            ),
            constructorStandings = ConstructorStandingsResponseDto(
                ConstructorStandingsMrDataDto("30", "0", "0", ConstructorStandingsTableDto(season = "2023")),
            ),
            drivers = DriverTableResponseDto(DriverTableMrDataDto("30", "0", "0", DriverTableDto())),
            constructors = ConstructorTableResponseDto(ConstructorTableMrDataDto("30", "0", "0", ConstructorTableDto())),
        )
        val repository = buildRepository(api, MutableClock(Instant.parse("2024-06-01T00:00:00Z")))

        // Act
        repository.syncSeason(2023)

        // Assert
        assertEquals(0, api.seasonScheduleCallCount)
    }

    @Test
    fun `syncSeason refetches a past season that is only partially cached`() = runTest {
        // Arrange
        raceDao.upserted += RaceEntity(
            season = 2023, round = 1, raceName = "Bahrain Grand Prix",
            circuitId = "bahrain", date = "2023-03-05", time = null,
        )
        raceDao.upserted += RaceEntity(
            season = 2023, round = 2, raceName = "Saudi Arabian Grand Prix",
            circuitId = "jeddah", date = "2023-03-19", time = null,
        )
        // Only round 1 has cached results; round 2 is missing, so the season isn't complete yet.
        resultDao.upserted += ResultEntity(
            season = 2023, round = 1, driverId = "max_verstappen", constructorId = "red_bull",
            position = 1, positionText = "1", points = 25.0, grid = 1, laps = 57, status = "Finished",
            finishTimeMillis = null, fastestLapRank = null, fastestLapTime = null,
        )
        val scheduleRace = RaceDto(
            season = "2023", round = "1", raceName = "Bahrain Grand Prix", circuit = circuit, date = "2023-03-05",
        )
        val api = FakeJolpicaApiService(
            schedule = RaceResponseDto(RaceMrDataDto("30", "0", "1", RaceTableDto(races = listOf(scheduleRace)))),
            resultsByRound = mapOf(
                1 to RaceResponseDto(
                    RaceMrDataDto("30", "0", "1", RaceTableDto(races = listOf(scheduleRace))),
                ),
            ),
            driverStandings = DriverStandingsResponseDto(
                DriverStandingsMrDataDto("30", "0", "0", DriverStandingsTableDto(season = "2023")),
            ),
            constructorStandings = ConstructorStandingsResponseDto(
                ConstructorStandingsMrDataDto("30", "0", "0", ConstructorStandingsTableDto(season = "2023")),
            ),
            drivers = DriverTableResponseDto(DriverTableMrDataDto("30", "0", "0", DriverTableDto())),
            constructors = ConstructorTableResponseDto(ConstructorTableMrDataDto("30", "0", "0", ConstructorTableDto())),
        )
        val repository = buildRepository(api, MutableClock(Instant.parse("2024-06-01T00:00:00Z")))

        // Act
        repository.syncSeason(2023)

        // Assert
        assertEquals(1, api.seasonScheduleCallCount)
    }

    @Test
    fun `getResultsForDriverAtCircuit only returns results from races at that circuit`() = runTest {
        // Arrange
        raceDao.upserted += RaceEntity(
            season = 2023,
            round = 1,
            raceName = "Bahrain Grand Prix",
            circuitId = "bahrain",
            date = "2023-03-05",
            time = null,
        )
        raceDao.upserted += RaceEntity(
            season = 2023,
            round = 2,
            raceName = "Saudi Arabian Grand Prix",
            circuitId = "jeddah",
            date = "2023-03-19",
            time = null,
        )
        resultDao.upserted += ResultEntity(
            season = 2023, round = 1, driverId = "max_verstappen", constructorId = "red_bull",
            position = 1, positionText = "1", points = 25.0, grid = 1, laps = 57, status = "Finished",
            finishTimeMillis = null, fastestLapRank = null, fastestLapTime = null,
        )
        resultDao.upserted += ResultEntity(
            season = 2023, round = 2, driverId = "max_verstappen", constructorId = "red_bull",
            position = 1, positionText = "1", points = 25.0, grid = 1, laps = 50, status = "Finished",
            finishTimeMillis = null, fastestLapRank = null, fastestLapTime = null,
        )
        val repository = buildRepository(
            FakeJolpicaApiService(
                schedule = RaceResponseDto(RaceMrDataDto("30", "0", "0", RaceTableDto())),
                resultsByRound = emptyMap(),
                driverStandings = DriverStandingsResponseDto(
                    DriverStandingsMrDataDto("30", "0", "0", DriverStandingsTableDto(season = "2023")),
                ),
                constructorStandings = ConstructorStandingsResponseDto(
                    ConstructorStandingsMrDataDto("30", "0", "0", ConstructorStandingsTableDto(season = "2023")),
                ),
                drivers = DriverTableResponseDto(DriverTableMrDataDto("30", "0", "0", DriverTableDto())),
                constructors = ConstructorTableResponseDto(
                    ConstructorTableMrDataDto("30", "0", "0", ConstructorTableDto()),
                ),
            ),
        )

        // Act
        val results = repository.getResultsForDriverAtCircuit("max_verstappen", "bahrain").first()

        // Assert
        assertEquals(1, results.size)
        assertEquals(1, results.single().round)
    }

    @Test
    fun `syncDriverRoster caches the full driver list for a season`() = runTest {
        // Arrange
        val secondDriver = driver.copy(driverId = "norris", givenName = "Lando", familyName = "Norris")
        val api = FakeJolpicaApiService(
            schedule = RaceResponseDto(RaceMrDataDto("30", "0", "0", RaceTableDto())),
            resultsByRound = emptyMap(),
            driverStandings = DriverStandingsResponseDto(
                DriverStandingsMrDataDto("30", "0", "0", DriverStandingsTableDto(season = "2099")),
            ),
            constructorStandings = ConstructorStandingsResponseDto(
                ConstructorStandingsMrDataDto("30", "0", "0", ConstructorStandingsTableDto(season = "2099")),
            ),
            drivers = DriverTableResponseDto(
                DriverTableMrDataDto("30", "0", "2", DriverTableDto(drivers = listOf(driver, secondDriver))),
            ),
            constructors = ConstructorTableResponseDto(ConstructorTableMrDataDto("30", "0", "0", ConstructorTableDto())),
        )
        val repository = buildRepository(api)

        // Act
        repository.syncDriverRoster(2099)

        // Assert
        assertEquals(2, driverDao.upserted.size)
        assertEquals(setOf("max_verstappen", "norris"), driverDao.upserted.keys)
    }

    @Test
    fun `syncConstructorRoster caches the full constructor list for a season`() = runTest {
        // Arrange
        val secondConstructor = constructor.copy(constructorId = "mclaren", name = "McLaren")
        val api = FakeJolpicaApiService(
            schedule = RaceResponseDto(RaceMrDataDto("30", "0", "0", RaceTableDto())),
            resultsByRound = emptyMap(),
            driverStandings = DriverStandingsResponseDto(
                DriverStandingsMrDataDto("30", "0", "0", DriverStandingsTableDto(season = "2099")),
            ),
            constructorStandings = ConstructorStandingsResponseDto(
                ConstructorStandingsMrDataDto("30", "0", "0", ConstructorStandingsTableDto(season = "2099")),
            ),
            drivers = DriverTableResponseDto(DriverTableMrDataDto("30", "0", "0", DriverTableDto())),
            constructors = ConstructorTableResponseDto(
                ConstructorTableMrDataDto(
                    "30", "0", "2",
                    ConstructorTableDto(constructors = listOf(constructor, secondConstructor)),
                ),
            ),
        )
        val repository = buildRepository(api)

        // Act
        repository.syncConstructorRoster(2099)

        // Assert
        assertEquals(2, constructorDao.upserted.size)
        assertEquals(setOf("red_bull", "mclaren"), constructorDao.upserted.keys)
    }

    @Test
    fun `syncDriverStandings does not refetch within the TTL window`() = runTest {
        // Arrange
        val api = standingsOnlyApi()
        val clock = MutableClock()
        val repository = buildRepository(api, clock)

        // Act
        repository.syncDriverStandings(2099)
        repository.syncDriverStandings(2099)

        // Assert
        assertEquals(1, api.driverStandingsCallCount)
    }

    @Test
    fun `syncDriverStandings refetches once the TTL window has elapsed`() = runTest {
        // Arrange
        val api = standingsOnlyApi()
        val clock = MutableClock()
        val repository = buildRepository(api, clock)
        repository.syncDriverStandings(2099)

        // Act
        clock.advanceBy(Duration.ofMinutes(16))
        repository.syncDriverStandings(2099)

        // Assert
        assertEquals(2, api.driverStandingsCallCount)
    }

    @Test
    fun `syncDriverStandings with forceRefresh always refetches`() = runTest {
        // Arrange
        val api = standingsOnlyApi()
        val repository = buildRepository(api, MutableClock())
        repository.syncDriverStandings(2099)

        // Act
        repository.syncDriverStandings(2099, forceRefresh = true)

        // Assert
        assertEquals(2, api.driverStandingsCallCount)
    }

    private fun standingsOnlyApi() = FakeJolpicaApiService(
        schedule = RaceResponseDto(RaceMrDataDto("30", "0", "0", RaceTableDto())),
        resultsByRound = emptyMap(),
        driverStandings = DriverStandingsResponseDto(
            DriverStandingsMrDataDto(
                "30", "0", "1",
                DriverStandingsTableDto(
                    season = "2099",
                    standingsLists = listOf(
                        DriverStandingsListDto(
                            season = "2099",
                            round = "1",
                            driverStandings = listOf(
                                DriverStandingDto("1", "1", "26", "1", driver, listOf(constructor)),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        constructorStandings = ConstructorStandingsResponseDto(
            ConstructorStandingsMrDataDto("30", "0", "0", ConstructorStandingsTableDto(season = "2099")),
        ),
        drivers = DriverTableResponseDto(DriverTableMrDataDto("30", "0", "0", DriverTableDto())),
        constructors = ConstructorTableResponseDto(ConstructorTableMrDataDto("30", "0", "0", ConstructorTableDto())),
    )

    private fun buildRepository(api: FakeJolpicaApiService, clock: Clock = MutableClock()) = F1RepositoryImpl(
        api = api,
        driverDao = driverDao,
        constructorDao = constructorDao,
        circuitDao = circuitDao,
        raceDao = raceDao,
        resultDao = resultDao,
        driverStandingDao = driverStandingDao,
        constructorStandingDao = constructorStandingDao,
        clock = clock,
    )
}
