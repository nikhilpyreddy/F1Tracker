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
import androidx.compose.material3.TextButton
import java.util.Locale
import com.nikhil.f1tracker.ui.common.RacePosition
import com.nikhil.f1tracker.ui.common.PositionChart
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
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
import com.nikhil.f1tracker.ui.common.identity.DriverAvatar
import com.nikhil.f1tracker.ui.common.identity.FinishBadge
import com.nikhil.f1tracker.ui.common.identity.TeamDot
import com.nikhil.f1tracker.ui.common.identity.driverColor
import com.nikhil.f1tracker.ui.common.identity.teamColor
import com.nikhil.f1tracker.ui.common.identity.teamStripe
import com.nikhil.f1tracker.ui.theme.F1TrackerTheme

@Composable
fun DriverDetailRoute(
    onBackClick: () -> Unit,
    onResultClick: (season: Int, round: Int, circuitId: String) -> Unit,
    onCompareClick: () -> Unit,
    viewModel: DriverDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DriverDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onSeasonSelected = viewModel::selectSeason,
        onResultClick = onResultClick,
        onCompareClick = onCompareClick,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDetailScreen(
    uiState: DriverDetailUiState,
    onBackClick: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onResultClick: (season: Int, round: Int, circuitId: String) -> Unit,
    onCompareClick: () -> Unit,
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
                actions = { TextButton(onClick = onCompareClick) { Text("Compare with…") } },
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
    onResultClick: (season: Int, round: Int, circuitId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(DriverTab.RACE_BY_RACE) }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DriverAvatar(uiState.driverId, size = 72.dp)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(uiState.driverName, style = MaterialTheme.typography.titleLarge)
                        uiState.nationality?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                PrimaryTabRow(selectedTabIndex = tab.ordinal, modifier = Modifier.padding(top = 16.dp)) {
                    DriverTab.entries.forEach { candidate ->
                        Tab(selected = tab == candidate, onClick = { tab = candidate }, text = { Text(candidate.label) })
                    }
                }
                when (tab) {
                    DriverTab.SEASONS -> {
                        Text(
                            "Points by season",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        )
                        LineChart(
                            series = listOf(ChartSeries("Points", driverColor(uiState.driverId), uiState.pointsTrend)),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    DriverTab.RACE_BY_RACE -> {
                        Text(
                            "Finishing position by Grand Prix",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        )
                        SeasonPicker(uiState.availableSeasons, uiState.selectedSeason, onSeasonSelected)
                        PositionChart(
                            races = uiState.seasonResults.map {
                                RacePosition(it.round, it.positionText.toIntOrNull(), it.positionText, it.grid)
                            },
                            lineColor = driverColor(uiState.driverId),
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        )
                        SeasonSummary(uiState.seasonResults)
                    }
                }
            }
        }
        if (tab == DriverTab.SEASONS) return@LazyColumn
        items(uiState.seasonResults, key = { it.round }) { result ->
            ListItem(
                leadingContent = { FinishBadge(result.positionText, size = 36.dp) },
                headlineContent = { Text(result.raceName) },
                supportingContent = {
                    Text(if (result.grid > 0) "Started P${result.grid} · ${result.status}" else "Pit-lane start · ${result.status}")
                },
                trailingContent = {
                    Text("${result.positionText} · ${result.points.formatPoints()} pts")
                },
                modifier = Modifier.clickable {
                    uiState.selectedSeason?.let { onResultClick(it, result.round, result.circuitId) }
                },
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
                    DriverSeasonResultRow(1, "Bahrain Grand Prix", "bahrain", "6", 3, 8.0, "Finished"),
                ),
            ),
            onBackClick = {},
            onSeasonSelected = {},
            onResultClick = { _, _, _ -> },
            onCompareClick = {},
            onRetry = {},
        )
    }
}

private enum class DriverTab(val label: String) { SEASONS("Seasons"), RACE_BY_RACE("Race by race") }

/** Season-at-a-glance numbers for the race-by-race view. */
@Composable
private fun SeasonSummary(results: List<DriverSeasonResultRow>) {
    if (results.isEmpty()) return
    val finishes = results.mapNotNull { it.positionText.toIntOrNull() }
    val gained = results.filter { it.grid > 0 }.mapNotNull { r -> r.positionText.toIntOrNull()?.let { r.grid - it } }
    val parts = listOfNotNull(
        finishes.takeIf { it.isNotEmpty() }?.let { "Avg finish P%.1f".format(Locale.US, it.average()) },
        "Wins ${finishes.count { it == 1 }}",
        "Podiums ${finishes.count { it <= 3 }}",
        "Top 10 ${finishes.count { it <= 10 }}/${results.size}",
        "DNF ${results.size - finishes.size}",
        gained.takeIf { it.isNotEmpty() }?.let { "Avg places gained %+.1f".format(Locale.US, it.average()) },
    )
    Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
}
