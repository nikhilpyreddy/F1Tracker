package com.nikhil.f1tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DriverStandingDto(
    val position: String,
    val positionText: String,
    val points: String,
    val wins: String,
    @SerialName("Driver") val driver: DriverDto,
    @SerialName("Constructors") val constructors: List<ConstructorDto> = emptyList(),
)

@Serializable
data class DriverStandingsResponseDto(
    @SerialName("MRData") val mrData: DriverStandingsMrDataDto,
)

@Serializable
data class DriverStandingsMrDataDto(
    val limit: String,
    val offset: String,
    val total: String,
    @SerialName("StandingsTable") val standingsTable: DriverStandingsTableDto,
)

@Serializable
data class DriverStandingsTableDto(
    val season: String,
    @SerialName("StandingsLists") val standingsLists: List<DriverStandingsListDto> = emptyList(),
)

@Serializable
data class DriverStandingsListDto(
    val season: String,
    val round: String,
    @SerialName("DriverStandings") val driverStandings: List<DriverStandingDto> = emptyList(),
)
