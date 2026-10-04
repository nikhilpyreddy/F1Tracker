package com.nikhil.f1tracker.data.local.entity

import androidx.room.Entity

/** A qualifying classification line. Times are as Jolpica gives them, e.g. "1:35.130". */
@Entity(tableName = "qualifying", primaryKeys = ["season", "round", "driverId"])
data class QualifyingEntity(
    val season: Int,
    val round: Int,
    val driverId: String,
    val constructorId: String,
    val position: Int,
    val q1: String?,
    val q2: String?,
    val q3: String?,
)
