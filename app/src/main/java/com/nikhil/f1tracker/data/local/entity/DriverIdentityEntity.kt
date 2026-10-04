package com.nikhil.f1tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A current-grid driver's look from OpenF1, keyed by the 3-letter code shared with Jolpica. */
@Entity(tableName = "driver_identities")
data class DriverIdentityEntity(
    @PrimaryKey val code: String,
    val lastName: String?,
    val teamName: String?,
    /** Hex RGB without '#'. */
    val teamColour: String?,
    val headshotUrl: String?,
)
