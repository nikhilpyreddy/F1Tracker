package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.local.entity.ConstructorStandingEntity
import com.nikhil.f1tracker.data.local.entity.DriverStandingEntity
import com.nikhil.f1tracker.data.remote.dto.ConstructorStandingDto
import com.nikhil.f1tracker.data.remote.dto.DriverStandingDto

fun DriverStandingDto.toEntity(season: Int): DriverStandingEntity = DriverStandingEntity(
    season = season,
    driverId = driver.driverId,
    constructorId = constructors.firstOrNull()?.constructorId,
    position = position.toIntOrNull() ?: 0,
    points = points.toDoubleOrNull() ?: 0.0,
    wins = wins.toIntOrNull() ?: 0,
)

fun ConstructorStandingDto.toEntity(season: Int): ConstructorStandingEntity = ConstructorStandingEntity(
    season = season,
    constructorId = constructor.constructorId,
    position = position.toIntOrNull() ?: 0,
    points = points.toDoubleOrNull() ?: 0.0,
    wins = wins.toIntOrNull() ?: 0,
)
