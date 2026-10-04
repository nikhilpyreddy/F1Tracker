package com.nikhil.f1tracker.data.repository.fakes

import com.nikhil.f1tracker.data.remote.JolpicaApiService
import com.nikhil.f1tracker.data.remote.dto.ConstructorStandingsResponseDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorTableResponseDto
import com.nikhil.f1tracker.data.remote.dto.DriverStandingsResponseDto
import com.nikhil.f1tracker.data.remote.dto.DriverTableResponseDto
import com.nikhil.f1tracker.data.remote.dto.RaceMrDataDto
import com.nikhil.f1tracker.data.remote.dto.RaceResponseDto
import com.nikhil.f1tracker.data.remote.dto.RaceTableDto
import com.nikhil.f1tracker.data.remote.dto.SeasonDto
import com.nikhil.f1tracker.data.remote.dto.SeasonTableDto
import com.nikhil.f1tracker.data.remote.dto.SeasonTableMrDataDto
import com.nikhil.f1tracker.data.remote.dto.SeasonTableResponseDto

class FakeJolpicaApiService(
    private val schedule: RaceResponseDto,
    /** Season results pages, keyed by request offset. */
    private val seasonResultPages: Map<Int, RaceResponseDto>,
    private val driverStandings: DriverStandingsResponseDto,
    private val constructorStandings: ConstructorStandingsResponseDto,
    private val drivers: DriverTableResponseDto,
    private val constructors: ConstructorTableResponseDto,
    private val circuitSeasons: Map<String, List<Int>> = emptyMap(),
    /** Results at a circuit, keyed by (season, circuitId). */
    private val circuitResults: Map<Pair<Int, String>, RaceResponseDto> = emptyMap(),
) : JolpicaApiService {

    val seasonResultOffsetsRequested = mutableListOf<Int>()
    val circuitResultSeasonsRequested = mutableListOf<Int>()

    var seasonScheduleCallCount = 0
        private set
    var driverStandingsCallCount = 0
        private set
    var constructorStandingsCallCount = 0
        private set

    override suspend fun getSeasonSchedule(season: Int, limit: Int): RaceResponseDto {
        seasonScheduleCallCount++
        return schedule
    }

    override suspend fun getSeasonResults(season: Int, offset: Int, limit: Int): RaceResponseDto {
        seasonResultOffsetsRequested += offset
        return seasonResultPages[offset] ?: EMPTY_RACES
    }

    override suspend fun getSeasonQualifying(season: Int, offset: Int, limit: Int): RaceResponseDto = EMPTY_RACES

    override suspend fun getCircuitQualifying(season: Int, circuitId: String, limit: Int): RaceResponseDto = EMPTY_RACES

    override suspend fun getCircuitSeasons(circuitId: String, limit: Int): SeasonTableResponseDto {
        val seasons = circuitSeasons[circuitId].orEmpty().map { SeasonDto(it.toString()) }
        return SeasonTableResponseDto(
            SeasonTableMrDataDto("100", "0", seasons.size.toString(), SeasonTableDto(circuitId, seasons)),
        )
    }

    override suspend fun getCircuitResults(season: Int, circuitId: String, limit: Int): RaceResponseDto {
        circuitResultSeasonsRequested += season
        return circuitResults[season to circuitId] ?: EMPTY_RACES
    }

    override suspend fun getDrivers(season: Int, limit: Int): DriverTableResponseDto = drivers

    override suspend fun getConstructors(season: Int, limit: Int): ConstructorTableResponseDto = constructors

    override suspend fun getDriverStandings(season: Int, limit: Int): DriverStandingsResponseDto {
        driverStandingsCallCount++
        return driverStandings
    }

    override suspend fun getConstructorStandings(season: Int, limit: Int): ConstructorStandingsResponseDto {
        constructorStandingsCallCount++
        return constructorStandings
    }

    private companion object {
        val EMPTY_RACES = RaceResponseDto(RaceMrDataDto("100", "0", "0", RaceTableDto()))
    }
}
