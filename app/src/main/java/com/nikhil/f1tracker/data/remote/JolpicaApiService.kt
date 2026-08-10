package com.nikhil.f1tracker.data.remote

import com.nikhil.f1tracker.data.remote.dto.ConstructorStandingsResponseDto
import com.nikhil.f1tracker.data.remote.dto.ConstructorTableResponseDto
import com.nikhil.f1tracker.data.remote.dto.DriverStandingsResponseDto
import com.nikhil.f1tracker.data.remote.dto.DriverTableResponseDto
import com.nikhil.f1tracker.data.remote.dto.RaceResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

private const val STANDINGS_PAGE_LIMIT = 100

interface JolpicaApiService {

    @GET("{season}.json")
    suspend fun getSeasonSchedule(
        @Path("season") season: Int,
        @Query("limit") limit: Int = STANDINGS_PAGE_LIMIT,
    ): RaceResponseDto

    @GET("{season}/{round}/results.json")
    suspend fun getRaceResults(
        @Path("season") season: Int,
        @Path("round") round: Int,
    ): RaceResponseDto

    @GET("{season}/drivers.json")
    suspend fun getDrivers(
        @Path("season") season: Int,
        @Query("limit") limit: Int = STANDINGS_PAGE_LIMIT,
    ): DriverTableResponseDto

    @GET("{season}/constructors.json")
    suspend fun getConstructors(
        @Path("season") season: Int,
        @Query("limit") limit: Int = STANDINGS_PAGE_LIMIT,
    ): ConstructorTableResponseDto

    @GET("{season}/driverStandings.json")
    suspend fun getDriverStandings(
        @Path("season") season: Int,
        @Query("limit") limit: Int = STANDINGS_PAGE_LIMIT,
    ): DriverStandingsResponseDto

    @GET("{season}/constructorStandings.json")
    suspend fun getConstructorStandings(
        @Path("season") season: Int,
        @Query("limit") limit: Int = STANDINGS_PAGE_LIMIT,
    ): ConstructorStandingsResponseDto

    companion object {
        const val BASE_URL = "https://api.jolpi.ca/ergast/f1/"
    }
}
