package com.nikhil.f1tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeasonDto(
    val season: String,
    val url: String? = null,
)

@Serializable
data class SeasonTableResponseDto(
    @SerialName("MRData") val mrData: SeasonTableMrDataDto,
)

@Serializable
data class SeasonTableMrDataDto(
    val limit: String,
    val offset: String,
    val total: String,
    @SerialName("SeasonTable") val seasonTable: SeasonTableDto,
)

@Serializable
data class SeasonTableDto(
    val circuitId: String? = null,
    @SerialName("Seasons") val seasons: List<SeasonDto> = emptyList(),
)
