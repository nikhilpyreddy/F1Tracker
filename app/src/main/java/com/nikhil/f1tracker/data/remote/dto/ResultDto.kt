package com.nikhil.f1tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResultDto(
    val number: String? = null,
    val position: String,
    val positionText: String,
    val points: String,
    @SerialName("Driver") val driver: DriverDto,
    @SerialName("Constructor") val constructor: ConstructorDto,
    val grid: String,
    val laps: String,
    val status: String,
    @SerialName("Time") val time: ResultTimeDto? = null,
    @SerialName("FastestLap") val fastestLap: FastestLapDto? = null,
)

@Serializable
data class ResultTimeDto(
    val millis: String? = null,
    val time: String,
)

@Serializable
data class FastestLapDto(
    val rank: String? = null,
    val lap: String? = null,
    @SerialName("Time") val time: FastestLapTimeDto? = null,
    @SerialName("AverageSpeed") val averageSpeed: AverageSpeedDto? = null,
)

@Serializable
data class FastestLapTimeDto(
    val time: String,
)

@Serializable
data class AverageSpeedDto(
    val units: String,
    val speed: String,
)
