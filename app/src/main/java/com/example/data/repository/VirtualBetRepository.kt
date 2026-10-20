package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.CollectionCheckpoint
import com.example.data.model.PaperBetEntity
import com.example.data.model.PredictionEntity
import com.example.data.model.UnifiedFixtureRecord
import com.example.engine.ModelRegistry
import com.example.engine.PlayerSimulatorEngine
import com.example.engine.PredictionEngine
import com.example.engine.VirtualLeagueGenerator
import com.example.network.BetPawaClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class VirtualBetRepository(
    private val database: AppDatabase,
    private val networkClient: BetPawaClient
) {
    private val fixtureDao = database.fixtureDao()
    private val checkpointDao = database.checkpointDao()
    private val predictionDao = database.predictionDao()
    private val paperBetDao = database.paperBetDao()

    val allFixtures: Flow<List<UnifiedFixtureRecord>> = fixtureDao.getAllFixtures()
    val upcomingFixtures: Flow<List<UnifiedFixtureRecord>> = fixtureDao.getUpcomingFixtures()
    val recentFinalFixtures: Flow<List<UnifiedFixtureRecord>> = fixtureDao.getRecentFinalFixtures()
    val checkpoint: Flow<CollectionCheckpoint?> = checkpointDao.getCheckpoint()
    val allPredictions: Flow<List<PredictionEntity>> = predictionDao.getAllPredictions()
    val allBets: Flow<List<PaperBetEntity>> = paperBetDao.getAllBets()

    private fun currentUtcString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Initializes checkpoint, registers system network drop listeners for auto-checkpointing,
     * and seeds initial sample records if fresh install.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        // Register real-time OS network callback for automatic state-saving on connection loss
        networkClient.registerConnectivityCallback(
            onConnectionLost = {
                CoroutineScope(Dispatchers.IO).launch {
                    handleConnectionInterruption("Network signal lost (OS network dropped)")
                }
            },
            onConnectionAvailable = {
                CoroutineScope(Dispatchers.IO).launch {
                    val cp = checkpointDao.getCurrentCheckpoint()
                    if (cp != null && cp.autoResumeOnReconnect) {
                        checkpointDao.updateConnectionStatus("CONNECTED")
                    }
                }
            }
        )

        var cp = checkpointDao.getCurrentCheckpoint()
        if (cp == null) {
            val now = currentUtcString()
            cp = CollectionCheckpoint(
                id = 1,
                connectionStatus = if (networkClient.isNetworkAvailable()) "CONNECTED" else "DISCONNECTED",
                isCollecting = true,
                savedStartPointRoundId = "RND-24-001",
                savedStartPointMatchday = 24,
                savedStartPointSeasonId = "season-2026-zambia",
                savedStartPointUtc = now,
                lastSuccessfulCaptureUtc = now,
                totalCapturedOdds = 0,
                totalFinishedMatches = 0
            )
            checkpointDao.saveCheckpoint(cp)
        }

        val existingUpcoming = fixtureDao.getFixturesByRound(cp.savedStartPointRoundId)
        if (existingUpcoming.isEmpty()) {
            seedSampleRounds()
        }
    }

    /**
     * Automatically saves current data collection state and starting point when
     * an unexpected network interruption occurs.
     */
    suspend fun handleConnectionInterruption(reason: String) = withContext(Dispatchers.IO) {
        val cp = checkpointDao.getCurrentCheckpoint() ?: return@withContext
        val now = currentUtcString()
        val startPointRound = cp.savedStartPointRoundId.ifEmpty { "RND-${cp.savedStartPointMatchday}" }
        checkpointDao.saveCheckpoint(
            cp.copy(
                connectionStatus = "INTERRUPTED",
                savedStartPointRoundId = startPointRound,
                savedStartPointMatchday = cp.savedStartPointMatchday,
                savedStartPointUtc = now,
                lastErrorMessage = "Auto-Saved on Interruption: Start Point at Matchday ${cp.savedStartPointMatchday} ($startPointRound)."
            )
        )
    }

    /**
     * Seed initial rounds (historical completed rounds for model learning and rankings,
     * plus upcoming round with predictions for pre-match board).
     */
    suspend fun seedSampleRounds() = withContext(Dispatchers.IO) {
        val season = "season-2026-zambia"

        // Generate 4 previous completed rounds
        for (day in 20..23) {
            val rId = "RND-$day"
            val rawFixtures = VirtualLeagueGenerator.generateUpcomingRound(season, day, rId)
            val finalFixtures = rawFixtures.map { VirtualLeagueGenerator.simulateMatchResults(it) }
            fixtureDao.insertAll(finalFixtures)

            // Generate and settle predictions
            for (fix in finalFixtures) {
                val preds = PredictionEngine.generatePredictionsForFixture(fix)
                val settledPreds = preds.map { p ->
                    val isWon = PredictionEngine.evaluatePredictionOutcome(p, fix)
                    p.copy(isCorrect = isWon, settledUtc = fix.updatedUtc)
                }
                predictionDao.insertAll(settledPreds)

                // Place and settle paper bets for each model and consensus
                var bankroll = 1000.0
                for (model in ModelRegistry.ALL_MODELS) {
                    val bets = PlayerSimulatorEngine.createBetsForRound(
                        listOf(fix),
                        settledPreds,
                        model.id,
                        10.0,
                        bankroll
                    )
                    for (bet in bets) {
                        val settledBet = PlayerSimulatorEngine.settleBet(bet, fix, bankroll)
                        paperBetDao.insertBet(settledBet)
                    }
                }
            }
        }

        // Generate current upcoming round 24
        val upcomingRoundId = "RND-24"
        val upcomingFixtures = VirtualLeagueGenerator.generateUpcomingRound(season, 24, upcomingRoundId)
        fixtureDao.insertAll(upcomingFixtures)

        // Generate pre-match predictions across all models!
        for (fix in upcomingFixtures) {
            val preds = PredictionEngine.generatePredictionsForFixture(fix)
            predictionDao.insertAll(preds)
        }

        checkpointDao.updateStartPoint(upcomingRoundId, 24, season, currentUtcString())
    }

    /**
     * Advances to the next round: settles existing round and creates new upcoming round.
     */
    suspend fun collectNextRound(forceVirtual: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        val cp = checkpointDao.getCurrentCheckpoint() ?: CollectionCheckpoint()
        val currentMatchday = cp.savedStartPointMatchday
        val isOnline = networkClient.isNetworkAvailable()

        if (!isOnline && !forceVirtual) {
            handleConnectionInterruption("Offline: network unavailable")
            return@withContext Result.failure(
                IllegalStateException("Connection interrupted. Auto-saved recovery start point at Matchday $currentMatchday.")
            )
        }

        try {
            // Progress current upcoming matches to final results
            val existingUpcoming = fixtureDao.getFixturesByRound(cp.savedStartPointRoundId)
            if (existingUpcoming.isNotEmpty()) {
                val finalized = existingUpcoming.map { VirtualLeagueGenerator.simulateMatchResults(it) }
                // Update subsequent results in the exact same record!
                fixtureDao.insertAll(finalized)

                // Grade all predictions for this round
                for (f in finalized) {
                    val pendingPreds = predictionDao.getPendingPredictionsForFixture(f.fixtureId)
                    for (p in pendingPreds) {
                        val isCorrect = PredictionEngine.evaluatePredictionOutcome(p, f)
                        if (isCorrect != null) {
                            predictionDao.settlePredictionForMarket(f.fixtureId, p.market, isCorrect, f.updatedUtc)
                        }
                    }

                    // Settle pending paper bets
                    val pendingBets = paperBetDao.getPendingBetsForFixture(f.fixtureId)
                    for (b in pendingBets) {
                        val settled = PlayerSimulatorEngine.settleBet(b, f, b.bankrollAfter + b.stake)
                        paperBetDao.updateBet(settled)
                    }
                }
            }

            // Advance to next round
            val nextMatchday = if (currentMatchday >= 38) 1 else currentMatchday + 1
            val nextRoundId = "RND-$nextMatchday-${System.currentTimeMillis() % 10000}"
            val seasonId = cp.savedStartPointSeasonId.ifEmpty { "season-2026-zambia" }

            // Generate upcoming round fixtures with pre-match odds
            val nextUpcoming = VirtualLeagueGenerator.generateUpcomingRound(seasonId, nextMatchday, nextRoundId)
            fixtureDao.insertAll(nextUpcoming)

            // Generate predictions for all models across all betting markets
            for (fix in nextUpcoming) {
                val preds = PredictionEngine.generatePredictionsForFixture(fix)
                predictionDao.insertAll(preds)
            }

            // Save updated checkpoint
            val now = currentUtcString()
            checkpointDao.saveCheckpoint(
                cp.copy(
                    connectionStatus = if (isOnline) "CONNECTED" else "SIMULATED",
                    savedStartPointRoundId = nextRoundId,
                    savedStartPointMatchday = nextMatchday,
                    savedStartPointSeasonId = seasonId,
                    savedStartPointUtc = now,
                    lastSuccessfulCaptureUtc = now,
                    totalCapturedOdds = cp.totalCapturedOdds + (nextUpcoming.size * 10),
                    totalFinishedMatches = cp.totalFinishedMatches + existingUpcoming.size,
                    lastErrorMessage = null
                )
            )

            Result.success("Round $nextMatchday processed. Odds & predictions ready.")
        } catch (e: Exception) {
            handleConnectionInterruption("Exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Resumes data collection from the explicitly saved start point.
     */
    suspend fun resumeFromStartPoint(matchday: Int, roundId: String) = withContext(Dispatchers.IO) {
        val cp = checkpointDao.getCurrentCheckpoint() ?: CollectionCheckpoint()
        val now = currentUtcString()
        checkpointDao.saveCheckpoint(
            cp.copy(
                connectionStatus = if (networkClient.isNetworkAvailable()) "CONNECTED" else "RESUMING",
                savedStartPointMatchday = matchday,
                savedStartPointRoundId = roundId.ifEmpty { "RND-$matchday" },
                savedStartPointUtc = now,
                lastErrorMessage = null
            )
        )
        collectNextRound(forceVirtual = true)
    }

    /**
     * Places a simulated bet using the chosen model (individual model or Consensus Hive).
     */
    suspend fun placeSimulatedBet(
        fixture: UnifiedFixtureRecord,
        modelId: String,
        stake: Double,
        currentBankroll: Double
    ) = withContext(Dispatchers.IO) {
        val preds = predictionDao.getPredictionsByRound(fixture.roundId)
        val newBets = PlayerSimulatorEngine.createBetsForRound(
            listOf(fixture),
            preds,
            modelId,
            stake,
            currentBankroll
        )
        for (b in newBets) {
            paperBetDao.insertBet(b)
        }
    }

    /**
     * Runs simulations for ALL models simultaneously on upcoming round so users
     * can directly compare how each model performs against Consolidated Consensus.
     */
    suspend fun placeSimulatedBetsForAllModels(
        stake: Double,
        currentBankroll: Double
    ) = withContext(Dispatchers.IO) {
        val upcoming = fixtureDao.getUpcomingFixtures()
        // We will collect the first list from the Flow in ViewModel or query directly
        val cp = checkpointDao.getCurrentCheckpoint() ?: return@withContext
        val currentRoundFixtures = fixtureDao.getFixturesByRound(cp.savedStartPointRoundId)
        val preds = predictionDao.getPredictionsByRound(cp.savedStartPointRoundId)

        for (model in ModelRegistry.ALL_MODELS) {
            val bets = PlayerSimulatorEngine.createBetsForRound(
                currentRoundFixtures,
                preds,
                model.id,
                stake,
                currentBankroll
            )
            for (b in bets) {
                paperBetDao.insertBet(b)
            }
        }
    }
}
