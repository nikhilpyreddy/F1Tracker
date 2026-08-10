package com.nikhil.f1tracker.data.local.entity

import androidx.room.Entity

@Entity(tableName = "races", primaryKeys = ["season", "round"])
data class RaceEntity(
    val season: Int,
    val round: Int,
    val raceName: String,
    val circuitId: String,
    val date: String,
    val time: String?,
)
