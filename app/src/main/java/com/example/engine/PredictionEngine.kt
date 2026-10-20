package com.example.engine

import com.example.data.model.PredictionEntity
import com.example.data.model.UnifiedFixtureRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

/**
 * Multi-market prediction engine that supports:
 * - 1X2 (Home / Draw / Away)
 * - Over/Under 2.5 & Over/Under 1.5
 * - Both Teams To Score (BTTS Yes / No)
 * - Double Chance (1X, 12, X2)
 *
 * Implements distinct algorithmic philosophies for each model persona and
 * creates the master Consensus Hive that pools all models and picks the
 * most common selection.
 */
object PredictionEngine {

    private fun currentUtcString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Generates predictions for all registered models across all betting markets
     * for a given fixture.
     */
    fun generatePredictionsForFixture(fixture: UnifiedFixtureRecord): List<PredictionEntity> {
        val timestamp = currentUtcString()
        val allPredictions = mutableListOf<PredictionEntity>()

        // 1. Generate predictions from each individual model
        val individualModels = ModelRegistry.INDIVIDUAL_MODELS
        val individualPredictions = mutableListOf<PredictionEntity>()

        for (model in individualModels) {
            val preds = generateModelPredictions(model, fixture, timestamp)
            individualPredictions.addAll(preds)
        }
        allPredictions.addAll(individualPredictions)

        // 2. Synthesize Consensus Hive (Majority Vote / Most Common Selection)
        val consensusPredictions = generateConsensusPredictions(
            individualPredictions,
            fixture,
            timestamp
        )
        allPredictions.addAll(consensusPredictions)

        return allPredictions
    }

