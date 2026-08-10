package com.nikhil.f1tracker.data.local.entity

import androidx.room.Entity

@Entity(tableName = "results", primaryKeys = ["season", "round", "driverId"])
data class ResultEntity(
    val season: Int,
    val round: Int,
    val driverId: String,
    val constructorId: String,
    val position: Int?,
    val positionText: String,
    val points: Double,
    val grid: Int,
    val laps: Int,
    val status: String,
    val finishTimeMillis: Long?,
    val fastestLapRank: Int?,
    val fastestLapTime: String?,
)
