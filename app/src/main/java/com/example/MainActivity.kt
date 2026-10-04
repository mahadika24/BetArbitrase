package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DatasetScreen
import com.example.ui.screens.KnockoutAnalysisScreen
import com.example.ui.screens.OutOfSampleScreen
import com.example.ui.screens.ResearchReportScreen
import com.example.ui.screens.RobustnessScreen
import com.example.ui.screens.SameSampleComparisonScreen
import com.example.ui.screens.StrategiesScreen
import com.example.ui.theme.FootyResearchTheme
import com.example.ui.theme.QuantCyan
import com.example.ui.viewmodel.ResearchViewModel

data class NavItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

val NAV_ITEMS = listOf(
    NavItem("Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    NavItem("Dataset", Icons.Default.Dataset, "nav_dataset"),
    NavItem("Strategies", Icons.Default.Timeline, "nav_strategies"),
    NavItem("Knockout", Icons.Default.EmojiEvents, "nav_knockout"),
    NavItem("Same-Sample", Icons.Default.CompareArrows, "nav_same_sample"),
    NavItem("Out-of-Sample", Icons.Default.CallSplit, "nav_oos"),
    NavItem("Robustness", Icons.Default.Shield, "nav_robustness"),
    NavItem("Report", Icons.Default.Assignment, "nav_report")
)

class MainActivity : ComponentActivity() {
    private val viewModel: ResearchViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var userDarkTheme by remember { mutableStateOf<Boolean?>(null) }
            val isDark = userDarkTheme ?: systemDark

            FootyResearchTheme(darkTheme = isDark) {
                val state by viewModel.uiState.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                // Display info notifications
                LaunchedEffect(state.infoMessage) {
                    state.infoMessage?.let {
                        snackbarHostState.showSnackbar(
                            message = it,
                            duration = SnackbarDuration.Short
                        )
                        viewModel.clearInfoMessage()
                    }
                }

                // Handle back press to return to Dashboard
                BackHandler(enabled = state.activeNavTab != 0) {
                    viewModel.setNavTab(0)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = QuantCyan.copy(alpha = 0.2f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Science,
                                            contentDescription = "Research Lab Icon",
                                            tint = QuantCyan,
                                            modifier = Modifier.padding(6.dp).size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "QuantFooty Lab",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Match-Market Statistical Research",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { userDarkTheme = !isDark },
                                    modifier = Modifier.testTag("theme_toggle_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Toggle Theme"
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            ScrollableTabRow(
                                selectedTabIndex = state.activeNavTab,
                                containerColor = MaterialTheme.colorScheme.surface,
                                edgePadding = 12.dp,
                                modifier = Modifier.fillMaxWidth().testTag("main_nav_tabs")
                            ) {
                                NAV_ITEMS.forEachIndexed { index, item ->
                                    Tab(
                                        selected = state.activeNavTab == index,
                                        onClick = { viewModel.setNavTab(index) },
                                        modifier = Modifier.testTag(item.testTag),
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = item.title,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (state.activeNavTab == index) QuantCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = item.title,
                                                    fontWeight = if (state.activeNavTab == index) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (state.activeNavTab == index) QuantCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (state.isLoading) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            color = QuantCyan,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Running 10,000 Bootstrap Resamples...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "Estimating empirical ROI confidence intervals",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            when (state.activeNavTab) {
                                0 -> DashboardScreen(
                                    state = state,
                                    onStrategySelected = {
                                        viewModel.setSelectedStrategy(it)
                                        viewModel.setNavTab(2) // Jump to Strategies deep dive
                                    },
                                    onNavigateToTab = { viewModel.setNavTab(it) },
                                    onStageFilterChanged = { viewModel.setStageFilter(it) }
                                )
                                1 -> DatasetScreen(
                                    state = state,
                                    onImportCsv = { viewModel.importCsv(it) },
                                    onResetToBenchmark = { viewModel.loadBenchmarkDataset() },
                                    onGetExportDataCsv = { viewModel.getExportableDataCsv() },
                                    onGetExportComparisonCsv = { viewModel.getExportableComparisonCsv() }
                                )
                                2 -> StrategiesScreen(
                                    state = state,
                                    onStrategySelected = { viewModel.setSelectedStrategy(it) }
                                )
                                3 -> KnockoutAnalysisScreen(state = state)
                                4 -> SameSampleComparisonScreen(state = state)
                                5 -> OutOfSampleScreen(
                                    state = state,
                                    onStrategySelected = { viewModel.setSelectedStrategy(it) }
                                )
                                6 -> RobustnessScreen(
                                    state = state,
                                    onStrategySelected = { viewModel.setSelectedStrategy(it) }
                                )
                                7 -> ResearchReportScreen(
                                    state = state,
                                    onExportReport = { /* Handled in screen */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
