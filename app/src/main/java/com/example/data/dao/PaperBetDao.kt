package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PaperBetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperBetDao {

    @Query("SELECT * FROM paper_bets ORDER BY id DESC")
    fun getAllBets(): Flow<List<PaperBetEntity>>

    @Query("SELECT * FROM paper_bets WHERE modelId = :modelId ORDER BY id DESC")
    fun getBetsForModel(modelId: String): Flow<List<PaperBetEntity>>

    @Query("SELECT * FROM paper_bets WHERE status = 'PENDING'")
    suspend fun getPendingBets(): List<PaperBetEntity>

    @Query("SELECT * FROM paper_bets WHERE fixtureId = :fixtureId AND status = 'PENDING'")
    suspend fun getPendingBetsForFixture(fixtureId: String): List<PaperBetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBet(bet: PaperBetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bets: List<PaperBetEntity>)

    @Update
    suspend fun updateBet(bet: PaperBetEntity)

    @Query("DELETE FROM paper_bets")
    suspend fun deleteAll()
}
