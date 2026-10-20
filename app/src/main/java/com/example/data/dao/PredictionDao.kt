package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PredictionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionDao {

    @Query("SELECT * FROM predictions ORDER BY id DESC")
    fun getAllPredictions(): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM predictions WHERE fixtureId = :fixtureId")
    fun getPredictionsForFixture(fixtureId: String): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM predictions WHERE modelId = :modelId ORDER BY id DESC")
    fun getPredictionsForModel(modelId: String): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM predictions WHERE roundId = :roundId")
    suspend fun getPredictionsByRound(roundId: String): List<PredictionEntity>

    @Query("SELECT * FROM predictions WHERE fixtureId = :fixtureId AND isCorrect IS NULL")
    suspend fun getPendingPredictionsForFixture(fixtureId: String): List<PredictionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(predictions: List<PredictionEntity>)

    @Query("""
        UPDATE predictions 
        SET isCorrect = :isCorrect, settledUtc = :settledUtc 
        WHERE fixtureId = :fixtureId AND market = :market
    """)
    suspend fun settlePredictionForMarket(fixtureId: String, market: String, isCorrect: Boolean, settledUtc: String)

    @Query("DELETE FROM predictions")
    suspend fun deleteAll()
}
