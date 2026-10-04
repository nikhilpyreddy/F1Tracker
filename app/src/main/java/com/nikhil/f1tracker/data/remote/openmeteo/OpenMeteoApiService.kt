package com.nikhil.f1tracker.data.remote.openmeteo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/** Open-Meteo (https://open-meteo.com): free, no key, non-commercial. Forecasts reach 16 days out. */
interface OpenMeteoApiService {

    @GET("forecast")
    suspend fun getDailyForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String = "precipitation_probability_max,temperature_2m_max,wind_speed_10m_max",
        @Query("timezone") timezone: String = "auto",
    ): OpenMeteoForecastDto

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/v1/"
        const val FORECAST_DAYS = 16L
    }
}

@Serializable
data class OpenMeteoForecastDto(val daily: OpenMeteoDailyDto? = null)

@Serializable
data class OpenMeteoDailyDto(
    val time: List<String> = emptyList(),
    @SerialName("precipitation_probability_max") val rainChance: List<Int?> = emptyList(),
    @SerialName("temperature_2m_max") val maxTemperature: List<Double?> = emptyList(),
    @SerialName("wind_speed_10m_max") val maxWind: List<Double?> = emptyList(),
)
