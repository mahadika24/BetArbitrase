package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.csv.CsvExporter
import com.example.data.csv.CsvImportResult
import com.example.data.csv.CsvParser
import com.example.data.csv.CsvValidationError
import com.example.data.model.BetObservation
import com.example.data.model.FootballMatch
import com.example.data.model.ResearchStrategyType
import com.example.data.sample.SampleHistoricalData
import com.example.stats.StrategyEvaluationReport
import com.example.stats.StrategyEvaluator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ResearchUiState(
    val isLoading: Boolean = false,
    val matches: List<FootballMatch> = emptyList(),
    val observations: List<BetObservation> = emptyList(),
    val stageFilter: String = "ALL", // "ALL", "KNOCKOUT", "LEAGUE"
    val selectedStrategy: ResearchStrategyType = ResearchStrategyType.UNDER_3_5,
    val strategyReports: Map<ResearchStrategyType, StrategyEvaluationReport> = emptyMap(),
    val sameSampleReports: Map<ResearchStrategyType, StrategyEvaluationReport> = emptyMap(),
    val knockoutOnlyReports: Map<ResearchStrategyType, StrategyEvaluationReport> = emptyMap(),
    val validationErrors: List<CsvValidationError> = emptyList(),
    val activeNavTab: Int = 0, // 0: Dashboard, 1: Dataset, 2: Strategies, 3: Knockout, 4: Same-Sample, 5: Out-of-Sample, 6: Robustness, 7: Report
    val bootstrapIterations: Int = 10_000,
    val infoMessage: String? = null
)

class ResearchViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ResearchUiState(isLoading = true))
    val uiState: StateFlow<ResearchUiState> = _uiState.asStateFlow()

    init {
        loadBenchmarkDataset()
    }

    fun setNavTab(tabIndex: Int) {
        _uiState.update { it.copy(activeNavTab = tabIndex) }
    }

    fun setSelectedStrategy(strategy: ResearchStrategyType) {
        _uiState.update { it.copy(selectedStrategy = strategy) }
    }

    fun setStageFilter(filter: String) {
        _uiState.update { it.copy(stageFilter = filter) }
        recomputeAllReports()
    }

    fun loadBenchmarkDataset() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val (matches, observations) = withContext(Dispatchers.Default) {
                SampleHistoricalData.getBenchmarkDataset()
            }
            _uiState.update {
                it.copy(
                    matches = matches,
                    observations = observations,
                    validationErrors = emptyList(),
                    infoMessage = "Loaded benchmark historical dataset (${matches.size} matches, ${observations.size} observations)"
                )
            }
            recomputeAllReports()
        }
    }

    fun importCsv(csvContent: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result: CsvImportResult = withContext(Dispatchers.Default) {
                CsvParser.parse(csvContent)
            }

            if (result.validRowsCount == 0 && result.errors.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        validationErrors = result.errors,
                        infoMessage = "CSV import failed: No valid rows could be imported."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    matches = result.matches,
                    observations = result.observations,
                    validationErrors = result.errors,
                    infoMessage = "Imported ${result.matches.size} matches, ${result.observations.size} observations. (${result.errors.size} validation warnings)"
                )
            }
            recomputeAllReports()
        }
    }

    fun clearInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun getExportableDataCsv(): String {
        val s = _uiState.value
        return CsvExporter.exportObservationsToCsv(s.matches, s.observations)
    }

    fun getExportableComparisonCsv(): String {
        val s = _uiState.value
        return CsvExporter.exportStrategyComparisonCsv(s.strategyReports.values.toList())
    }

    fun recomputeAllReports() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentState = _uiState.value
            val allObs = currentState.observations
            val allMatches = currentState.matches
            val matchMap = allMatches.associateBy { it.id }

            val (regularReports, knockoutReports, sameSampleReports) = withContext(Dispatchers.Default) {
                // 1. Filter based on stageFilter
                val filteredObs = when (currentState.stageFilter) {
                    "KNOCKOUT" -> allObs.filter { matchMap[it.matchId]?.isKnockout == true }
                    "LEAGUE" -> allObs.filter { matchMap[it.matchId]?.isKnockout == false }
                    else -> allObs
                }

                val repMap = LinkedHashMap<ResearchStrategyType, StrategyEvaluationReport>()
                for (type in ResearchStrategyType.values()) {
                    val strategyObs = filteredObs.filter { it.strategyType == type }
                    val report = StrategyEvaluator.evaluate(
                        strategyType = type,
                        observations = strategyObs,
                        bootstrapIterations = currentState.bootstrapIterations
                    )
                    repMap[type] = report
                }

                // 2. Knockout-only reports specifically
                val koObs = allObs.filter { matchMap[it.matchId]?.isKnockout == true }
                val koRepMap = LinkedHashMap<ResearchStrategyType, StrategyEvaluationReport>()
                for (type in ResearchStrategyType.values()) {
                    val strategyObs = koObs.filter { it.strategyType == type }
                    val report = StrategyEvaluator.evaluate(
                        strategyType = type,
                        observations = strategyObs,
                        bootstrapIterations = currentState.bootstrapIterations
                    )
                    koRepMap[type] = report
                }

                // 3. Apple-to-Apple / Same-Sample Comparison
                // Group match IDs that have observations for ALL 6 strategies (or at least 2)
                val matchesWithAllStrategies = allMatches.filter { m ->
                    val obsForMatch = allObs.filter { it.matchId == m.id }
                    val uniqueTypes = obsForMatch.map { it.strategyType }.distinct()
                    uniqueTypes.size >= 4 // comparable intersection
                }.map { it.id }.toSet()

                val sameSampleObs = allObs.filter { it.matchId in matchesWithAllStrategies }
                val sameRepMap = LinkedHashMap<ResearchStrategyType, StrategyEvaluationReport>()
                for (type in ResearchStrategyType.values()) {
                    val strategyObs = sameSampleObs.filter { it.strategyType == type }
                    val report = StrategyEvaluator.evaluate(
                        strategyType = type,
                        observations = strategyObs,
                        bootstrapIterations = currentState.bootstrapIterations
                    )
                    sameRepMap[type] = report
                }

                Triple(repMap, koRepMap, sameRepMap)
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    strategyReports = regularReports,
                    knockoutOnlyReports = knockoutReports,
                    sameSampleReports = sameSampleReports
                )
            }
        }
    }
}
