package com.example.engine

import com.example.data.model.PaperBetEntity
import com.example.data.model.PredictionEntity

/**
 * Data structure holding performance statistics and ranking position
 * for each machine learning model.
 */
data class ModelRankStats(
    val persona: ModelPersona,
    val rank: Int,
    val totalPredictions: Int,
    val correctPredictions: Int,
    val accuracyPercentage: Double,
    val totalBetsPlaced: Int,
    val wonBets: Int,
    val betWinRate: Double,
    val totalStaked: Double,
    val totalProfit: Double,
    val roiPercentage: Double,
    val marketAccuracies: Map<String, Double> // Market name -> accuracy %
)

object ModelRankingEngine {

    /**
     * Ranks models from best to worst based on prediction accuracy (% correct),
     * with secondary tie-breakers on total profit and ROI.
     */
    fun computeRankings(
        predictions: List<PredictionEntity>,
        paperBets: List<PaperBetEntity>
    ): List<ModelRankStats> {
        val models = ModelRegistry.ALL_MODELS
        val statsList = mutableListOf<ModelRankStats>()

        for (persona in models) {
            val modelPreds = predictions.filter { it.modelId == persona.id && it.isCorrect != null }
            val totalPreds = modelPreds.size
            val correctPreds = modelPreds.count { it.isCorrect == true }
            val accuracy = if (totalPreds > 0) (correctPreds.toDouble() / totalPreds) * 100.0 else 0.0

            // Market breakdowns
            val markets = listOf("1X2", "O/U 2.5", "BTTS", "DC")
            val marketAccMap = mutableMapOf<String, Double>()
            for (m in markets) {
                val mPreds = modelPreds.filter { it.market == m }
                val mTotal = mPreds.size
                val mCorrect = mPreds.count { it.isCorrect == true }
                marketAccMap[m] = if (mTotal > 0) (mCorrect.toDouble() / mTotal) * 100.0 else 0.0
            }

            // Paper bet performance
            val modelBets = paperBets.filter { it.modelId == persona.id && it.status != "PENDING" }
            val totalBets = modelBets.size
            val wonBets = modelBets.count { it.status == "WON" }
            val winRate = if (totalBets > 0) (wonBets.toDouble() / totalBets) * 100.0 else 0.0
            val totalStaked = modelBets.sumOf { it.stake }
            val totalProfit = modelBets.sumOf { it.netProfit }
            val roi = if (totalStaked > 0.0) (totalProfit / totalStaked) * 100.0 else 0.0

            statsList.add(
                ModelRankStats(
                    persona = persona,
                    rank = 0, // Assigned below
                    totalPredictions = totalPreds,
                    correctPredictions = correctPreds,
                    accuracyPercentage = accuracy,
                    totalBetsPlaced = totalBets,
                    wonBets = wonBets,
                    betWinRate = winRate,
                    totalStaked = totalStaked,
                    totalProfit = totalProfit,
                    roiPercentage = roi,
                    marketAccuracies = marketAccMap
                )
            )
        }

        // Sort from best to worst: highest accuracy first, then profit, then win rate
        val sorted = statsList.sortedWith(
            compareByDescending<ModelRankStats> { it.accuracyPercentage }
                .thenByDescending { it.correctPredictions }
                .thenByDescending { it.totalProfit }
                .thenByDescending { it.roiPercentage }
        )

        // Assign ranks 1 to N
        return sorted.mapIndexed { index, item ->
            item.copy(rank = index + 1)
        }
    }
}