    private fun generateModelPredictions(
        model: ModelPersona,
        f: UnifiedFixtureRecord,
        timestamp: String
    ): List<PredictionEntity> {
        val results = mutableListOf<PredictionEntity>()

        // Normalizing 1X2 odds
        val rawHome = if (f.oddsHome > 1.0) f.oddsHome else 2.10
        val rawDraw = if (f.oddsDraw > 1.0) f.oddsDraw else 3.20
        val rawAway = if (f.oddsAway > 1.0) f.oddsAway else 3.40

        val invHome = 1.0 / rawHome
        val invDraw = 1.0 / rawDraw
        val invAway = 1.0 / rawAway
        val sumInv = invHome + invDraw + invAway
        val baseProbHome = invHome / sumInv
        val baseProbDraw = invDraw / sumInv
        val baseProbAway = invAway / sumInv

        // Expected goals approximation
        val expGoalsHome = max(0.5, min(3.5, 1.4 * (baseProbHome / (baseProbAway + 0.1))))
        val expGoalsAway = max(0.4, min(3.0, 1.1 * (baseProbAway / (baseProbHome + 0.1))))
        val expTotalGoals = expGoalsHome + expGoalsAway

        when (model.id) {
            "model_market" -> {
                // Market Maven: Pure implied probabilities
                // 1X2
                val pick1X2 = when {
                    baseProbHome >= baseProbAway && baseProbHome >= baseProbDraw -> "Home" to rawHome
                    baseProbAway >= baseProbHome && baseProbAway >= baseProbDraw -> "Away" to rawAway
                    else -> "Draw" to rawDraw
                }
                val conf1X2 = maxOf(baseProbHome, baseProbDraw, baseProbAway)
                results.add(createPrediction(f, model.id, "1X2", pick1X2.first, conf1X2, pick1X2.second, timestamp))

                // O/U 2.5
                val pOver25 = if (f.oddsOver25 > 1.0) (1.0 / f.oddsOver25) / ((1.0 / f.oddsOver25) + (1.0 / max(1.01, f.oddsUnder25))) else 0.52
                val pickOU25 = if (pOver25 >= 0.50) "Over 2.5" to f.oddsOver25 else "Under 2.5" to f.oddsUnder25
                results.add(createPrediction(f, model.id, "O/U 2.5", pickOU25.first, max(pOver25, 1.0 - pOver25), pickOU25.second, timestamp))

                // O/U 1.5
                val pOver15 = if (f.oddsOver15 > 1.0) (1.0 / f.oddsOver15) / ((1.0 / f.oddsOver15) + (1.0 / max(1.01, f.oddsUnder15))) else 0.72
                val pickOU15 = if (pOver15 >= 0.50) "Over 1.5" to f.oddsOver15 else "Under 1.5" to f.oddsUnder15
                results.add(createPrediction(f, model.id, "O/U 1.5", pickOU15.first, max(pOver15, 1.0 - pOver15), pickOU15.second, timestamp))

                // BTTS
                val pBtts = if (f.oddsBttsYes > 1.0) (1.0 / f.oddsBttsYes) / ((1.0 / f.oddsBttsYes) + (1.0 / max(1.01, f.oddsBttsNo))) else 0.55
                val pickBtts = if (pBtts >= 0.50) "BTTS Yes" to f.oddsBttsYes else "BTTS No" to f.oddsBttsNo
                results.add(createPrediction(f, model.id, "BTTS", pickBtts.first, max(pBtts, 1.0 - pBtts), pickBtts.second, timestamp))

                // DC
                val pickDc = if (baseProbHome >= baseProbAway) "1X" to f.oddsDc1X else "X2" to f.oddsDcX2
                results.add(createPrediction(f, model.id, "DC", pickDc.first, 0.70, pickDc.second, timestamp))
            }

            "model_professor" -> {
                // The Professor: Statistical value with Poisson math
                // Home advantage factor boost
                val profHomeProb = min(0.85, baseProbHome * 1.06)
                val profAwayProb = baseProbAway * 0.94
                val profDrawProb = max(0.15, 1.0 - profHomeProb - profAwayProb)

                val pick1X2 = when {
                    profHomeProb >= profAwayProb && profHomeProb >= profDrawProb -> "Home" to rawHome
                    profAwayProb >= profHomeProb && profAwayProb >= profDrawProb -> "Away" to rawAway
                    else -> "Draw" to rawDraw
                }
                results.add(createPrediction(f, model.id, "1X2", pick1X2.first, maxOf(profHomeProb, profAwayProb, profDrawProb), pick1X2.second, timestamp))

                // Poisson probability for 0, 1, 2 goals
                val p012 = poissonCdf(2, expTotalGoals)
                val isUnder25 = p012 > 0.48
                val pickOU25 = if (isUnder25) "Under 2.5" to f.oddsUnder25 else "Over 2.5" to f.oddsOver25
                results.add(createPrediction(f, model.id, "O/U 2.5", pickOU25.first, if (isUnder25) p012 else 1.0 - p012, pickOU25.second, timestamp))

                // O/U 1.5
                val p01 = poissonCdf(1, expTotalGoals)
                val isOver15 = (1.0 - p01) >= 0.60
                val pickOU15 = if (isOver15) "Over 1.5" to f.oddsOver15 else "Under 1.5" to f.oddsUnder15
                results.add(createPrediction(f, model.id, "O/U 1.5", pickOU15.first, if (isOver15) 1.0 - p01 else p01, pickOU15.second, timestamp))

                // BTTS Poisson: (1 - exp(-h)) * (1 - exp(-a))
                val pBtts = (1.0 - exp(-expGoalsHome)) * (1.0 - exp(-expGoalsAway))
                val pickBtts = if (pBtts > 0.50) "BTTS Yes" to f.oddsBttsYes else "BTTS No" to f.oddsBttsNo
                results.add(createPrediction(f, model.id, "BTTS", pickBtts.first, max(pBtts, 1.0 - pBtts), pickBtts.second, timestamp))

                // DC
                val pickDc = if (profHomeProb >= profAwayProb) "1X" to f.oddsDc1X else "12" to f.oddsDc12
                results.add(createPrediction(f, model.id, "DC", pickDc.first, 0.74, pickDc.second, timestamp))
            }

            "model_forest" -> {
                // Forest Oracle: Decision tree ensemble
                // Tree votes across attack/defense ratios
                val ratio = expGoalsHome / max(0.1, expGoalsAway)
                val pick1X2 = when {
                    ratio > 1.25 -> "Home" to rawHome
                    ratio < 0.80 -> "Away" to rawAway
                    else -> "Draw" to rawDraw
                }
                val conf1X2 = min(0.88, 0.50 + abs(ratio - 1.0) * 0.25)
                results.add(createPrediction(f, model.id, "1X2", pick1X2.first, conf1X2, pick1X2.second, timestamp))

                // Tree votes on goals
                val pickOU25 = if (expTotalGoals >= 2.65) "Over 2.5" to f.oddsOver25 else "Under 2.5" to f.oddsUnder25
                results.add(createPrediction(f, model.id, "O/U 2.5", pickOU25.first, 0.64, pickOU25.second, timestamp))

                val pickOU15 = if (expTotalGoals >= 1.85) "Over 1.5" to f.oddsOver15 else "Under 1.5" to f.oddsUnder15
                results.add(createPrediction(f, model.id, "O/U 1.5", pickOU15.first, 0.75, pickOU15.second, timestamp))

                val bttsScore = (expGoalsHome * expGoalsAway) / (expTotalGoals + 0.1)
                val pickBtts = if (bttsScore > 0.52) "BTTS Yes" to f.oddsBttsYes else "BTTS No" to f.oddsBttsNo
                results.add(createPrediction(f, model.id, "BTTS", pickBtts.first, 0.62, pickBtts.second, timestamp))

                val pickDc = if (ratio >= 1.0) "1X" to f.oddsDc1X else "X2" to f.oddsDcX2
                results.add(createPrediction(f, model.id, "DC", pickDc.first, 0.72, pickDc.second, timestamp))
            }

            "model_titan" -> {
                // Gradient Titan: Momentum & high volatility offensive booster
                val isHighPace = expTotalGoals > 2.5 || (f.oddsOver25 in 1.1..1.90)
                val pick1X2 = if (baseProbHome >= baseProbAway) "Home" to rawHome else "Away" to rawAway
                results.add(createPrediction(f, model.id, "1X2", pick1X2.first, 0.68, pick1X2.second, timestamp))

                // Titan loves Over 2.5 and BTTS
                val pickOU25 = if (isHighPace) "Over 2.5" to f.oddsOver25 else "Under 2.5" to f.oddsUnder25
                results.add(createPrediction(f, model.id, "O/U 2.5", pickOU25.first, 0.70, pickOU25.second, timestamp))

                val pickOU15 = "Over 1.5" to f.oddsOver15
                results.add(createPrediction(f, model.id, "O/U 1.5", pickOU15.first, 0.82, pickOU15.second, timestamp))

                val pickBtts = if (isHighPace) "BTTS Yes" to f.oddsBttsYes else "BTTS No" to f.oddsBttsNo
                results.add(createPrediction(f, model.id, "BTTS", pickBtts.first, 0.65, pickBtts.second, timestamp))

                val pickDc = "12" to f.oddsDc12 // Decisive outcome
                results.add(createPrediction(f, model.id, "DC", pickDc.first, 0.76, pickDc.second, timestamp))
            }

            "model_defense" -> {
                // Defense Guardian: Goal suppression & Under specialist
                val pick1X2 = if (baseProbDraw > 0.28 || abs(baseProbHome - baseProbAway) < 0.12) {
                    "Draw" to rawDraw
                } else if (baseProbHome >= baseProbAway) {
                    "Home" to rawHome
                } else {
                    "Away" to rawAway
                }
                results.add(createPrediction(f, model.id, "1X2", pick1X2.first, 0.58, pick1X2.second, timestamp))

                // Strongly favors Under 2.5 unless huge mismatch
                val isUnder = expTotalGoals <= 2.85 || f.oddsUnder25 <= 2.10
                val pickOU25 = if (isUnder) "Under 2.5" to f.oddsUnder25 else "Over 2.5" to f.oddsOver25
                results.add(createPrediction(f, model.id, "O/U 2.5", pickOU25.first, 0.73, pickOU25.second, timestamp))

                val pickOU15 = if (expTotalGoals < 1.7) "Under 1.5" to f.oddsUnder15 else "Over 1.5" to f.oddsOver15
                results.add(createPrediction(f, model.id, "O/U 1.5", pickOU15.first, 0.65, pickOU15.second, timestamp))

                // Favors BTTS No
                val pickBtts = if (expGoalsHome < 1.1 || expGoalsAway < 0.95) "BTTS No" to f.oddsBttsNo else "BTTS Yes" to f.oddsBttsYes
                results.add(createPrediction(f, model.id, "BTTS", pickBtts.first, 0.66, pickBtts.second, timestamp))

                val pickDc = if (baseProbHome >= baseProbAway) "1X" to f.oddsDc1X else "X2" to f.oddsDcX2
                results.add(createPrediction(f, model.id, "DC", pickDc.first, 0.78, pickDc.second, timestamp))
            }
        }

        return results
    }

