package com.example.engine

import com.example.data.model.UnifiedFixtureRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random
import java.util.TimeZone
import java.util.UUID

/**
 * Generates realistic BetPawa Virtual League matches with genuine odds distributions
 * across 1X2, Over/Under, BTTS, and Double Chance.
 * Supports both pre-match odds creation and subsequent match score resolution.
 */
object VirtualLeagueGenerator {

    private val random = Random()

    private val TEAMS = listOf(
        "London Blue" to 1.35,
        "Manchester Red" to 1.25,
        "Liverpool Red" to 1.40,
        "Manchester Blue" to 1.50,
        "London Gunner" to 1.30,
        "Newcastle B&W" to 1.15,
        "Tottenham White" to 1.18,
        "Aston Claret" to 1.12,
        "Brighton Seagull" to 1.05,
        "West Ham Iron" to 1.02,
        "Wolverhampton Gold" to 0.95,
        "Everton Blue" to 0.92,
        "Fulham White" to 0.90,
        "Brentford Bee" to 0.93,
        "Crystal Eagle" to 0.88,
        "Nottingham Forest" to 0.85
    )

    private val COMPETITIONS = listOf(
        "1001" to "English Virtual League",
        "1002" to "Spanish Virtual League",
        "1003" to "Italian Virtual League",
        "1004" to "German Virtual League"
    )

    private fun currentUtcString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    /**
     * Generates a new round of upcoming fixtures with pre-match odds for all markets.
     */
    fun generateUpcomingRound(
        seasonId: String,
        matchday: Int,
        roundId: String = "RND-${matchday}-${System.currentTimeMillis() % 100000}"
    ): List<UnifiedFixtureRecord> {
        val fixtures = mutableListOf<UnifiedFixtureRecord>()
        val capturedUtc = currentUtcString()
        val shuffledTeams = TEAMS.shuffled(random)

        val comp = COMPETITIONS[(matchday - 1) % COMPETITIONS.size]
        val compId = comp.first
        val compName = comp.second

        for (i in 0 until shuffledTeams.size / 2) {
            val home = shuffledTeams[i * 2]
            val away = shuffledTeams[i * 2 + 1]

            val homeRating = home.second * 1.12 // Home ground advantage
            val awayRating = away.second
            val totalRating = homeRating + awayRating

            // Implied probabilities with 6% bookmaker margin
            val margin = 1.06
            val probHome = (homeRating / totalRating) * 0.72
            val probAway = (awayRating / totalRating) * 0.65
            val probDraw = 1.0 - probHome - probAway

            val oddsHome = ((margin / probHome) * 100).toInt() / 100.0
            val oddsDraw = ((margin / probDraw) * 100).toInt() / 100.0
            val oddsAway = ((margin / probAway) * 100).toInt() / 100.0

            // Over / Under 2.5
            val expGoals = (homeRating * 1.2) + (awayRating * 1.0)
            val probOver25 = if (expGoals > 2.5) 0.58 else 0.44
            val oddsOver25 = ((margin / probOver25) * 100).toInt() / 100.0
            val oddsUnder25 = ((margin / (1.0 - probOver25)) * 100).toInt() / 100.0

            // Over / Under 1.5
            val probOver15 = 0.76
            val oddsOver15 = ((margin / probOver15) * 100).toInt() / 100.0
            val oddsUnder15 = ((margin / (1.0 - probOver15)) * 100).toInt() / 100.0

            // BTTS
            val probBtts = if (expGoals > 2.4) 0.56 else 0.46
            val oddsBttsYes = ((margin / probBtts) * 100).toInt() / 100.0
            val oddsBttsNo = ((margin / (1.0 - probBtts)) * 100).toInt() / 100.0

            // Double Chance
            val oddsDc1X = ((margin / (probHome + probDraw)) * 100).toInt() / 100.0
            val oddsDc12 = ((margin / (probHome + probAway)) * 100).toInt() / 100.0
            val oddsDcX2 = ((margin / (probDraw + probAway)) * 100).toInt() / 100.0

            val fixtureId = "FIX-${roundId}-${i + 1}"

            fixtures.add(
                UnifiedFixtureRecord(
                    fixtureId = fixtureId,
                    roundId = roundId,
                    matchday = matchday,
                    seasonId = seasonId,
                    competitionId = compId,
                    competitionName = compName,
                    fixture = "${home.first} - ${away.first}",
                    homeTeam = home.first,
                    awayTeam = away.first,
                    kickoffUtc = capturedUtc,
                    capturedUtc = capturedUtc,
                    oddsHome = oddsHome,
                    oddsDraw = oddsDraw,
                    oddsAway = oddsAway,
                    oddsOver25 = oddsOver25,
                    oddsUnder25 = oddsUnder25,
                    oddsOver15 = oddsOver15,
                    oddsUnder15 = oddsUnder15,
                    oddsBttsYes = oddsBttsYes,
                    oddsBttsNo = oddsBttsNo,
                    oddsDc1X = oddsDc1X,
                    oddsDc12 = oddsDc12,
                    oddsDcX2 = oddsDcX2,
                    status = "UPCOMING"
                )
            )
        }

        return fixtures
    }

    /**
     * Resolves match scores and updates subsequent results into the exact same record.
     */
    fun simulateMatchResults(record: UnifiedFixtureRecord): UnifiedFixtureRecord {
        val updatedUtc = currentUtcString()

        // Probability of goals based on odds
        val homeAdvantage = if (record.oddsHome < record.oddsAway) 1 else 0
        val awayAdvantage = if (record.oddsAway < record.oddsHome) 1 else 0

        // Realistic score distribution
        val ftHome = maxOf(0, random.nextInt(3) + homeAdvantage + if (random.nextDouble() > 0.8) 1 else 0)
        val ftAway = maxOf(0, random.nextInt(3) + awayAdvantage + if (random.nextDouble() > 0.85) 1 else 0)

        val htHome = if (ftHome > 0) random.nextInt(ftHome + 1) else 0
        val htAway = if (ftAway > 0) random.nextInt(ftAway + 1) else 0

        val totalGoals = ftHome + ftAway
        val outcome = when {
            ftHome > ftAway -> "1"
            ftHome < ftAway -> "2"
            else -> "X"
        }

        val bttsResult = if (ftHome > 0 && ftAway > 0) "Yes" else "No"
        val ou25Result = if (totalGoals >= 3) "Over" else "Under"
        val ou15Result = if (totalGoals >= 2) "Over" else "Under"
        val dcResult = when {
            ftHome > ftAway -> "1X, 12"
            ftHome < ftAway -> "12, X2"
            else -> "1X, X2"
        }

        return record.copy(
            status = "FINAL",
            ftHome = ftHome,
            ftAway = ftAway,
            htHome = htHome,
            htAway = htAway,
            outcome = outcome,
            totalGoals = totalGoals,
            bttsResult = bttsResult,
            ou25Result = ou25Result,
            ou15Result = ou15Result,
            dcResult = dcResult,
            updatedUtc = updatedUtc
        )
    }
}
