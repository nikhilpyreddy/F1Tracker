package com.nikhil.f1tracker.data.repository.fakes

import com.nikhil.f1tracker.data.remote.JolpicaApiService
import com.nikhil.f1tracker.data.remote.dto.ConstructorStandingsResponseDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorTableResponseDto
import com.nikhil.f1tracker.data.remote.dto.DriverStandingsResponseDto
import com.nikhil.f1tracker.data.remote.dto.DriverTableResponseDto
import com.nikhil.f1tracker.data.remote.dto.RaceResponseDto

class FakeJolpicaApiService(
    private val schedule: RaceResponseDto,
    private val resultsByRound: Map<Int, RaceResponseDto>,
    private val driverStandings: DriverStandingsResponseDto,
    private val constructorStandings: ConstructorStandingsResponseDto,
    private val drivers: DriverTableResponseDto,
    private val constructors: ConstructorTableResponseDto,
) : JolpicaApiService {

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

    override suspend fun getRaceResults(season: Int, round: Int): RaceResponseDto =
        resultsByRound.getValue(round)

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
}
