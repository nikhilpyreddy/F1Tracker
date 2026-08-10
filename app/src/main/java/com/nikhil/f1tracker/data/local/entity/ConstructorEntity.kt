package com.nikhil.f1tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "constructors")
data class ConstructorEntity(
    @PrimaryKey val constructorId: String,
    val name: String,
    val nationality: String,
)
