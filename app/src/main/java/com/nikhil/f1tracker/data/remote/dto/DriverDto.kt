package com.nikhil.f1tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DriverDto(
    val driverId: String,
    val permanentNumber: String? = null,
    val code: String? = null,
    val url: String? = null,
    val givenName: String,
    val familyName: String,
    // Reserve/test drivers on the season roster endpoint often lack bio data entirely.
    val dateOfBirth: String? = null,
    val nationality: String? = null,
)

@Serializable
data class DriverTableResponseDto(
    @SerialName("MRData") val mrData: DriverTableMrDataDto,
)

@Serializable
data class DriverTableMrDataDto(
    val limit: String,
    val offset: String,
    val total: String,
    @SerialName("DriverTable") val driverTable: DriverTableDto,
)

@Serializable
data class DriverTableDto(
    val season: String? = null,
    @SerialName("Drivers") val drivers: List<DriverDto> = emptyList(),
)
