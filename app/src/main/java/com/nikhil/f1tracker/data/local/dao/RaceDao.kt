package com.nikhil.f1tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceDao {

    @Upsert
    suspend fun upsertAll(races: List<RaceEntity>)

    @Query("SELECT * FROM races WHERE season = :season ORDER BY round")
    fun getBySeason(season: Int): Flow<List<RaceEntity>>

    @Query("SELECT * FROM races WHERE season = :season AND round = :round")
    fun getBySeasonAndRound(season: Int, round: Int): Flow<RaceEntity?>

    @Query("SELECT * FROM races WHERE circuitId = :circuitId ORDER BY season DESC")
    fun getByCircuit(circuitId: String): Flow<List<RaceEntity>>
}
