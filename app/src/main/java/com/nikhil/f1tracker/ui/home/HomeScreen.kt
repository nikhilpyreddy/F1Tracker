package com.nikhil.f1tracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhil.f1tracker.ui.common.identity.DriverAvatar
import com.nikhil.f1tracker.ui.common.identity.StandingRow
import com.nikhil.f1tracker.ui.common.identity.TeamDot
import com.nikhil.f1tracker.ui.common.identity.driverColor
import com.nikhil.f1tracker.ui.common.identity.teamColor
import com.nikhil.f1tracker.ui.theme.F1TrackerTheme

@Composable
fun HomeRoute(
    onDriverClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    onRaceClick: (season: Int, round: Int, circuitId: String) -> Unit,
    onFavoritesClick: () -> Unit,
    onCompareClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onDriverClick = onDriverClick,
        onTeamClick = onTeamClick,
        onRaceClick = onRaceClick,
        onFavoritesClick = onFavoritesClick,
        onCompareClick = onCompareClick,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retrySync,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onDriverClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    onRaceClick: (season: Int, round: Int, circuitId: String) -> Unit,
    onFavoritesClick: () -> Unit,
    onCompareClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("F1 Tracker") },
                actions = { HomeMenu(onFavoritesClick, onCompareClick) },
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
            else -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
            ) {
                HomeContent(uiState, onDriverClick, onTeamClick, onRaceClick, onFavoritesClick)
            }
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onDriverClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    onRaceClick: (season: Int, round: Int, circuitId: String) -> Unit,
    onFavoritesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { NextRaceCard(uiState.nextRace, onClick = { onRaceClick(it.season, it.round, it.circuitId) }) }
        item {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Your favourites", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onFavoritesClick) { Text("Edit") }
            }
        }
        if (uiState.favoriteDrivers.isNotEmpty()) {
            items(uiState.favoriteDrivers, key = { it.driverId }) { driver ->
                StandingRow(
                    position = driver.position,
                    title = driver.driverName,
                    subtitle = driver.teamName,
                    points = driver.points,
                    leaderPoints = uiState.driverLeaderPoints,
                    color = driverColor(driver.driverId, driver.constructorId),
                    onClick = { onDriverClick(driver.driverId) },
                ) { DriverAvatar(driver.driverId, driver.constructorId) }
            }
        }
        if (uiState.favoriteTeams.isNotEmpty()) {
            items(uiState.favoriteTeams, key = { it.teamId }) { team ->
                StandingRow(
                    position = team.position,
                    title = team.teamName,
                    subtitle = null,
                    points = team.points,
                    leaderPoints = uiState.teamLeaderPoints,
                    color = teamColor(team.teamId),
                    onClick = { onTeamClick(team.teamId) },
                ) { TeamDot(team.teamId, size = 20.dp) }
            }
        }
        if (uiState.favoriteDrivers.isEmpty() && uiState.favoriteTeams.isEmpty()) {
            item {
                OutlinedButton(onClick = onFavoritesClick, modifier = Modifier.padding(16.dp)) {
                    Text("Pick favourite drivers and teams")
                }
            }
        }
    }
}


@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}


@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    F1TrackerTheme {
        HomeScreen(
            uiState = HomeUiState(
                isLoading = false,
                nextRace = UpcomingRace("Bahrain Grand Prix", "2026-03-08", 1, "bahrain", 2026),
                favoriteDrivers = listOf(
                    FavoriteDriverStanding("max_verstappen", "Max Verstappen", "Red Bull", "red_bull", 1, 437.0),
                ),
                favoriteTeams = listOf(
                    FavoriteTeamStanding("red_bull", "Red Bull", 1, 589.0),
                ),
            ),
            onDriverClick = {},
            onTeamClick = {},
            onRaceClick = { _, _, _ -> },
            onFavoritesClick = {},
            onCompareClick = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}

@Composable
private fun HomeMenu(onFavoritesClick: () -> Unit, onCompareClick: () -> Unit) {
    var isOpen by remember { mutableStateOf(false) }
    var showSources by remember { mutableStateOf(false) }
    IconButton(onClick = { isOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
    DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
        DropdownMenuItem(text = { Text("Favourites") }, onClick = { isOpen = false; onFavoritesClick() })
        DropdownMenuItem(text = { Text("Compare") }, onClick = { isOpen = false; onCompareClick() })
        DropdownMenuItem(text = { Text("Data sources") }, onClick = { isOpen = false; showSources = true })
    }
    if (showSources) {
        AlertDialog(
            onDismissRequest = { showSources = false },
            confirmButton = { TextButton(onClick = { showSources = false }) { Text("OK") } },
            title = { Text("Data sources") },
            text = {
                Text(
                    "Results, qualifying and standings: Jolpica-F1 (api.jolpi.ca).\n" +
                        "Telemetry, tyres, race control, team colours: OpenF1 (openf1.org), 2023 onwards.\n" +
                        "Weather: Open-Meteo (open-meteo.com).\n\n" +
                        "All times are US Central (CDT/CST). Personal, non-commercial use.",
                )
            },
        )
    }
}
