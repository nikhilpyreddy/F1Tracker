package com.nikhil.f1tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConstructorDto(
    val constructorId: String,
    val url: String? = null,
    val name: String,
    val nationality: String,
)

@Serializable
data class ConstructorTableResponseDto(
    @SerialName("MRData") val mrData: ConstructorTableMrDataDto,
)

@Serializable
data class ConstructorTableMrDataDto(
    val limit: String,
    val offset: String,
    val total: String,
    @SerialName("ConstructorTable") val constructorTable: ConstructorTableDto,
)

@Serializable
data class ConstructorTableDto(
    val season: String? = null,
    @SerialName("Constructors") val constructors: List<ConstructorDto> = emptyList(),
)
