package com.nikhil.f1tracker.ui.team

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhil.f1tracker.ui.common.ChartSeries
import com.nikhil.f1tracker.ui.common.LineChart
import com.nikhil.f1tracker.ui.common.chartSeriesColor
import com.nikhil.f1tracker.ui.theme.F1TrackerTheme

@Composable
fun TeamDetailRoute(onBackClick: () -> Unit, viewModel: TeamDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TeamDetailScreen(uiState = uiState, onBackClick = onBackClick, onRetry = viewModel::retry)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDetailScreen(
    uiState: TeamDetailUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(uiState.teamName.ifEmpty { "Team" }) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.padding(innerPadding).fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.loadErrorMessage != null -> Box(Modifier.padding(innerPadding).fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.loadErrorMessage, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Retry") }
                }
            }
            else -> TeamDetailContent(uiState, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun TeamDetailContent(uiState: TeamDetailUiState, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(16.dp)) {
                uiState.nationality?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(
                    "Points by season",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                LineChart(
                    series = listOf(ChartSeries("Points", chartSeriesColor(0), uiState.pointsTrend)),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Season standings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        }
        items(uiState.seasonStandings, key = { it.season }) { standing ->
            ListItem(
                headlineContent = { Text(standing.season.toString()) },
                supportingContent = { Text("${standing.wins} wins") },
                trailingContent = { Text("P${standing.position} · ${standing.points.formatPoints()} pts") },
            )
        }
    }
}

private fun Double.formatPoints(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

@Preview(showBackground = true)
@Composable
private fun TeamDetailScreenPreview() {
    F1TrackerTheme {
        TeamDetailScreen(
            uiState = TeamDetailUiState(
                isLoading = false,
                teamName = "Red Bull",
                nationality = "Austrian",
                pointsTrend = listOf(
                    com.nikhil.f1tracker.ui.common.ChartPoint("2023", 860f),
                    com.nikhil.f1tracker.ui.common.ChartPoint("2024", 589f),
                    com.nikhil.f1tracker.ui.common.ChartPoint("2025", 402f),
                    com.nikhil.f1tracker.ui.common.ChartPoint("2026", 210f),
                ),
                seasonStandings = listOf(TeamSeasonStanding(2026, 3, 210.0, 2)),
            ),
            onBackClick = {},
            onRetry = {},
        )
    }
}
