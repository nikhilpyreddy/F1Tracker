package com.nikhil.f1tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nikhil.f1tracker.data.local.entity.DriverIdentityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverIdentityDao {

    @Upsert
    suspend fun upsertAll(identities: List<DriverIdentityEntity>)

    @Query("SELECT * FROM driver_identities")
    fun getAll(): Flow<List<DriverIdentityEntity>>
}
