package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores a simulated bet placed by the player simulator.
 * Explicitly records amount betted, match outcome, payout, net profit or loss,
 * and updated bankroll for maximum transparency.
 */
@Entity(
    tableName = "paper_bets",
    indices = [
        Index(value = ["modelId", "status"]),
        Index(value = ["placedUtc"])
    ]
)
data class PaperBetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fixtureId: String,
    val roundId: String,
    val fixture: String,
    val modelId: String, // e.g. "model_consensus", "model_professor", etc.
    val market: String,  // "1X2", "O/U 2.5", "BTTS", "DC"
    val selection: String, // e.g. "Over 2.5", "Home", "BTTS Yes"
    val odds: Double,
    val stake: Double, // Amount betted
    val confidence: Double,
    val status: String = "PENDING", // "PENDING", "WON", "LOST"
    val ftScore: String? = null,
    val grossPayout: Double = 0.0,
    val netProfit: Double = 0.0, // positive if won, negative if lost
    val bankrollAfter: Double = 0.0,
    val placedUtc: String,
    val settledUtc: String? = null
)
