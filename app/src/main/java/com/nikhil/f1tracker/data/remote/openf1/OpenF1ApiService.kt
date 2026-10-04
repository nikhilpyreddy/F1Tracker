package com.nikhil.f1tracker.data.remote.openf1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** OpenF1 (https://openf1.org): free, no key, 2023+ data. Personal/non-commercial use only. */
interface OpenF1ApiService {

    @GET("drivers")
    suspend fun getDrivers(@Query("session_key") sessionKey: String = LATEST_SESSION): List<OpenF1DriverDto>

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
