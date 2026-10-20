package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DataThresholding
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.DataThresholding
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppScreen
import com.example.ui.VirtualBetUiState
import com.example.ui.VirtualBetViewModel
import com.example.ui.screens.CollectorScreen
import com.example.ui.screens.ModelRankingsScreen
import com.example.ui.screens.PlayerSimulatorScreen
import com.example.ui.screens.PreMatchBoardScreen
import com.example.ui.screens.UnifiedRecordsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: VirtualBetViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val upcomingFixtures by viewModel.upcomingFixtures.collectAsStateWithLifecycle()
                val allFixtures by viewModel.allFixtures.collectAsStateWithLifecycle()
                val checkpoint by viewModel.checkpoint.collectAsStateWithLifecycle()
                val allPredictions by viewModel.allPredictions.collectAsStateWithLifecycle()
                val allBets by viewModel.allBets.collectAsStateWithLifecycle()
                val modelRankings by viewModel.modelRankings.collectAsStateWithLifecycle()
                val simulationFinancials by viewModel.simulationFinancials.collectAsStateWithLifecycle()
                val modelComparisons by viewModel.modelComparisons.collectAsStateWithLifecycle()

                // Support Android System Back to return to Pre-Match home screen
                BackHandler(enabled = uiState.currentScreen != AppScreen.PRE_MATCH) {
                    viewModel.setScreen(AppScreen.PRE_MATCH)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "VirtualBet",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Text(
                                            text = "AI",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            actions = {
                                // Status indicator pulse & quick simulate button
                                val isConnected = checkpoint?.connectionStatus == "CONNECTED"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { viewModel.processNextRound(forceVirtual = true) },
                                        enabled = !uiState.isBusy
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Advance round",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.PRE_MATCH,
                                onClick = { viewModel.setScreen(AppScreen.PRE_MATCH) },
                                icon = {
                                    Icon(
                                        if (uiState.currentScreen == AppScreen.PRE_MATCH) Icons.Default.SportsSoccer else Icons.Outlined.SportsSoccer,
                                        contentDescription = "Pre-Match"
                                    )
                                },
                                label = { Text("Pre-Match", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )

                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.RANKINGS,
                                onClick = { viewModel.setScreen(AppScreen.RANKINGS) },
                                icon = {
                                    Icon(
                                        if (uiState.currentScreen == AppScreen.RANKINGS) Icons.Default.EmojiEvents else Icons.Outlined.EmojiEvents,
                                        contentDescription = "Rankings"
                                    )
                                },
                                label = { Text("Rankings", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )

                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.SIMULATOR,
                                onClick = { viewModel.setScreen(AppScreen.SIMULATOR) },
                                icon = {
                                    Icon(
                                        if (uiState.currentScreen == AppScreen.SIMULATOR) Icons.Default.Casino else Icons.Outlined.Casino,
                                        contentDescription = "Simulator"
                                    )
                                },
                                label = { Text("Simulator", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )

                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.RECORDS,
                                onClick = { viewModel.setScreen(AppScreen.RECORDS) },
                                icon = {
                                    Icon(
                                        if (uiState.currentScreen == AppScreen.RECORDS) Icons.Default.DataThresholding else Icons.Outlined.DataThresholding,
                                        contentDescription = "Records"
                                    )
                                },
                                label = { Text("Records", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )

                            NavigationBarItem(
                                selected = uiState.currentScreen == AppScreen.COLLECTOR,
                                onClick = { viewModel.setScreen(AppScreen.COLLECTOR) },
                                icon = {
                                    Icon(
                                        if (uiState.currentScreen == AppScreen.COLLECTOR) Icons.Default.Sync else Icons.Outlined.Sync,
                                        contentDescription = "Collector"
                                    )
                                },
                                label = { Text("Collector", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (uiState.currentScreen) {
                            AppScreen.PRE_MATCH -> {
                                PreMatchBoardScreen(
                                    upcomingFixtures = upcomingFixtures,
                                    allPredictions = allPredictions,
                                    uiState = uiState,
                                    onMarketSelected = { viewModel.setMarketFilter(it) },
                                    onSimulateRound = { viewModel.processNextRound(forceVirtual = true) },
                                    onPlaceBets = { viewModel.placeBetsForUpcomingFixtures() }
                                )
                            }
                            AppScreen.RANKINGS -> {
                                ModelRankingsScreen(
                                    rankings = modelRankings,
                                    activeSimulatorModelId = uiState.activeSimulatorModelId,
                                    onSelectForSimulator = {
                                        viewModel.setActiveSimulatorModel(it)
                                        viewModel.setScreen(AppScreen.SIMULATOR)
                                    }
                                )
                            }
                            AppScreen.SIMULATOR -> {
                                PlayerSimulatorScreen(
                                    financials = simulationFinancials,
                                    modelComparisons = modelComparisons,
                                    bets = allBets,
                                    activeModelId = uiState.activeSimulatorModelId,
                                    stakePerBet = uiState.stakePerBet,
                                    showComparison = uiState.showSideBySideComparison,
                                    isBusy = uiState.isBusy,
                                    onModelSelected = { viewModel.setActiveSimulatorModel(it) },
                                    onStakeSelected = { viewModel.setStakePerBet(it) },
                                    onToggleComparison = { viewModel.toggleComparisonMode() },
                                    onPlaceBets = { viewModel.placeBetsForUpcomingFixtures() },
                                    onPlaceAllModelBets = { viewModel.placeBetsForAllModels() },
                                    onSimulateRound = { viewModel.processNextRound(forceVirtual = true) }
                                )
                            }
                            AppScreen.RECORDS -> {
                                UnifiedRecordsScreen(
                                    fixtures = allFixtures
                                )
                            }
                            AppScreen.COLLECTOR -> {
                                CollectorScreen(
                                    checkpoint = checkpoint,
                                    isBusy = uiState.isBusy,
                                    statusMessage = uiState.statusMessage,
                                    onProcessNextRound = { viewModel.processNextRound(forceVirtual = true) },
                                    onSimulateDisconnect = { viewModel.simulateConnectionLoss() },
                                    onResumeFromStartPoint = { mDay, rId ->
                                        viewModel.resumeCollection(mDay, rId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
