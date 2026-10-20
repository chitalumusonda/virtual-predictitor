package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores both pre-match market odds and the subsequent post-match results
 * in the exact same record, establishing an explicit, auditable link between
 * pre-game pricing and final outcomes.
 */
@Entity(
    tableName = "unified_fixtures",
    indices = [
        Index(value = ["roundId", "fixtureId"], unique = true),
        Index(value = ["status", "kickoffUtc"]),
        Index(value = ["competitionId"])
    ]
)
data class UnifiedFixtureRecord(
    @PrimaryKey
    val fixtureId: String,
    val roundId: String,
    val matchday: Int,
    val seasonId: String,
    val competitionId: String,
    val competitionName: String,
    val fixture: String,
    val homeTeam: String,
    val awayTeam: String,
    val kickoffUtc: String,
    val capturedUtc: String, // Exact timestamp when pre-match odds were locked

    // Pre-match Odds (captured before game)
    // 1X2 Market
    val oddsHome: Double = 0.0,
    val oddsDraw: Double = 0.0,
    val oddsAway: Double = 0.0,

    // Over/Under 2.5 Market
    val oddsOver25: Double = 0.0,
    val oddsUnder25: Double = 0.0,

    // Over/Under 1.5 Market
    val oddsOver15: Double = 0.0,
    val oddsUnder15: Double = 0.0,

    // Both Teams to Score (BTTS) Market
    val oddsBttsYes: Double = 0.0,
    val oddsBttsNo: Double = 0.0,

    // Double Chance Market
    val oddsDc1X: Double = 0.0,
    val oddsDc12: Double = 0.0,
    val oddsDcX2: Double = 0.0,

    // Subsequent Match Results (linked directly in the exact same database record)
    val status: String = "UPCOMING", // "UPCOMING", "LIVE", "FINAL"
    val ftHome: Int? = null,
    val ftAway: Int? = null,
    val htHome: Int? = null,
    val htAway: Int? = null,
    val outcome: String? = null, // "1", "X", "2"
    val totalGoals: Int? = null,
    val bttsResult: String? = null, // "Yes", "No"
    val ou25Result: String? = null, // "Over", "Under"
    val ou15Result: String? = null, // "Over", "Under"
    val dcResult: String? = null,   // "1X", "12", "X2"
    val updatedUtc: String = capturedUtc // Exact timestamp when post-match scores were recorded
) {
    fun isHomeWinHit(): Boolean? = if (status != "FINAL") null else outcome == "1"
    fun isDrawHit(): Boolean? = if (status != "FINAL") null else outcome == "X"
    fun isAwayWinHit(): Boolean? = if (status != "FINAL") null else outcome == "2"
    fun isOver25Hit(): Boolean? = if (status != "FINAL") null else (totalGoals ?: 0) >= 3
    fun isUnder25Hit(): Boolean? = if (status != "FINAL") null else (totalGoals ?: 0) < 3
    fun isBttsYesHit(): Boolean? = if (status != "FINAL") null else bttsResult == "Yes"
    fun isBttsNoHit(): Boolean? = if (status != "FINAL") null else bttsResult == "No"
}
