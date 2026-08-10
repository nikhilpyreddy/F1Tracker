package com.nikhil.f1tracker.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CircuitDto(
    val circuitId: String,
    val url: String? = null,
    val circuitName: String,
    @SerialName("Location") val location: LocationDto,
)

@Serializable
data class LocationDto(
    val lat: String,
    val long: String,
    val locality: String,
    val country: String,
)

@Serializable
data class SessionDto(
    val date: String,
    val time: String? = null,
)
