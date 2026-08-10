package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.remote.dto.RaceDto

fun RaceDto.toEntity(): RaceEntity = RaceEntity(
    season = season.toInt(),
    round = round.toInt(),
    raceName = raceName,
    circuitId = circuit.circuitId,
    date = date,
    time = time,
)
