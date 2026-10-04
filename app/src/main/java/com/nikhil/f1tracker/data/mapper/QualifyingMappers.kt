package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.local.entity.QualifyingEntity
import com.nikhil.f1tracker.data.remote.dto.QualifyingResultDto

fun QualifyingResultDto.toEntity(season: Int, round: Int): QualifyingEntity? {
    val position = position.toIntOrNull() ?: return null
    return QualifyingEntity(season, round, driver.driverId, constructor.constructorId, position, q1, q2, q3)
}
