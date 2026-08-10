package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.remote.dto.DriverDto

fun DriverDto.toEntity(): DriverEntity = DriverEntity(
    driverId = driverId,
    permanentNumber = permanentNumber?.toIntOrNull(),
    code = code,
    givenName = givenName,
    familyName = familyName,
    dateOfBirth = dateOfBirth,
    nationality = nationality,
)
