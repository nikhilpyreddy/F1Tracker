package com.nikhil.f1tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverDao {

    @Upsert
    suspend fun upsertAll(drivers: List<DriverEntity>)

    @Query("SELECT * FROM drivers ORDER BY familyName")
    fun getAll(): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers WHERE driverId = :driverId")
    fun getById(driverId: String): Flow<DriverEntity?>

    @Query("SELECT * FROM drivers WHERE driverId IN (:driverIds)")
    fun getByIds(driverIds: List<String>): Flow<List<DriverEntity>>
}
