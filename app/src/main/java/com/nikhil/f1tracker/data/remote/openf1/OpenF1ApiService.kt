package com.nikhil.f1tracker.data.remote.openf1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * OpenF1 (https://openf1.org): free, no key, 2023+ data. Personal/non-commercial use only.
 * Time-window filters use OpenF1's `date>=`/`date<=` syntax, i.e. query names `date>` and `date<`.
 */
interface OpenF1ApiService {

    @GET("drivers")
    suspend fun getDrivers(@Query("session_key") sessionKey: String = LATEST_SESSION): List<OpenF1DriverDto>

    @GET("sessions")
    suspend fun getSessionsOnDay(
        @Query("date_start>") from: String,
        @Query("date_start<") until: String,
    ): List<OpenF1SessionDto>

    @GET("sessions")
    suspend fun getSessionsAtCircuit(@Query("circuit_key") circuitKey: Int): List<OpenF1SessionDto>

    @GET("session_result")
    suspend fun getSessionResult(
        @Query("session_key") sessionKey: Int,
        @Query("position") position: Int? = null,
    ): List<OpenF1SessionResultDto>

    @GET("laps")
    suspend fun getLaps(
        @Query("session_key") sessionKey: Int,
        @Query("driver_number") driverNumber: Int,
    ): List<OpenF1LapDto>

    @GET("car_data")
    suspend fun getCarData(
        @Query("session_key") sessionKey: Int,
        @Query("driver_number") driverNumber: Int,
        @Query("date>") from: String,
        @Query("date<") until: String,
    ): List<OpenF1CarDataDto>

    @GET("location")
    suspend fun getLocation(
        @Query("session_key") sessionKey: Int,
        @Query("driver_number") driverNumber: Int,
        @Query("date>") from: String,
        @Query("date<") until: String,
    ): List<OpenF1LocationDto>

    @GET("stints")
    suspend fun getStints(@Query("session_key") sessionKey: Int): List<OpenF1StintDto>

    @GET("drivers")
    suspend fun getSessionDrivers(@Query("session_key") sessionKey: Int): List<OpenF1DriverDto>

    @GET("race_control")
    suspend fun getRaceControl(@Query("session_key") sessionKey: Int): List<OpenF1RaceControlDto>

    @GET("overtakes")
    suspend fun getOvertakes(@Query("session_key") sessionKey: Int): List<OpenF1OvertakeDto>

    @GET("pit")
    suspend fun getPitStops(@Query("session_key") sessionKey: Int): List<OpenF1PitDto>

    companion object {
        const val BASE_URL = "https://api.openf1.org/v1/"
        const val LATEST_SESSION = "latest"
    }
}

@Serializable
data class OpenF1DriverDto(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("name_acronym") val nameAcronym: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    @SerialName("team_name") val teamName: String? = null,
    /** Hex RGB without '#', e.g. "F47600". */
    @SerialName("team_colour") val teamColour: String? = null,
    @SerialName("headshot_url") val headshotUrl: String? = null,
)

@Serializable
data class OpenF1SessionDto(
    @SerialName("session_key") val sessionKey: Int,
    @SerialName("session_name") val sessionName: String,
    @SerialName("date_start") val dateStart: String,
    @SerialName("date_end") val dateEnd: String? = null,
    @SerialName("circuit_key") val circuitKey: Int,
    val year: Int,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
)

@Serializable
data class OpenF1SessionResultDto(
    @SerialName("driver_number") val driverNumber: Int,
    val position: Int? = null,
)

@Serializable
data class OpenF1LapDto(
    @SerialName("lap_number") val lapNumber: Int,
    @SerialName("lap_duration") val lapDuration: Double? = null,
    @SerialName("date_start") val dateStart: String? = null,
    @SerialName("st_speed") val speedTrap: Int? = null,
)

@Serializable
data class OpenF1CarDataDto(
    val date: String,
    val speed: Int,
    val throttle: Int,
    val brake: Int,
    @SerialName("n_gear") val gear: Int,
)

@Serializable
data class OpenF1LocationDto(
    val date: String,
    val x: Int,
    val y: Int,
)

@Serializable
data class OpenF1StintDto(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("stint_number") val stintNumber: Int,
    @SerialName("lap_start") val lapStart: Int? = null,
    @SerialName("lap_end") val lapEnd: Int? = null,
    val compound: String? = null,
    @SerialName("tyre_age_at_start") val tyreAgeAtStart: Int? = null,
)

@Serializable
data class OpenF1RaceControlDto(
    val category: String,
    val flag: String? = null,
    val message: String? = null,
    @SerialName("lap_number") val lapNumber: Int? = null,
)

@Serializable
data class OpenF1OvertakeDto(
    @SerialName("overtaking_driver_number") val overtakingDriverNumber: Int,
)

@Serializable
data class OpenF1PitDto(
    @SerialName("driver_number") val driverNumber: Int,
    @SerialName("lap_number") val lapNumber: Int? = null,
    @SerialName("lane_duration") val laneDuration: Double? = null,
    @SerialName("stop_duration") val stopDuration: Double? = null,
)
