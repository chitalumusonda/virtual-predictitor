package com.example.engine

import com.example.data.model.PaperBetEntity
import com.example.data.model.PredictionEntity
import com.example.data.model.UnifiedFixtureRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class SimulationFinancialSummary(
    val initialBankroll: Double,
    val currentBankroll: Double,
    val totalBetted: Double,       // Total amount staked across all bets
    val totalWonProfit: Double,    // Net earnings from winning bets
    val totalLossAmount: Double,   // Total capital lost from losing bets
    val totalPayout: Double,       // Gross payout returned
    val netProfitOrLoss: Double,   // Net bottom line (totalWonProfit - totalLossAmount)
    val roiPercentage: Double,     // Return on Investment %
    val totalBetsCount: Int,
    val wonBetsCount: Int,
    val lostBetsCount: Int,
    val winRatePercentage: Double
)

data class ModelSimulationComparison(
    val model: ModelPersona,
    val summary: SimulationFinancialSummary
)

object PlayerSimulatorEngine {

    private fun currentUtcString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Generates simulated bets for a list of fixtures using the selected model's predictions.
     * Supports both individual models and the Consolidated Consensus Hive (most common selection).
     */
    fun createBetsForRound(
        fixtures: List<UnifiedFixtureRecord>,
        predictions: List<PredictionEntity>,
        activeModelId: String,
        stakePerBet: Double,
        currentBankroll: Double
    ): List<PaperBetEntity> {
        val newBets = mutableListOf<PaperBetEntity>()
        var runningBankroll = currentBankroll
        val timestamp = currentUtcString()

        for (fixture in fixtures) {
            val modelPreds = predictions.filter {
                it.fixtureId == fixture.fixtureId && it.modelId == activeModelId
            }

            if (modelPreds.isEmpty()) continue

            // Pick the prediction with the highest model confidence
            val topPrediction = modelPreds.maxByOrNull { it.confidence } ?: modelPreds.first()

            if (runningBankroll < stakePerBet) {
                break // Insufficient bankroll
            }

            runningBankroll -= stakePerBet

            val bet = PaperBetEntity(
                fixtureId = fixture.fixtureId,
                roundId = fixture.roundId,
                fixture = fixture.fixture,
                modelId = activeModelId,
                market = topPrediction.market,
                selection = topPrediction.predictedSelection,
                odds = topPrediction.oddsAtPrediction,
                stake = stakePerBet,
                confidence = topPrediction.confidence,
                status = "PENDING",
                ftScore = null,
                grossPayout = 0.0,
                netProfit = 0.0,
                bankrollAfter = runningBankroll,
                placedUtc = timestamp
            )
            newBets.add(bet)
        }

        return newBets
    }

    /**
     * Settles a pending bet once the match is final, recording exact amount betted,
     * outcome, gross payout, net profit/loss, and updated bankroll.
     */
    fun settleBet(
        bet: PaperBetEntity,
        fixtureRecord: UnifiedFixtureRecord,
        priorBankroll: Double
    ): PaperBetEntity {
        if (fixtureRecord.status != "FINAL" || fixtureRecord.ftHome == null || fixtureRecord.ftAway == null) {
            return bet
        }

        val timestamp = currentUtcString()
        val score = "${fixtureRecord.ftHome} - ${fixtureRecord.ftAway}"
        val isWon = evaluateBetOutcome(bet, fixtureRecord)

        val grossPayout = if (isWon) bet.stake * bet.odds else 0.0
        val netProfit = if (isWon) (bet.stake * bet.odds) - bet.stake else -bet.stake
        val bankrollAfter = priorBankroll + netProfit

        return bet.copy(
            status = if (isWon) "WON" else "LOST",
            ftScore = score,
            grossPayout = grossPayout,
            netProfit = netProfit,
            bankrollAfter = bankrollAfter,
            settledUtc = timestamp
        )
    }

    private fun evaluateBetOutcome(bet: PaperBetEntity, f: UnifiedFixtureRecord): Boolean {
        val ftH = f.ftHome ?: return false
        val ftA = f.ftAway ?: return false
        val totalGoals = ftH + ftA

        return when (bet.market) {
            "1X2" -> when (bet.selection) {
                "Home", "1" -> ftH > ftA
                "Draw", "X" -> ftH == ftA
                "Away", "2" -> ftH < ftA
                else -> false
            }
            "O/U 2.5" -> if (bet.selection.contains("Over")) totalGoals >= 3 else totalGoals < 3
            "O/U 1.5" -> if (bet.selection.contains("Over")) totalGoals >= 2 else totalGoals < 2
            "BTTS" -> {
                val bothScored = ftH > 0 && ftA > 0
                if (bet.selection.contains("Yes")) bothScored else !bothScored
            }
            "DC" -> when (bet.selection) {
                "1X" -> ftH >= ftA
                "12" -> ftH != ftA
                "X2" -> ftH <= ftA
                else -> false
            }
            else -> false
        }
    }

    /**
     * Calculates transparent financial KPI statistics with explicit separation
     * of Total Betted, Total Profit from wins, and Total Loss from losses.
     */
    fun calculateFinancials(
        startingBankroll: Double,
        settledBets: List<PaperBetEntity>
    ): SimulationFinancialSummary {
        val totalBetted = settledBets.sumOf { it.stake }
        val totalPayout = settledBets.sumOf { it.grossPayout }
        val totalWonProfit = settledBets.filter { it.status == "WON" }.sumOf { it.netProfit }
        val totalLossAmount = settledBets.filter { it.status == "LOST" }.sumOf { kotlin.math.abs(it.netProfit) }
        val netProfitOrLoss = settledBets.sumOf { it.netProfit }
        val currentBankroll = startingBankroll + netProfitOrLoss
        val roi = if (totalBetted > 0.0) (netProfitOrLoss / totalBetted) * 100.0 else 0.0

        val totalCount = settledBets.size
        val wonCount = settledBets.count { it.status == "WON" }
        val lostCount = settledBets.count { it.status == "LOST" }
        val winRate = if (totalCount > 0) (wonCount.toDouble() / totalCount) * 100.0 else 0.0

        return SimulationFinancialSummary(
            initialBankroll = startingBankroll,
            currentBankroll = currentBankroll,
            totalBetted = totalBetted,
            totalWonProfit = totalWonProfit,
            totalLossAmount = totalLossAmount,
            totalPayout = totalPayout,
            netProfitOrLoss = netProfitOrLoss,
            roiPercentage = roi,
            totalBetsCount = totalCount,
            wonBetsCount = wonCount,
            lostBetsCount = lostCount,
            winRatePercentage = winRate
        )
    }

    /**
     * Computes side-by-side financial comparisons for all models.
     */
    fun compareAllModels(
        startingBankroll: Double,
        allSettledBets: List<PaperBetEntity>
    ): List<ModelSimulationComparison> {
        return ModelRegistry.ALL_MODELS.map { model ->
            val modelBets = allSettledBets.filter { it.modelId == model.id }
            ModelSimulationComparison(
                model = model,
                summary = calculateFinancials(startingBankroll, modelBets)
            )
        }
    }
}
