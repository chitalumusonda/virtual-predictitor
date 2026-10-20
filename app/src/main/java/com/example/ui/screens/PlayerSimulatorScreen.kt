package com.example.ui.screens

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaperBetEntity
import com.example.engine.ModelRegistry
import com.example.engine.ModelSimulationComparison
import com.example.engine.SimulationFinancialSummary
import com.example.ui.components.ModelAvatarBadge

@Composable
fun PlayerSimulatorScreen(
    financials: SimulationFinancialSummary,
    modelComparisons: List<ModelSimulationComparison>,
    bets: List<PaperBetEntity>,
    activeModelId: String,
    stakePerBet: Double,
    showComparison: Boolean,
    isBusy: Boolean,
    onModelSelected: (String) -> Unit,
    onStakeSelected: (Double) -> Unit,
    onToggleComparison: () -> Unit,
    onPlaceBets: () -> Unit,
    onPlaceAllModelBets: () -> Unit,
    onSimulateRound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePersona = ModelRegistry.getById(activeModelId)
    val filteredBets = if (activeModelId == "ALL") bets else bets.filter { it.modelId == activeModelId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Crystal-Clear Financial Header prioritizing clarity & simplicity
            HighClarityFinancialHeader(
                financials = financials,
                activeModelName = activePersona.name
            )
        }

        item {
            // Model Selector: Individual Models + Consolidated Consensus
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT SIMULATOR MODEL STRATEGY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onToggleComparison() },
                        color = if (showComparison) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CompareArrows,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showComparison) "Hide Comparison" else "Compare All Models",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ModelRegistry.ALL_MODELS) { model ->
                        val isSelected = model.id == activeModelId
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) model.accentColor else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onModelSelected(model.id) },
                            color = if (isSelected) model.accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ModelAvatarBadge(persona = model, size = 28)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = model.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) model.accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (model.isEnsemble) "Most common pick" else model.title,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Optional Side-by-Side Multi-Model Comparison Table
        if (showComparison) {
            item {
                MultiModelComparisonCard(comparisons = modelComparisons)
            }
        }

        item {
            // Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STAKE PER BET",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(10.0, 25.0, 50.0, 100.0).forEach { stake ->
                                val isSelected = stakePerBet == stake
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onStakeSelected(stake) },
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = "$${stake.toInt()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPlaceBets,
                            modifier = Modifier.weight(1f),
                            enabled = !isBusy
                        ) {
                            Text("Bet with ${activePersona.name}", fontSize = 11.sp, maxLines = 1)
                        }

                        Button(
                            onClick = onSimulateRound,
                            modifier = Modifier.weight(1f),
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = activePersona.accentColor)
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Play & Settle", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = onPlaceAllModelBets,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBusy
                    ) {
                        Text("Simulate All Models on Upcoming Round (Side-by-Side)", fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SIMULATED GAMES FINANCIAL LOG",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${filteredBets.size} bets • ${activePersona.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (filteredBets.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Casino,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No bets simulated yet for ${activePersona.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Bet with ${activePersona.name}' to simulate bets on upcoming games.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredBets, key = { it.id }) { bet ->
                HighClarityBetCard(bet = bet)
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

/**
 * Top Financial Dashboard prioritizing maximum clarity:
 * Explicitly separates Total Betted, Total Profit from wins, Total Loss from losses,
 * and Net Financial Outcome.
 */
@Composable
fun HighClarityFinancialHeader(
    financials: SimulationFinancialSummary,
    activeModelName: String
) {
    val isNetProfit = financials.netProfitOrLoss >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Active Model and Current Bankroll
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FINANCIAL SUMMARY: $activeModelName",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Initial Bankroll: $${String.format("%.0f", financials.initialBankroll)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Current Bankroll Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CURRENT BANKROLL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format("%.2f", financials.currentBankroll)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isNetProfit) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFFEF4444).copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isNetProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = if (isNetProfit) Color(0xFF047857) else Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (isNetProfit) "+" else ""}${String.format("%.1f", financials.roiPercentage)}% ROI",
                            color = if (isNetProfit) Color(0xFF047857) else Color(0xFFDC2626),
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Ultra-Clear Financial Metric Cards (Total Betted, Total Profit, Total Loss)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Betted Card
                FinancialMetricBox(
                    title = "TOTAL BETTED",
                    amount = "$${String.format("%.2f", financials.totalBetted)}",
                    subtext = "${financials.totalBetsCount} bets placed",
                    color = MaterialTheme.colorScheme.onSurface,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f)
                )

                // Total Profit (from winning bets)
                FinancialMetricBox(
                    title = "TOTAL PROFIT",
                    amount = "+$${String.format("%.2f", financials.totalWonProfit)}",
                    subtext = "${financials.wonBetsCount} wins",
                    color = Color(0xFF059669),
                    containerColor = Color(0xFF10B981).copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                )

                // Total Loss (from losing bets)
                FinancialMetricBox(
                    title = "TOTAL LOSS",
                    amount = "-$${String.format("%.2f", financials.totalLossAmount)}",
                    subtext = "${financials.lostBetsCount} losses",
                    color = Color(0xFFDC2626),
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // NET OUTCOME STRIP (The Ultimate Financial Bottom Line)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isNetProfit) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NET FINANCIAL OUTCOME",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = if (isNetProfit) Color(0xFF047857) else Color(0xFFB91C1C)
                    )
                    Text(
                        text = "${if (isNetProfit) "+$" else "-$"}${String.format("%.2f", kotlin.math.abs(financials.netProfitOrLoss))} ${if (isNetProfit) "NET PROFIT" else "NET LOSS"}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (isNetProfit) Color(0xFF059669) else Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialMetricBox(
    title: String,
    amount: String,
    subtext: String,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Multi-Model Side-by-Side Comparison Matrix
 */
@Composable
fun MultiModelComparisonCard(comparisons: List<ModelSimulationComparison>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "ALL MODELS PERFORMANCE COMPARISON",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            comparisons.forEach { item ->
                val isProfit = item.summary.netProfitOrLoss >= 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1.5f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModelAvatarBadge(persona = item.model, size = 22)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.model.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Text(
                        text = "Bet: $${String.format("%.0f", item.summary.totalBetted)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${if (isProfit) "+" else ""}$${String.format("%.2f", item.summary.netProfitOrLoss)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${String.format("%.0f", item.summary.winRatePercentage)}% Win",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Individual Simulated Game Card designed for instant visual clarity on:
 * - Amount Betted
 * - Outcome (Score, WON/LOST)
 * - Net Profit or Loss
 * - Running Bankroll
 */
@Composable
fun HighClarityBetCard(bet: PaperBetEntity) {
    val isWon = bet.status == "WON"
    val isPending = bet.status == "PENDING"
    val modelPersona = ModelRegistry.getById(bet.modelId)

    val cardBorder = when {
        isWon -> Color(0xFF10B981)
        isPending -> MaterialTheme.colorScheme.outlineVariant
        else -> Color(0xFFEF4444).copy(alpha = 0.6f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Fixture and Model Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bet.fixture,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Round ${bet.roundId}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ModelAvatarBadge(persona = modelPersona, size = 26)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = modelPersona.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bet Selection & Market
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = modelPersona.accentColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = bet.market,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = modelPersona.accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = bet.selection,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = "Odds: ${String.format("%.2f", bet.odds)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ULTRA-CLEAR FINANCIAL BREAKDOWN ROW
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = when {
                    isWon -> Color(0xFF10B981).copy(alpha = 0.12f)
                    isPending -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    else -> Color(0xFFEF4444).copy(alpha = 0.10f)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Amount Betted
                    Column {
                        Text(
                            text = "AMOUNT BETTED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$${String.format("%.2f", bet.stake)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    // Score Outcome
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MATCH RESULT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${bet.ftScore ?: "IN PROGRESS"} • ${bet.status}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isWon) Color(0xFF047857) else if (isPending) MaterialTheme.colorScheme.primary else Color(0xFFDC2626)
                        )
                    }

                    // Profit or Loss
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isWon) "PROFIT" else if (isPending) "POTENTIAL" else "LOSS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isWon) Color(0xFF047857) else if (isPending) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFFDC2626)
                        )
                        val profitLossText = when {
                            isPending -> "+$${String.format("%.2f", (bet.stake * bet.odds) - bet.stake)}"
                            isWon -> "+$${String.format("%.2f", bet.netProfit)}"
                            else -> "-$${String.format("%.2f", kotlin.math.abs(bet.netProfit))}"
                        }
                        Text(
                            text = profitLossText,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = when {
                                isPending -> MaterialTheme.colorScheme.onSurfaceVariant
                                isWon -> Color(0xFF10B981)
                                else -> Color(0xFFEF4444)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Footer with bankroll
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bankroll After: $${String.format("%.2f", bet.bankrollAfter)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isWon) "Payout: $${String.format("%.2f", bet.grossPayout)}" else if (isPending) "Awaiting Outcome" else "No Payout",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isWon) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
