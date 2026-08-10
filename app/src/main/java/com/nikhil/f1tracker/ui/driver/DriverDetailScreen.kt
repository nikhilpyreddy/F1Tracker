package com.nikhil.f1tracker.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nikhil.f1tracker.ui.common.ChartPoint
import com.nikhil.f1tracker.ui.common.ChartSeries
import com.nikhil.f1tracker.ui.common.LineChart
import com.nikhil.f1tracker.ui.common.chartSeriesColor
import com.nikhil.f1tracker.ui.theme.F1TrackerTheme

@Composable
fun DriverDetailRoute(
    onBackClick: () -> Unit,
    onResultClick: (circuitId: String) -> Unit,
    viewModel: DriverDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DriverDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onSeasonSelected = viewModel::selectSeason,
        onResultClick = onResultClick,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDetailScreen(
    uiState: DriverDetailUiState,
    onBackClick: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onResultClick: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(uiState.driverName.ifEmpty { "Driver" }) },
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
            else -> DriverDetailContent(
                uiState = uiState,
                onSeasonSelected = onSeasonSelected,
                onResultClick = onResultClick,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun DriverDetailContent(
    uiState: DriverDetailUiState,
    onSeasonSelected: (Int) -> Unit,
    onResultClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    "Results",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                )
                SeasonPicker(uiState.availableSeasons, uiState.selectedSeason, onSeasonSelected)
            }
        }
        items(uiState.seasonResults, key = { it.round }) { result ->
            ListItem(
                headlineContent = { Text(result.raceName) },
                supportingContent = { Text(result.status) },
                trailingContent = {
                    Text("${result.positionText} · ${result.points.formatPoints()} pts")
                },
                modifier = Modifier.clickable { onResultClick(result.circuitId) },
            )
        }
    }
}

@Composable
private fun SeasonPicker(seasons: List<Int>, selected: Int?, onSeasonSelected: (Int) -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            seasons.forEach { season ->
                val isSelected = season == selected
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable { onSeasonSelected(season) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        season.toString(),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun Double.formatPoints(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

@Preview(showBackground = true)
@Composable
private fun DriverDetailScreenPreview() {
    F1TrackerTheme {
        DriverDetailScreen(
            uiState = DriverDetailUiState(
                isLoading = false,
                driverName = "Max Verstappen",
                driverCode = "VER",
                nationality = "Dutch",
                pointsTrend = listOf(
                    ChartPoint("2023", 575f),
                    ChartPoint("2024", 437f),
                    ChartPoint("2025", 396f),
                    ChartPoint("2026", 109f),
                ),
                availableSeasons = listOf(2023, 2024, 2025, 2026),
                selectedSeason = 2026,
                seasonResults = listOf(
                    DriverSeasonResultRow(1, "Bahrain Grand Prix", "bahrain", "6", 8.0, "Finished"),
                ),
            ),
            onBackClick = {},
            onSeasonSelected = {},
            onResultClick = {},
            onRetry = {},
        )
    }
}
