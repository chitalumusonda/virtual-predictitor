package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.PredictionEntity
import com.example.data.model.UnifiedFixtureRecord
import com.example.engine.ModelPersona
import com.example.engine.ModelRegistry
import com.example.ui.VirtualBetUiState
import com.example.ui.components.MarketFilterChips
import com.example.ui.components.ModelAvatarBadge
import com.example.ui.components.OddsPill
import com.example.ui.components.TeamBadge

@Composable
fun PreMatchBoardScreen(
    upcomingFixtures: List<UnifiedFixtureRecord>,
    allPredictions: List<PredictionEntity>,
    uiState: VirtualBetUiState,
    onMarketSelected: (String) -> Unit,
    onSimulateRound: () -> Unit,
    onPlaceBets: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Round Information & Action Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "UPCOMING GAME PREDICTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Matchday ${upcomingFixtures.firstOrNull()?.matchday ?: 24} Board",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${upcomingFixtures.size} upcoming matches with full model forecasts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onPlaceBets,
                            enabled = !uiState.isBusy && upcomingFixtures.isNotEmpty()
                        ) {
                            Text("Bet All", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onSimulateRound,
                            enabled = !uiState.isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (uiState.isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play Round", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Graphical Model Showcase Strip (Differentiates each model visually)
        GraphicalModelShowcaseBar()

        // Market Filter Chips
        MarketFilterChips(
            selectedMarket = uiState.selectedMarketFilter,
            onMarketSelected = onMarketSelected
        )

        // Fixtures List
        if (upcomingFixtures.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SportsSoccer,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No upcoming games in queue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap 'Play Round' to advance and collect the next virtual round.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onSimulateRound) {
                        Text("Collect Next Round")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(upcomingFixtures, key = { it.fixtureId }) { fixture ->
                    val fixturePreds = allPredictions.filter { it.fixtureId == fixture.fixtureId }
                    GraphicalGamePredictionCard(
                        fixture = fixture,
                        predictions = fixturePreds,
                        marketFilter = uiState.selectedMarketFilter
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

/**
 * Visual Carousel illustrating each prediction model graphically with icons, colors, and philosophies.
 */
@Composable
fun GraphicalModelShowcaseBar() {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ModelRegistry.ALL_MODELS) { model ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, model.accentColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                    color = model.accentColor.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModelAvatarBadge(persona = model, size = 26)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = model.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = model.accentColor
                            )
                            Text(
                                text = if (model.isEnsemble) "Majority Pick" else model.title,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GraphicalGamePredictionCard(
    fixture: UnifiedFixtureRecord,
    predictions: List<PredictionEntity>,
    marketFilter: String
) {
    var expanded by remember { mutableStateOf(true) }

    // Target market for quick comparison
    val targetMarket = if (marketFilter == "ALL") "O/U 2.5" else marketFilter
    val consensusPred = predictions.find {
        it.modelId == ModelRegistry.CONSENSUS.id && (if (marketFilter == "ALL") it.market == "O/U 2.5" || it.market == "1X2" else it.market == marketFilter)
    } ?: predictions.find { it.modelId == ModelRegistry.CONSENSUS.id }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: League & Status
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
                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "PRE-KICKOFF",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Teams Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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

                Text(
                    text = "vs",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

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

            Spacer(modifier = Modifier.height(10.dp))

            // Odds Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OddsPill(label = "1", odds = fixture.oddsHome, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(4.dp))
                    OddsPill(label = "X", odds = fixture.oddsDraw, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(4.dp))
                    OddsPill(label = "2", odds = fixture.oddsAway, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OddsPill(label = "Over 2.5", odds = fixture.oddsOver25, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(4.dp))
                    OddsPill(label = "Under 2.5", odds = fixture.oddsUnder25, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(4.dp))
                    OddsPill(label = "BTTS Yes", odds = fixture.oddsBttsYes, modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // CONSOLIDATED CONSENSUS MASTER FORECAST (Gold Hero Card)
            consensusPred?.let { cp ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp)),
                    color = Color(0xFFF59E0B).copy(alpha = 0.12f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🎯", fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "CONSOLIDATED CONSENSUS (MOST COMMON PICK)",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = Color(0xFFB45309)
                                    )
                                    Text(
                                        text = "${cp.predictedSelection} @ ${String.format("%.2f", cp.oddsAtPrediction)}",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF59E0B)
                            ) {
                                Text(
                                    text = "${(cp.confidence * 100).toInt()}% Conf",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { cp.confidence.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFF59E0B),
                            trackColor = Color(0xFFF59E0B).copy(alpha = 0.25f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expand / Collapse Toggle for Individual Models
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "Hide all model forecasts" else "View forecasts from all 5 individual models",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Graphical Breakdown of Individual Models with Custom Avatars & Confidence Bars
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val individualPreds = predictions.filter { it.modelId != ModelRegistry.CONSENSUS.id }
                    val groupedByModel = individualPreds.groupBy { it.modelId }

                    for (model in ModelRegistry.INDIVIDUAL_MODELS) {
                        val mPreds = groupedByModel[model.id] ?: emptyList()
                        val topPred = mPreds.find {
                            if (marketFilter == "ALL") it.market == "O/U 2.5" || it.market == "1X2" else it.market == marketFilter
                        } ?: mPreds.firstOrNull()

                        if (topPred != null) {
                            GraphicalModelPredictionTile(
                                model = model,
                                prediction = topPred
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Graphical representation of a model's prediction with its mascot avatar,
 * accent color outline, selection badge, and confidence gauge chart.
 */
@Composable
fun GraphicalModelPredictionTile(
    model: ModelPersona,
    prediction: PredictionEntity
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, model.accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
        color = model.accentColor.copy(alpha = 0.05f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModelAvatarBadge(persona = model, size = 24)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = model.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = model.accentColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "• ${prediction.market}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${prediction.predictedSelection} @ ${String.format("%.2f", prediction.oddsAtPrediction)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Graphical Confidence Meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { prediction.confidence.toFloat() },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = model.accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "${(prediction.confidence * 100).toInt()}% Confidence",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
