package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UnifiedFixtureRecord
import com.example.ui.components.TeamBadge

@Composable
fun UnifiedRecordsScreen(
    fixtures: List<UnifiedFixtureRecord>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "FINAL", "UPCOMING"

    val filteredFixtures = fixtures.filter { f ->
        val matchesStatus = when (statusFilter) {
            "FINAL" -> f.status == "FINAL"
            "UPCOMING" -> f.status == "UPCOMING"
            else -> true
        }
        val matchesSearch = searchQuery.isEmpty() ||
                f.homeTeam.contains(searchQuery, ignoreCase = true) ||
                f.awayTeam.contains(searchQuery, ignoreCase = true) ||
                f.competitionName.contains(searchQuery, ignoreCase = true)
        matchesStatus && matchesSearch
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "UNIFIED ODDS & RESULTS RECORDS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Each record stores the pre-game odds and the subsequent match result together in the same row.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search by team or league...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", "FINAL", "UPCOMING").forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { statusFilter = status },
                        label = { Text(status) }
                    )
                }
            }
        }

        items(filteredFixtures, key = { it.fixtureId }) { fixture ->
            UnifiedFixtureCard(fixture = fixture)
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun UnifiedFixtureCard(fixture: UnifiedFixtureRecord) {
    val isFinal = fixture.status == "FINAL"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: League, Round & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${fixture.competitionName} • Matchday ${fixture.matchday}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isFinal) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = fixture.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFinal) Color(0xFF047857) else Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Match Scores Block (Home - Away)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Team
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamBadge(teamName = fixture.homeTeam, isHome = true)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = fixture.homeTeam,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Score pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFinal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isFinal) "${fixture.ftHome} - ${fixture.ftAway}" else "VS",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = if (isFinal) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isFinal && fixture.htHome != null) {
                            Text(
                                text = "HT: ${fixture.htHome}-${fixture.htAway}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Away Team
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = fixture.awayTeam,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TeamBadge(teamName = fixture.awayTeam, isHome = false)
                }
            }

            if (isFinal) {
                Spacer(modifier = Modifier.height(8.dp))
                // Quick Result tags
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ResultPill(label = "1X2: ${fixture.outcome ?: "-"}")
                    ResultPill(label = "Goals: ${fixture.totalGoals ?: 0}")
                    ResultPill(label = "BTTS: ${fixture.bttsResult ?: "-"}")
                    ResultPill(label = "O/U 2.5: ${fixture.ou25Result ?: "-"}")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // COMBINED PRE-MATCH ODDS & SUBSEQUENT OUTCOMES TABLE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(10.dp)
            ) {
                Text(
                    text = "PRE-MATCH ODDS & CORRESPONDING OUTCOMES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                // 1X2 Row
                OddsOutcomeRow(
                    market = "1X2",
                    selections = listOf(
                        Triple("1", fixture.oddsHome, isFinal && fixture.outcome == "1"),
                        Triple("X", fixture.oddsDraw, isFinal && fixture.outcome == "X"),
                        Triple("2", fixture.oddsAway, isFinal && fixture.outcome == "2")
                    ),
                    isFinal = isFinal
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Over / Under 2.5 Row
                OddsOutcomeRow(
                    market = "O/U 2.5",
                    selections = listOf(
                        Triple("Over 2.5", fixture.oddsOver25, isFinal && (fixture.totalGoals ?: 0) >= 3),
                        Triple("Under 2.5", fixture.oddsUnder25, isFinal && (fixture.totalGoals ?: 0) < 3)
                    ),
                    isFinal = isFinal
                )

                Spacer(modifier = Modifier.height(6.dp))

                // BTTS Row
                OddsOutcomeRow(
                    market = "BTTS",
                    selections = listOf(
                        Triple("Yes", fixture.oddsBttsYes, isFinal && fixture.bttsResult == "Yes"),
                        Triple("No", fixture.oddsBttsNo, isFinal && fixture.bttsResult == "No")
                    ),
                    isFinal = isFinal
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // EXPLICIT RECORD LINKAGE AUDIT STRIP
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LINKED RECORD AUDIT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Odds Locked: ${fixture.capturedUtc.takeLast(13).replace("Z", " UTC")}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isFinal) "Subsequent Score Linked" else "Awaiting Match Outcome",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFinal) Color(0xFF047857) else Color(0xFFB45309)
                        )
                        Text(
                            text = if (isFinal) "Settled: ${fixture.updatedUtc.takeLast(13).replace("Z", " UTC")}" else "Linked to Round ${fixture.roundId}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultPill(label: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun OddsOutcomeRow(
    market: String,
    selections: List<Triple<String, Double, Boolean>>,
    isFinal: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = market,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            selections.forEach { (label, odds, won) ->
                val bg = when {
                    !isFinal -> MaterialTheme.colorScheme.surface
                    won -> Color(0xFF10B981).copy(alpha = 0.2f)
                    else -> Color(0xFFEF4444).copy(alpha = 0.10f)
                }
                val border = when {
                    !isFinal -> Color.Transparent
                    won -> Color(0xFF10B981)
                    else -> Color(0xFFEF4444).copy(alpha = 0.3f)
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, border, RoundedCornerShape(6.dp)),
                    color = bg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = String.format("%.2f", odds),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (won) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface
                        )
                        if (isFinal) {
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = if (won) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (won) Color(0xFF047857) else Color(0xFFDC2626),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
