package com.nikhil.f1tracker.data.local.entity

import androidx.room.Entity

@Entity(tableName = "races", primaryKeys = ["season", "round"])
data class RaceEntity(
    val season: Int,
    val round: Int,
    val raceName: String,
    val circuitId: String,
    /** UTC date and time, as Jolpica gives them ("2026-10-11", "12:00:00Z"). */
    val date: String,
    val time: String?,
    val qualifyingDate: String? = null,
    val qualifyingTime: String? = null,
    /** Set on sprint weekends only. */
    val sprintDate: String? = null,
    val sprintTime: String? = null,
)
