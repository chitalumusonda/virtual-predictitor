package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CollectionCheckpoint
import com.example.data.model.PaperBetEntity
import com.example.data.model.PredictionEntity
import com.example.data.model.UnifiedFixtureRecord
import com.example.data.repository.VirtualBetRepository
import com.example.engine.ModelRankStats
import com.example.engine.ModelRankingEngine
import com.example.engine.ModelRegistry
import com.example.engine.ModelSimulationComparison
import com.example.engine.PlayerSimulatorEngine
import com.example.engine.SimulationFinancialSummary
import com.example.network.BetPawaClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen(val title: String) {
    PRE_MATCH("Pre-Match"),
    RANKINGS("Model Ranks"),
    SIMULATOR("Simulator"),
    RECORDS("Records"),
    COLLECTOR("Collector")
}

data class VirtualBetUiState(
    val currentScreen: AppScreen = AppScreen.PRE_MATCH,
    val selectedMarketFilter: String = "ALL", // "ALL", "1X2", "O/U 2.5", "BTTS", "DC"
    val selectedLeagueFilter: String = "ALL",
    val activeSimulatorModelId: String = ModelRegistry.CONSENSUS.id,
    val stakePerBet: Double = 10.0,
    val showSideBySideComparison: Boolean = false,
    val isAutoCollecting: Boolean = false,
    val statusMessage: String? = null,
    val isBusy: Boolean = false
)

class VirtualBetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VirtualBetRepository

    init {
        val db = AppDatabase.getInstance(application)
        val client = BetPawaClient(application)
        repository = VirtualBetRepository(db, client)

        viewModelScope.launch {
            repository.initialize()
        }
    }

    private val _uiState = MutableStateFlow(VirtualBetUiState())
    val uiState: StateFlow<VirtualBetUiState> = _uiState.asStateFlow()

    val upcomingFixtures: StateFlow<List<UnifiedFixtureRecord>> = repository.upcomingFixtures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFixtures: StateFlow<List<UnifiedFixtureRecord>> = repository.allFixtures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val checkpoint: StateFlow<CollectionCheckpoint?> = repository.checkpoint
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allPredictions: StateFlow<List<PredictionEntity>> = repository.allPredictions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBets: StateFlow<List<PaperBetEntity>> = repository.allBets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic model rankings (Ranked best to worst in getting predictions right)
    val modelRankings: StateFlow<List<ModelRankStats>> = combine(
        repository.allPredictions,
        repository.allBets
    ) { preds, bets ->
        ModelRankingEngine.computeRankings(preds, bets)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial KPIs for player simulator based on selected active model
    val simulationFinancials: StateFlow<SimulationFinancialSummary> = combine(
        _uiState,
        repository.allBets
    ) { state, bets ->
        val modelBets = if (state.activeSimulatorModelId == "ALL") {
            bets.filter { it.status != "PENDING" }
        } else {
            bets.filter { it.modelId == state.activeSimulatorModelId && it.status != "PENDING" }
        }
        PlayerSimulatorEngine.calculateFinancials(1000.0, modelBets)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SimulationFinancialSummary(1000.0, 1000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0.0)
    )

    // Side-by-side financial comparison across all individual models and Consolidated Consensus
    val modelComparisons: StateFlow<List<ModelSimulationComparison>> = repository.allBets.map { bets ->
        val settled = bets.filter { it.status != "PENDING" }
        PlayerSimulatorEngine.compareAllModels(1000.0, settled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setScreen(screen: AppScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun setMarketFilter(market: String) {
        _uiState.value = _uiState.value.copy(selectedMarketFilter = market)
    }

    fun setLeagueFilter(league: String) {
        _uiState.value = _uiState.value.copy(selectedLeagueFilter = league)
    }

    fun setActiveSimulatorModel(modelId: String) {
        _uiState.value = _uiState.value.copy(activeSimulatorModelId = modelId)
    }

    fun setStakePerBet(stake: Double) {
        _uiState.value = _uiState.value.copy(stakePerBet = stake)
    }

    fun toggleComparisonMode() {
        _uiState.value = _uiState.value.copy(
            showSideBySideComparison = !_uiState.value.showSideBySideComparison
        )
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    /**
     * Advances to the next round: settles existing round and creates new upcoming round.
     */
    fun processNextRound(forceVirtual: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            val result = repository.collectNextRound(forceVirtual = forceVirtual)
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                statusMessage = result.getOrNull() ?: result.exceptionOrNull()?.message
            )
        }
    }

    /**
     * Simulates connection loss and confirms the Start Point is auto-saved.
     */
    fun simulateConnectionLoss() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            repository.handleConnectionInterruption("Simulated manual network cut")
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                statusMessage = "Connection lost. Start point saved."
            )
        }
    }

    /**
     * Resumes data collection from the saved start point.
     */
    fun resumeCollection(matchday: Int, roundId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            repository.resumeFromStartPoint(matchday, roundId)
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                statusMessage = "Resumed collection from Matchday $matchday ($roundId)"
            )
        }
    }

    /**
     * Player places simulated bets for all upcoming fixtures using active model or Consensus.
     */
    fun placeBetsForUpcomingFixtures() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            val upcoming = upcomingFixtures.value
            val currentFin = simulationFinancials.value
            val modelId = _uiState.value.activeSimulatorModelId
            val stake = _uiState.value.stakePerBet

            var bankroll = currentFin.currentBankroll
            for (fix in upcoming) {
                if (bankroll >= stake) {
                    repository.placeSimulatedBet(fix, modelId, stake, bankroll)
                    bankroll -= stake
                }
            }
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                statusMessage = "Placed bets for ${upcoming.size} upcoming games using ${ModelRegistry.getById(modelId).name}."
            )
        }
    }

    /**
     * Places simulated bets for ALL models simultaneously to run a full round simulation
     * and compare individual models against the Consolidated Consensus.
     */
    fun placeBetsForAllModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true)
            val currentFin = simulationFinancials.value
            repository.placeSimulatedBetsForAllModels(
                stake = _uiState.value.stakePerBet,
                currentBankroll = currentFin.currentBankroll
            )
            _uiState.value = _uiState.value.copy(
                isBusy = false,
                statusMessage = "Simulated bets placed across ALL models and Consolidated Consensus!"
            )
        }
    }
}
