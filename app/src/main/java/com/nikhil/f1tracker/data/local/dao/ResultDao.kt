package com.nikhil.f1tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nikhil.f1tracker.data.local.entity.ResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ResultDao {

    @Upsert
    suspend fun upsertAll(results: List<ResultEntity>)

    @Query("SELECT * FROM results WHERE season = :season AND round = :round ORDER BY position")
    fun getByRace(season: Int, round: Int): Flow<List<ResultEntity>>

    @Query("SELECT * FROM results WHERE driverId = :driverId ORDER BY season DESC, round DESC")
    fun getByDriver(driverId: String): Flow<List<ResultEntity>>

    @Query("SELECT * FROM results WHERE driverId = :driverId AND season = :season ORDER BY round")
    fun getByDriverAndSeason(driverId: String, season: Int): Flow<List<ResultEntity>>

    @Query("SELECT * FROM results WHERE constructorId = :constructorId AND season = :season ORDER BY round")
    fun getByConstructorAndSeason(constructorId: String, season: Int): Flow<List<ResultEntity>>

    @Query(
        """
        SELECT results.* FROM results
        INNER JOIN races ON results.season = races.season AND results.round = races.round
        WHERE results.driverId = :driverId AND races.circuitId = :circuitId
        ORDER BY results.season DESC
        """
    )
    fun getByDriverAndCircuit(driverId: String, circuitId: String): Flow<List<ResultEntity>>
}
