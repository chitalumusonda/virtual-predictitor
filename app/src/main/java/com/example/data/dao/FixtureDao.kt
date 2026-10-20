package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UnifiedFixtureRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface FixtureDao {

    @Query("SELECT * FROM unified_fixtures ORDER BY kickoffUtc DESC")
    fun getAllFixtures(): Flow<List<UnifiedFixtureRecord>>

    @Query("SELECT * FROM unified_fixtures WHERE status = 'UPCOMING' ORDER BY kickoffUtc ASC")
    fun getUpcomingFixtures(): Flow<List<UnifiedFixtureRecord>>

    @Query("SELECT * FROM unified_fixtures WHERE status = 'FINAL' ORDER BY kickoffUtc DESC LIMIT :limit")
    fun getRecentFinalFixtures(limit: Int = 100): Flow<List<UnifiedFixtureRecord>>

    @Query("SELECT * FROM unified_fixtures WHERE fixtureId = :fixtureId LIMIT 1")
    suspend fun getFixtureById(fixtureId: String): UnifiedFixtureRecord?

    @Query("SELECT * FROM unified_fixtures WHERE roundId = :roundId")
    suspend fun getFixturesByRound(roundId: String): List<UnifiedFixtureRecord>

    @Query("SELECT COUNT(*) FROM unified_fixtures")
    fun getFixtureCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: UnifiedFixtureRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<UnifiedFixtureRecord>)

    @Update
    suspend fun update(record: UnifiedFixtureRecord)

    @Query("DELETE FROM unified_fixtures")
    suspend fun deleteAll()
}
