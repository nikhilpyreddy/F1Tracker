package com.nikhil.f1tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nikhil.f1tracker.data.local.entity.DriverStandingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DriverStandingDao {

    @Upsert
    suspend fun upsertAll(standings: List<DriverStandingEntity>)

    @Query("SELECT * FROM driver_standings WHERE season = :season ORDER BY position")
    fun getBySeason(season: Int): Flow<List<DriverStandingEntity>>

    @Query(
        "SELECT * FROM driver_standings WHERE driverId = :driverId AND season IN (:seasons) ORDER BY season",
    )
    fun getByDriverAndSeasons(driverId: String, seasons: List<Int>): Flow<List<DriverStandingEntity>>
}
