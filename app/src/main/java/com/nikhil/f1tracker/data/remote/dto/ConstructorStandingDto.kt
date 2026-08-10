package com.nikhil.f1tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConstructorStandingDto(
    val position: String,
    val positionText: String,
    val points: String,
    val wins: String,
    @SerialName("Constructor") val constructor: ConstructorDto,
)

@Serializable
data class ConstructorStandingsResponseDto(
    @SerialName("MRData") val mrData: ConstructorStandingsMrDataDto,
)

@Serializable
data class ConstructorStandingsMrDataDto(
    val limit: String,
    val offset: String,
    val total: String,
    @SerialName("StandingsTable") val standingsTable: ConstructorStandingsTableDto,
)

@Serializable
data class ConstructorStandingsTableDto(
    val season: String,
    @SerialName("StandingsLists") val standingsLists: List<ConstructorStandingsListDto> = emptyList(),
)

@Serializable
data class ConstructorStandingsListDto(
    val season: String,
    val round: String,
    @SerialName("ConstructorStandings") val constructorStandings: List<ConstructorStandingDto> = emptyList(),
)
