package com.nikhil.f1tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey val driverId: String,
    val permanentNumber: Int?,
    val code: String?,
    val givenName: String,
    val familyName: String,
    val dateOfBirth: String?,
    val nationality: String?,
)
