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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ModelRankStats
import com.example.ui.components.ModelAvatarBadge

@Composable
fun ModelRankingsScreen(
    rankings: List<ModelRankStats>,
    activeSimulatorModelId: String,
    onSelectForSimulator: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            // Podium Header
            LeaderboardPodiumHeader(rankings = rankings)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI MODEL LEADERBOARD",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Ranked best to worst (% correct)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(rankings, key = { it.persona.id }) { stats ->
            ModelRankCard(
                stats = stats,
                isSelectedInSimulator = stats.persona.id == activeSimulatorModelId,
                onSelectForSimulator = { onSelectForSimulator(stats.persona.id) }
            )
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun LeaderboardPodiumHeader(rankings: List<ModelRankStats>) {
    val top1 = rankings.getOrNull(0)
    val top2 = rankings.getOrNull(1)
    val top3 = rankings.getOrNull(2)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PREDICTION ACCURACY PODIUM",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFFB45309)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Podium pillars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // #2 Silver
                top2?.let { s ->
                    PodiumPillar(
                        stats = s,
                        medal = "🥈",
                        rankLabel = "#2",
                        pillarHeight = 90,
                        medalColor = Color(0xFF94A3B8)
                    )
                }

                // #1 Gold
                top1?.let { g ->
                    PodiumPillar(
                        stats = g,
                        medal = "👑",
                        rankLabel = "#1",
                        pillarHeight = 120,
                        medalColor = Color(0xFFF59E0B)
                    )
                }

                // #3 Bronze
                top3?.let { b ->
                    PodiumPillar(
                        stats = b,
                        medal = "🥉",
                        rankLabel = "#3",
                        pillarHeight = 75,
                        medalColor = Color(0xFFD97706)
                    )
                }
            }
        }
    }
}

@Composable
fun PodiumPillar(
    stats: ModelRankStats,
    medal: String,
    rankLabel: String,
    pillarHeight: Int,
    medalColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(96.dp)
    ) {
        Text(text = medal, fontSize = 22.sp)
        Spacer(modifier = Modifier.height(4.dp))
        ModelAvatarBadge(persona = stats.persona, size = 36)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stats.persona.name,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1
        )
        Text(
            text = "${String.format("%.1f", stats.accuracyPercentage)}%",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = medalColor
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(pillarHeight.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(medalColor.copy(alpha = 0.25f))
                .border(1.5.dp, medalColor, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rankLabel,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = medalColor
            )
        }
    }
}

@Composable
fun ModelRankCard(
    stats: ModelRankStats,
    isSelectedInSimulator: Boolean,
    onSelectForSimulator: () -> Unit
) {
    val rankBadgeColor = when (stats.rank) {
        1 -> Color(0xFFF59E0B) // Gold
        2 -> Color(0xFF94A3B8) // Silver
        3 -> Color(0xFFD97706) // Bronze
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Rank badge, Avatar, Title & Select Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Rank badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(rankBadgeColor.copy(alpha = 0.15f))
                            .border(1.5.dp, rankBadgeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${stats.rank}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = rankBadgeColor
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    ModelAvatarBadge(persona = stats.persona, size = 42)
                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stats.persona.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (stats.persona.isEnsemble) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "ENSEMBLE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = stats.persona.title,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isSelectedInSimulator) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Active Sim", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onSelectForSimulator,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Use in Sim", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Philosophy quote
            Text(
                text = "\"${stats.persona.philosophy}\"",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Accuracy Bar Gauge
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Prediction Accuracy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${String.format("%.1f", stats.accuracyPercentage)}% (${stats.correctPredictions}/${stats.totalPredictions} correct)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = stats.persona.accentColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (stats.accuracyPercentage / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = stats.persona.accentColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Financial & Bet Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Profit", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val isProfit = stats.totalProfit >= 0
                    Text(
                        text = "${if (isProfit) "+" else ""}$${String.format("%.2f", stats.totalProfit)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }

                Column {
                    Text("Bet Win Rate", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format("%.1f", stats.betWinRate)}% (${stats.wonBets}/${stats.totalBetsPlaced})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Column {
                    Text("ROI", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val isPosRoi = stats.roiPercentage >= 0
                    Text(
                        text = "${if (isPosRoi) "+" else ""}${String.format("%.1f", stats.roiPercentage)}%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isPosRoi) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Market Breakdown Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                stats.marketAccuracies.forEach { (market, acc) ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = market, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${acc.toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = stats.persona.accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
