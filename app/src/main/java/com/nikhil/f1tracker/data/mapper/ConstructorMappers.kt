package com.nikhil.f1tracker.data.mapper

import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.remote.dto.ConstructorDto

fun ConstructorDto.toEntity(): ConstructorEntity = ConstructorEntity(
    constructorId = constructorId,
    name = name,
    nationality = nationality,
)
