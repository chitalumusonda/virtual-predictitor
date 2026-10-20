package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.engine.ModelPersona

@Composable
fun ModelAvatarBadge(
    persona: ModelPersona,
    modifier: Modifier = Modifier,
    size: Int = 40
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(persona.accentColor.copy(alpha = 0.2f))
            .border(1.5.dp, persona.accentColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = persona.avatarEmoji,
            fontSize = (size * 0.55).sp
        )
    }
}

@Composable
fun OddsPill(
    label: String,
    odds: Double,
    modifier: Modifier = Modifier,
    isWinning: Boolean? = null,
    isSelected: Boolean = false
) {
    val bgColor = when {
        isWinning == true -> Color(0xFF10B981).copy(alpha = 0.15f)
        isWinning == false -> Color(0xFFEF4444).copy(alpha = 0.10f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }

    val borderColor = when {
        isWinning == true -> Color(0xFF10B981)
        isWinning == false -> Color(0xFFEF4444).copy(alpha = 0.5f)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = String.format("%.2f", odds),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isWinning == true) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun WinLossBadge(
    status: String,
    profit: Double,
    modifier: Modifier = Modifier
) {
    val isWon = status == "WON"
    val isPending = status == "PENDING"

    val (bgColor, textColor, label) = when {
        isPending -> Triple(Color(0xFF64748B).copy(alpha = 0.15f), Color(0xFF64748B), "PENDING")
        isWon -> Triple(Color(0xFF10B981).copy(alpha = 0.2f), Color(0xFF059669), "+$${String.format("%.2f", profit)} WON")
        else -> Triple(Color(0xFFEF4444).copy(alpha = 0.2f), Color(0xFFDC2626), "-$${String.format("%.2f", kotlin.math.abs(profit))} LOST")
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(6.dp)),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isWon) Icons.Default.CheckCircle else Icons.Default.Close,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ConnectionStatusCard(
    status: String,
    savedStartPointRoundId: String,
    savedStartPointMatchday: Int,
    savedUtc: String,
    modifier: Modifier = Modifier
) {
    val isConnected = status == "CONNECTED"
    val isResuming = status == "RESUMING"

    val containerColor = if (isConnected) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
    val contentColor = if (isConnected) Color(0xFF047857) else Color(0xFFB45309)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isConnected) Icons.Default.SignalCellularAlt else Icons.Default.SignalCellularConnectedNoInternet0Bar,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isConnected) "Live Collector Connected" else if (isResuming) "Resuming Collection..." else "Connection Interrupted",
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                }
                Text(
                    text = "Saved Start Point: Matchday $savedStartPointMatchday ($savedStartPointRoundId)",
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.9f)
                )
                if (savedUtc.isNotEmpty()) {
                    Text(
                        text = "Checkpoint: $savedUtc",
                        fontSize = 10.sp,
                        color = contentColor.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

@Composable
fun MarketFilterChips(
    selectedMarket: String,
    onMarketSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val markets = listOf("ALL", "1X2", "O/U 2.5", "BTTS", "DC")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        markets.forEach { market ->
            val isSelected = selectedMarket == market
            FilterChip(
                selected = isSelected,
                onClick = { onMarketSelected(market) },
                label = { Text(market, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun TeamBadge(teamName: String, isHome: Boolean) {
    val initials = teamName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
    val bgColor = if (isHome) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (isHome) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = textColor
        )
    }
}
