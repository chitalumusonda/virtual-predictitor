package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists connection status, the designated Start Point checkpoint for resumption,
 * and collection progress metrics.
 */
@Entity(tableName = "collection_checkpoint")
data class CollectionCheckpoint(
    @PrimaryKey
    val id: Int = 1, // Single record pattern
    val connectionStatus: String = "CONNECTED", // "CONNECTED", "DISCONNECTED", "CONNECTING", "RESUMING"
    val isCollecting: Boolean = true,
    val savedStartPointRoundId: String = "",
    val savedStartPointMatchday: Int = 1,
    val savedStartPointSeasonId: String = "",
    val savedStartPointUtc: String = "",
    val lastSuccessfulCaptureUtc: String = "",
    val totalCapturedOdds: Int = 0,
    val totalFinishedMatches: Int = 0,
    val lastErrorMessage: String? = null,
    val autoResumeOnReconnect: Boolean = true
)
