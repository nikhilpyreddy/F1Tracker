package com.nikhil.f1tracker.data.local.entity

import androidx.room.Entity

@Entity(tableName = "driver_standings", primaryKeys = ["season", "driverId"])
data class DriverStandingEntity(
    val season: Int,
    val driverId: String,
    val constructorId: String?,
    val position: Int,
    val points: Double,
    val wins: Int,
)
