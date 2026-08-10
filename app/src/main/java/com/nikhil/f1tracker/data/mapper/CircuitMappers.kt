package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.local.entity.CircuitEntity
import com.nikhil.f1tracker.data.remote.dto.CircuitDto

fun CircuitDto.toEntity(): CircuitEntity = CircuitEntity(
    circuitId = circuitId,
    circuitName = circuitName,
    locality = location.locality,
    country = location.country,
    latitude = location.lat.toDoubleOrNull() ?: 0.0,
    longitude = location.long.toDoubleOrNull() ?: 0.0,
)
