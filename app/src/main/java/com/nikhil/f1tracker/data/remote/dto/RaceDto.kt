package com.nikhil.f1tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RaceDto(
    val season: String,
    val round: String,
    val url: String? = null,
    val raceName: String,
    @SerialName("Circuit") val circuit: CircuitDto,
    val date: String,
    val time: String? = null,
    @SerialName("Results") val results: List<ResultDto> = emptyList(),
    @SerialName("FirstPractice") val firstPractice: SessionDto? = null,
    @SerialName("SecondPractice") val secondPractice: SessionDto? = null,
    @SerialName("ThirdPractice") val thirdPractice: SessionDto? = null,
    @SerialName("Qualifying") val qualifying: SessionDto? = null,
    @SerialName("Sprint") val sprint: SessionDto? = null,
)

@Serializable
data class RaceResponseDto(
    @SerialName("MRData") val mrData: RaceMrDataDto,
)

@Serializable
data class RaceMrDataDto(
    val limit: String,
    val offset: String,
    val total: String,
    @SerialName("RaceTable") val raceTable: RaceTableDto,
)

@Serializable
data class RaceTableDto(
    val season: String? = null,
    val round: String? = null,
    @SerialName("Races") val races: List<RaceDto> = emptyList(),
)