    /**
     * Synthesizes Consensus Hive predictions by picking the MOST COMMON selection
     * across all individual models for each market.
     */
    private fun generateConsensusPredictions(
        allIndividualPredictions: List<PredictionEntity>,
        fixture: UnifiedFixtureRecord,
        timestamp: String
    ): List<PredictionEntity> {
        val consensusList = mutableListOf<PredictionEntity>()
        val markets = listOf("1X2", "O/U 2.5", "O/U 1.5", "BTTS", "DC")

        for (market in markets) {
            val marketPredictions = allIndividualPredictions.filter { it.market == market }
            if (marketPredictions.isEmpty()) continue

            // Count votes for each selection
            val voteCounts = mutableMapOf<String, Int>()
            val confidenceSums = mutableMapOf<String, Double>()
            val oddsMap = mutableMapOf<String, Double>()

            for (p in marketPredictions) {
                val sel = p.predictedSelection
                voteCounts[sel] = (voteCounts[sel] ?: 0) + 1
                confidenceSums[sel] = (confidenceSums[sel] ?: 0.0) + p.confidence
                oddsMap[sel] = p.oddsAtPrediction
            }

            // Find most common selection (most votes)
            // If tie, pick selection with highest average confidence
            val topSelection = voteCounts.entries.sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenByDescending { (confidenceSums[it.key] ?: 0.0) / (it.value) }
            ).firstOrNull()?.key ?: marketPredictions.first().predictedSelection

            val votes = voteCounts[topSelection] ?: 1
            val totalVotes = marketPredictions.size
            val avgConfidence = (confidenceSums[topSelection] ?: 0.5) / votes
            // Consensus confidence increases with consensus agreement proportion
            val consensusConfidence = min(0.95, (votes.toDouble() / totalVotes) * 0.5 + avgConfidence * 0.5)
            val odds = oddsMap[topSelection] ?: 1.80

            consensusList.add(
                createPrediction(
                    fixture = fixture,
                    modelId = ModelRegistry.CONSENSUS.id,
                    market = market,
                    selection = topSelection,
                    confidence = consensusConfidence,
                    odds = odds,
                    timestamp = timestamp
                )
            )
        }

