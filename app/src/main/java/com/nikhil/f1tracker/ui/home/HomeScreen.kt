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
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhil.f1tracker.ui.theme.F1TrackerTheme

@Composable
fun HomeRoute(
    onDriverClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    onGrandPrixClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onDriverClick = onDriverClick,
        onTeamClick = onTeamClick,
        onGrandPrixClick = onGrandPrixClick,
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
    onGrandPrixClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("F1 Tracker") }) },
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
                HomeContent(uiState, onDriverClick, onTeamClick, onGrandPrixClick)
            }
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onDriverClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    onGrandPrixClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { NextRaceCard(uiState.nextRace, onClick = onGrandPrixClick) }
        if (uiState.favoriteDrivers.isNotEmpty()) {
            item { SectionHeader("Favorite drivers") }
            items(uiState.favoriteDrivers, key = { it.driverId }) { driver ->
                ListItem(
                    headlineContent = { Text(driver.driverName) },
                    supportingContent = driver.teamName?.let { { Text(it) } },
                    trailingContent = { Text("P${driver.position} · ${driver.points.formatPoints()} pts") },
                    modifier = Modifier.clickable { onDriverClick(driver.driverId) },
                )
            }
        }
        if (uiState.favoriteTeams.isNotEmpty()) {
            item { SectionHeader("Favorite teams") }
            items(uiState.favoriteTeams, key = { it.teamId }) { team ->
                ListItem(
                    headlineContent = { Text(team.teamName) },
                    trailingContent = { Text("P${team.position} · ${team.points.formatPoints()} pts") },
                    modifier = Modifier.clickable { onTeamClick(team.teamId) },
                )
            }
        }
        if (uiState.favoriteDrivers.isEmpty() && uiState.favoriteTeams.isEmpty()) {
            item {
                Text(
                    text = "Pick your favorite drivers and teams to see them here.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun NextRaceCard(nextRace: UpcomingRace?, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp)
            .let { if (nextRace != null) it.clickable { onClick(nextRace.circuitId) } else it },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Next race", style = MaterialTheme.typography.labelLarge)
            Text(
                text = nextRace?.raceName ?: "Season complete",
                style = MaterialTheme.typography.titleLarge,
            )
            if (nextRace != null) {
                Text(text = nextRace.date, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private fun Double.formatPoints(): String = if (this == this.toLong().toDouble()) {
    this.toLong().toString()
} else {
    this.toString()
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    F1TrackerTheme {
        HomeScreen(
            uiState = HomeUiState(
                isLoading = false,
                nextRace = UpcomingRace("Bahrain Grand Prix", "2026-03-08", 1, "bahrain"),
                favoriteDrivers = listOf(
                    FavoriteDriverStanding("max_verstappen", "Max Verstappen", "Red Bull", 1, 437.0),
                ),
                favoriteTeams = listOf(
                    FavoriteTeamStanding("red_bull", "Red Bull", 1, 589.0),
                ),
            ),
            onDriverClick = {},
            onTeamClick = {},
            onGrandPrixClick = {},
            onRefresh = {},
            onRetry = {},
        )
    }
}
