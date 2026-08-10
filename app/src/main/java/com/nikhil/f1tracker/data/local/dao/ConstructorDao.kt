package com.nikhil.f1tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConstructorDao {

    @Upsert
    suspend fun upsertAll(constructors: List<ConstructorEntity>)

    @Query("SELECT * FROM constructors ORDER BY name")
    fun getAll(): Flow<List<ConstructorEntity>>

    @Query("SELECT * FROM constructors WHERE constructorId = :constructorId")
    fun getById(constructorId: String): Flow<ConstructorEntity?>

    @Query("SELECT * FROM constructors WHERE constructorId IN (:constructorIds)")
    fun getByIds(constructorIds: List<String>): Flow<List<ConstructorEntity>>
}
