package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CollectionCheckpoint
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckpointDao {

    @Query("SELECT * FROM collection_checkpoint WHERE id = 1 LIMIT 1")
    fun getCheckpoint(): Flow<CollectionCheckpoint?>

    @Query("SELECT * FROM collection_checkpoint WHERE id = 1 LIMIT 1")
    suspend fun getCurrentCheckpoint(): CollectionCheckpoint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCheckpoint(checkpoint: CollectionCheckpoint)

    @Query("UPDATE collection_checkpoint SET connectionStatus = :status WHERE id = 1")
    suspend fun updateConnectionStatus(status: String)

    @Query("""
        UPDATE collection_checkpoint 
        SET savedStartPointRoundId = :roundId, 
            savedStartPointMatchday = :matchday, 
            savedStartPointSeasonId = :seasonId, 
            savedStartPointUtc = :savedUtc 
        WHERE id = 1
    """)
    suspend fun updateStartPoint(roundId: String, matchday: Int, seasonId: String, savedUtc: String)
}
