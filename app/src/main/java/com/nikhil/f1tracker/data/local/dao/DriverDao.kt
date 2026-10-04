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

    @Query(
        """
        SELECT * FROM drivers WHERE driverId IN (
            SELECT driverId FROM driver_standings WHERE season = :season
            UNION SELECT driverId FROM results WHERE season = :season
        )
        ORDER BY familyName
        """
    )
    fun getBySeason(season: Int): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers WHERE driverId = :driverId")
    fun getById(driverId: String): Flow<DriverEntity?>

    @Query("SELECT * FROM drivers WHERE driverId IN (:driverIds)")
    fun getByIds(driverIds: List<String>): Flow<List<DriverEntity>>
}
