package com.nikhil.f1tracker.data.local.entity

import androidx.room.Entity

@Entity(tableName = "constructor_standings", primaryKeys = ["season", "constructorId"])
data class ConstructorStandingEntity(
    val season: Int,
    val constructorId: String,
    val position: Int,
    val points: Double,
    val wins: Int,
)
