package com.nikhil.f1tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "circuits")
data class CircuitEntity(
    @PrimaryKey val circuitId: String,
    val circuitName: String,
    val locality: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
)
