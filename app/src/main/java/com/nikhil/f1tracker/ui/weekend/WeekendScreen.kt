package com.nikhil.f1tracker.ui.weekend

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhil.f1tracker.domain.stats.CircuitStat
import com.nikhil.f1tracker.ui.weekend.strategy.StrategyTab
import com.nikhil.f1tracker.ui.weekend.track.TrackTab

@Composable
fun WeekendRoute(
    onBackClick: () -> Unit,
    onCircuitHistoryClick: (String) -> Unit,
    onDriverClick: (String) -> Unit,
    viewModel: WeekendViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeekendScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onTabSelected = viewModel::selectTab,
        onStatClick = viewModel::openStat,
        onOpenResults = { viewModel.openResults() },
        onCircuitHistoryClick = { onCircuitHistoryClick(uiState.circuitId) },
        onDriverClick = onDriverClick,
        onRetry = viewModel::retry,
    )
    uiState.resultsSheet?.let { sheet ->
        RaceResultsSheet(
            sheet = sheet,
            raceName = uiState.raceName,
            onSeasonSelected = viewModel::openResults,
            onDriverClick = { driverId ->
                viewModel.closeResults()
                onDriverClick(driverId)
            },
            onDismiss = viewModel::closeResults,
        )
    }
    uiState.statSheet?.let { sheet ->
        StatDetailSheet(
            sheet = sheet,
            onDismiss = viewModel::closeStat,
            onDriverClick = { driverId ->
                viewModel.closeStat()
                onDriverClick(driverId)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekendScreen(
    uiState: WeekendUiState,
    onBackClick: () -> Unit,
    onTabSelected: (WeekendTab) -> Unit,
    onStatClick: (CircuitStat) -> Unit,
    onOpenResults: () -> Unit,
    onCircuitHistoryClick: () -> Unit,
    onDriverClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.raceName.ifEmpty { "Race weekend" })
                        if (uiState.circuitName.isNotEmpty()) {
                            Text(
                                "${uiState.circuitName} · ${uiState.date}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).fillMaxSize()) {
            // Cached data stays visible while a sync runs; a first-time circuit backfill can take a while.
            if (uiState.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            uiState.loadErrorMessage?.let { ErrorBanner(it, onRetry) }
            PrimaryScrollableTabRow(selectedTabIndex = uiState.selectedTab.ordinal, edgePadding = 0.dp) {
                WeekendTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == uiState.selectedTab,
                        onClick = { onTabSelected(tab) },
                        text = { Text(tab.label) },
                    )
                }
            }
            when (uiState.selectedTab) {
                WeekendTab.CIRCUIT -> CircuitTab(uiState, onStatClick, onCircuitHistoryClick, onDriverClick, onOpenResults)
                WeekendTab.TRACK -> TrackTab()
                WeekendTab.STRATEGY -> StrategyTab()
                WeekendTab.FORM -> FormTab(uiState, onDriverClick)
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}