        return consensusList
    }

    private fun createPrediction(
        fixture: UnifiedFixtureRecord,
        modelId: String,
        market: String,
        selection: String,
        confidence: Double,
        odds: Double,
        timestamp: String
    ): PredictionEntity {
        return PredictionEntity(
            fixtureId = fixture.fixtureId,
            roundId = fixture.roundId,
            fixture = fixture.fixture,
            modelId = modelId,
            market = market,
            predictedSelection = selection,
            confidence = min(0.99, max(0.40, confidence)),
            oddsAtPrediction = if (odds > 1.0) odds else 1.80,
            predictedUtc = timestamp
        )
    }

    /**
     * Grades whether a prediction won or lost based on final match scores.
     */
    fun evaluatePredictionOutcome(prediction: PredictionEntity, record: UnifiedFixtureRecord): Boolean? {
        if (record.status != "FINAL" || record.ftHome == null || record.ftAway == null) {
            return null // Not yet finished
        }

        val ftH = record.ftHome
        val ftA = record.ftAway
        val totalGoals = ftH + ftA
        val outcome1X2 = when {
            ftH > ftA -> "Home"
            ftH < ftA -> "Away"
            else -> "Draw"
        }

        return when (prediction.market) {
            "1X2" -> {
                when (prediction.predictedSelection) {
                    "Home", "1" -> outcome1X2 == "Home"
                    "Draw", "X" -> outcome1X2 == "Draw"
                    "Away", "2" -> outcome1X2 == "Away"
                    else -> false
                }
            }
            "O/U 2.5" -> {
                if (prediction.predictedSelection.contains("Over")) {
                    totalGoals >= 3
                } else {
                    totalGoals < 3
                }
            }
            "O/U 1.5" -> {
                if (prediction.predictedSelection.contains("Over")) {
                    totalGoals >= 2
                } else {
                    totalGoals < 2
                }
            }
            "BTTS" -> {
                val bothScored = ftH > 0 && ftA > 0
                if (prediction.predictedSelection.contains("Yes")) {
                    bothScored
                } else {
                    !bothScored
                }
            }
            "DC" -> {
                when (prediction.predictedSelection) {
                    "1X" -> ftH >= ftA
                    "12" -> ftH != ftA
                    "X2" -> ftH <= ftA
                    else -> false
                }
            }
            else -> false
        }
    }

    private fun poissonCdf(k: Int, lambda: Double): Double {
        var sum = 0.0
        var term = exp(-lambda)
        sum += term
        for (i in 1..k) {
            term *= (lambda / i)
            sum += term
        }
        return min(0.99, sum)
    }
}
