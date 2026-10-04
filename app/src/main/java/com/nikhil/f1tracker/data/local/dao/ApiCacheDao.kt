package com.nikhil.f1tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nikhil.f1tracker.data.local.entity.ApiCacheEntity

@Dao
interface ApiCacheDao {

    @Query("SELECT * FROM api_cache WHERE `key` = :key")
    suspend fun get(key: String): ApiCacheEntity?

    @Upsert
    suspend fun put(entry: ApiCacheEntity)
}
