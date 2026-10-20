package com.example.engine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Fun graphical representation of each prediction model.
 */
data class ModelPersona(
    val id: String,
    val name: String,
    val title: String,
    val tagLine: String,
    val avatarEmoji: String,
    val icon: ImageVector,
    val accentColor: Color,
    val description: String,
    val philosophy: String,
    val isEnsemble: Boolean = false
)

object ModelRegistry {
    val CONSENSUS = ModelPersona(
        id = "model_consensus",
        name = "Consensus Hive",
        title = "The Collective Hive Mind",
        tagLine = "Picks the most common selection across all models",
        avatarEmoji = "🎯",
        icon = Icons.Default.Hub,
        accentColor = Color(0xFFF59E0B), // Vibrant Gold
        description = "Aggregates the votes of all AI models for each market and places bets on the most common selection.",
        philosophy = "Democratic consensus beats individual bias every time.",
        isEnsemble = true
    )

    val PROFESSOR = ModelPersona(
        id = "model_professor",
        name = "The Professor",
        title = "Statistical Value Hunter",
        tagLine = "Poisson models & bookmaker margin stripping",
        avatarEmoji = "🧠",
        icon = Icons.Default.Psychology,
        accentColor = Color(0xFF8B5CF6), // Royal Violet
        description = "Calculates true mathematical expectations and exploits mispriced odds with disciplined Kelly-fraction logic.",
        philosophy = "Emotion is the enemy. Math is the only edge."
    )

    val FOREST = ModelPersona(
        id = "model_forest",
        name = "Forest Oracle",
        title = "Decision Tree Ensemble",
        tagLine = "Hundreds of branched trees voting on game flow",
        avatarEmoji = "🌲",
        icon = Icons.Default.Park,
        accentColor = Color(0xFF10B981), // Emerald Green
        description = "Traverses 100+ random decision trees examining non-linear matchups, home/away splits, and attack rhythms.",
        philosophy = "Deep rooted patterns reveal what simple eyes miss."
    )

    val TITAN = ModelPersona(
        id = "model_titan",
        name = "Gradient Titan",
        title = "Momentum Booster",
        tagLine = "High-voltage loss minimizer for goal bursts",
        avatarEmoji = "⚡",
        icon = Icons.Default.Bolt,
        accentColor = Color(0xFFEA580C), // Cyber Orange
        description = "Sequentially trains on residuals to pinpoint high-tempo blowouts, late surges, and BTTS explosions.",
        philosophy = "Strike hard when momentum diverges from odds."
    )

    val MARKET = ModelPersona(
        id = "model_market",
        name = "Market Maven",
        title = "Sharp Odds Specialist",
        tagLine = "Reverse-engineers bookmaker sharp consensus",
        avatarEmoji = "📊",
        icon = Icons.Default.BarChart,
        accentColor = Color(0xFF0284C7), // Sky Blue
        description = "Deconstructs bookmaker margins to deduce pure market-implied probability and sharp price deviations.",
        philosophy = "The market is usually smart; find where it is wrong."
    )

    val DEFENSE = ModelPersona(
        id = "model_defense",
        name = "Defense Guardian",
        title = "Under & Clean Sheet Shield",
        tagLine = "Under 2.5 and low-scoring fortress specialist",
        avatarEmoji = "🛡️",
        icon = Icons.Default.Shield,
        accentColor = Color(0xFF64748B), // Slate Grey
        description = "Analyzes defensive setups, low expected goals, and conservative game scripts to identify Under value.",
        philosophy = "Attack wins headlines, defense wins the bankroll."
    )

    val ALL_MODELS = listOf(CONSENSUS, PROFESSOR, FOREST, TITAN, MARKET, DEFENSE)
    val INDIVIDUAL_MODELS = listOf(PROFESSOR, FOREST, TITAN, MARKET, DEFENSE)

    fun getById(id: String): ModelPersona {
        return ALL_MODELS.find { it.id == id } ?: CONSENSUS
    }
}
