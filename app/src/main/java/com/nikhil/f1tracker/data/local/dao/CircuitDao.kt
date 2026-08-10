package com.nikhil.f1tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nikhil.f1tracker.data.local.entity.CircuitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CircuitDao {

    @Upsert
    suspend fun upsertAll(circuits: List<CircuitEntity>)

    @Query("SELECT * FROM circuits ORDER BY circuitName")
    fun getAll(): Flow<List<CircuitEntity>>

    @Query("SELECT * FROM circuits WHERE circuitId = :circuitId")
    fun getById(circuitId: String): Flow<CircuitEntity?>
}
