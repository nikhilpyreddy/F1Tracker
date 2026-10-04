package com.nikhil.f1tracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A raw JSON API response, for data that never changes once a session is over. */
@Entity(tableName = "api_cache")
data class ApiCacheEntity(
    @PrimaryKey val key: String,
    val body: String,
    val fetchedAtMillis: Long,
)
