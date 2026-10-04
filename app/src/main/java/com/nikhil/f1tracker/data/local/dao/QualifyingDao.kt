package com.nikhil.f1tracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nikhil.f1tracker.data.local.entity.QualifyingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QualifyingDao {

    @Upsert
    suspend fun upsertAll(rows: List<QualifyingEntity>)

    @Query("SELECT * FROM qualifying WHERE season = :season ORDER BY round, position")
    fun getBySeason(season: Int): Flow<List<QualifyingEntity>>

    @Query(
        """
        SELECT qualifying.* FROM qualifying
        INNER JOIN races ON qualifying.season = races.season AND qualifying.round = races.round
        WHERE races.circuitId = :circuitId
        ORDER BY qualifying.season DESC, qualifying.position
        """
    )
    fun getByCircuit(circuitId: String): Flow<List<QualifyingEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM qualifying
        INNER JOIN races ON qualifying.season = races.season AND qualifying.round = races.round
        WHERE races.season = :season AND races.circuitId = :circuitId
        """
    )
    suspend fun countAtCircuitInSeason(season: Int, circuitId: String): Int
}
