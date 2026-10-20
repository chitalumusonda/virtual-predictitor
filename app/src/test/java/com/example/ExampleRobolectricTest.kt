package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PaperBetEntity
import com.example.data.model.UnifiedFixtureRecord
import com.example.engine.ModelRankingEngine
import com.example.engine.ModelRegistry
import com.example.engine.PlayerSimulatorEngine
import com.example.engine.PredictionEngine
import com.example.engine.VirtualLeagueGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("VirtualBet AI", appName)
    }

    @Test
    fun `prediction engine generates multi-market predictions including consensus`() {
        val fixture = UnifiedFixtureRecord(
            fixtureId = "test-fix-1",
            roundId = "RND-1",
            matchday = 1,
            seasonId = "season-1",
            competitionId = "1001",
            competitionName = "English Virtual",
            fixture = "London Blue - Manchester Red",
            homeTeam = "London Blue",
            awayTeam = "Manchester Red",
            kickoffUtc = "2026-10-20T12:00:00Z",
            capturedUtc = "2026-10-20T11:55:00Z",
            oddsHome = 2.10,
            oddsDraw = 3.20,
            oddsAway = 3.40,
            oddsOver25 = 1.85,
            oddsUnder25 = 1.95,
            oddsOver15 = 1.25,
            oddsUnder15 = 3.50,
            oddsBttsYes = 1.75,
            oddsBttsNo = 2.05,
            oddsDc1X = 1.30,
            oddsDc12 = 1.35,
            oddsDcX2 = 1.65,
            status = "UPCOMING"
        )

        val predictions = PredictionEngine.generatePredictionsForFixture(fixture)
        assertTrue(predictions.isNotEmpty())

        // Verify Consensus Hive pick exists for multi markets
        val consensusOU = predictions.find { it.modelId == ModelRegistry.CONSENSUS.id && it.market == "O/U 2.5" }
        assertNotNull(consensusOU)

        val consensus1X2 = predictions.find { it.modelId == ModelRegistry.CONSENSUS.id && it.market == "1X2" }
        assertNotNull(consensus1X2)

        val consensusBTTS = predictions.find { it.modelId == ModelRegistry.CONSENSUS.id && it.market == "BTTS" }
        assertNotNull(consensusBTTS)

        // Simulate match result: FT 2-1 (Total goals 3, BTTS Yes, Home win)
        val finalRecord = fixture.copy(
            status = "FINAL",
            ftHome = 2,
            ftAway = 1,
            outcome = "1",
            totalGoals = 3,
            bttsResult = "Yes",
            ou25Result = "Over"
        )

        val evaluated1X2 = PredictionEngine.evaluatePredictionOutcome(consensus1X2!!, finalRecord)
        assertNotNull(evaluated1X2)
    }

    @Test
    fun `player simulator correctly calculates profit and loss`() {
        val winningBet = PaperBetEntity(
            fixtureId = "fix-1",
            roundId = "RND-1",
            fixture = "London Blue - Manchester Red",
            modelId = ModelRegistry.CONSENSUS.id,
            market = "O/U 2.5",
            selection = "Over 2.5",
            odds = 2.0,
            stake = 10.0,
            confidence = 0.8,
            placedUtc = "2026-10-20T12:00:00Z"
        )

        val finalFixture = UnifiedFixtureRecord(
            fixtureId = "fix-1",
            roundId = "RND-1",
            matchday = 1,
            seasonId = "season-1",
            competitionId = "1001",
            competitionName = "English Virtual",
            fixture = "London Blue - Manchester Red",
            homeTeam = "London Blue",
            awayTeam = "Manchester Red",
            kickoffUtc = "2026-10-20T12:00:00Z",
            capturedUtc = "2026-10-20T11:55:00Z",
            status = "FINAL",
            ftHome = 2,
            ftAway = 1
        )

        val settledWinning = PlayerSimulatorEngine.settleBet(winningBet, finalFixture, 1000.0)
        assertEquals("WON", settledWinning.status)
        assertEquals(20.0, settledWinning.grossPayout, 0.001)
        assertEquals(10.0, settledWinning.netProfit, 0.001)

        val financials = PlayerSimulatorEngine.calculateFinancials(1000.0, listOf(settledWinning))
        assertEquals(1010.0, financials.currentBankroll, 0.001)
        assertEquals(10.0, financials.totalBetted, 0.001)
        assertEquals(10.0, financials.totalWonProfit, 0.001)
        assertEquals(0.0, financials.totalLossAmount, 0.001)
        assertEquals(10.0, financials.netProfitOrLoss, 0.001)
        assertEquals(100.0, financials.roiPercentage, 0.001)

        val comparisons = PlayerSimulatorEngine.compareAllModels(1000.0, listOf(settledWinning))
        assertTrue(comparisons.isNotEmpty())
    }
}
