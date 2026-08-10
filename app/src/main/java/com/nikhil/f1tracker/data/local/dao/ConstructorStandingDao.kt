package com.nikhil.f1tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nikhil.f1tracker.data.local.entity.ConstructorStandingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConstructorStandingDao {

    @Upsert
    suspend fun upsertAll(standings: List<ConstructorStandingEntity>)

    @Query("SELECT * FROM constructor_standings WHERE season = :season ORDER BY position")
    fun getBySeason(season: Int): Flow<List<ConstructorStandingEntity>>

    @Query(
        "SELECT * FROM constructor_standings WHERE constructorId = :constructorId AND season IN (:seasons) ORDER BY season",
    )
    fun getByConstructorAndSeasons(
        constructorId: String,
        seasons: List<Int>,
    ): Flow<List<ConstructorStandingEntity>>
}
