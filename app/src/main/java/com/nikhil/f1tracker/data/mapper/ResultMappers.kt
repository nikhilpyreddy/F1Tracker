package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.local.entity.ResultEntity
import com.nikhil.f1tracker.data.remote.dto.ResultDto

fun ResultDto.toEntity(season: Int, round: Int): ResultEntity = ResultEntity(
    season = season,
    round = round,
    driverId = driver.driverId,
    constructorId = constructor.constructorId,
    position = position.toIntOrNull(),
    positionText = positionText,
    points = points.toDoubleOrNull() ?: 0.0,
    grid = grid.toIntOrNull() ?: 0,
    laps = laps.toIntOrNull() ?: 0,
    status = status,
    finishTimeMillis = time?.millis?.toLongOrNull(),
    fastestLapRank = fastestLap?.rank?.toIntOrNull(),
    fastestLapTime = fastestLap?.time?.time,
)
