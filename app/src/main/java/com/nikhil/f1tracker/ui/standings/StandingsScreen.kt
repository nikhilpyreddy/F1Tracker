package com.nikhil.f1tracker.ui.standings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.nikhil.f1tracker.ui.common.identity.DriverAvatar
import com.nikhil.f1tracker.ui.common.identity.StandingRow
import com.nikhil.f1tracker.ui.common.identity.TeamDot
import com.nikhil.f1tracker.ui.common.identity.driverColor
import com.nikhil.f1tracker.ui.common.identity.teamColor
import com.nikhil.f1tracker.ui.theme.F1TrackerTheme

@Composable
fun StandingsRoute(
    onDriverClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    viewModel: StandingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StandingsScreen(
        uiState = uiState,
        onModeSelected = viewModel::setMode,
        onDriverClick = onDriverClick,
        onTeamClick = onTeamClick,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsScreen(
    uiState: StandingsUiState,
    onModeSelected: (StandingsMode) -> Unit,
    onDriverClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Standings") }) },
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
                StandingsContent(uiState, onModeSelected, onDriverClick, onTeamClick)
            }
        }
    }
}

@Composable
private fun StandingsContent(
    uiState: StandingsUiState,
    onModeSelected: (StandingsMode) -> Unit,
    onDriverClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { ModeToggle(uiState.mode, onModeSelected, Modifier.padding(16.dp)) }
        when (uiState.mode) {
            StandingsMode.DRIVERS -> {
                val leaderPoints = uiState.driverStandings.maxOfOrNull { it.points } ?: 0.0
                items(uiState.driverStandings, key = { it.driverId }) { row ->
                    StandingRow(
                        position = row.position,
                        title = row.driverName,
                        subtitle = listOfNotNull(row.teamName, winsLabel(row.wins)).joinToString(" · "),
                        points = row.points,
                        leaderPoints = leaderPoints,
                        color = driverColor(row.driverId, row.constructorId),
                        onClick = { onDriverClick(row.driverId) },
                    ) { DriverAvatar(row.driverId, row.constructorId) }
                }
            }
            StandingsMode.TEAMS -> {
                val leaderPoints = uiState.constructorStandings.maxOfOrNull { it.points } ?: 0.0
                items(uiState.constructorStandings, key = { it.constructorId }) { row ->
                    StandingRow(
                        position = row.position,
                        title = row.teamName,
                        subtitle = winsLabel(row.wins),
                        points = row.points,
                        leaderPoints = leaderPoints,
                        color = teamColor(row.constructorId),
                        onClick = { onTeamClick(row.constructorId) },
                    ) { TeamDot(row.constructorId, size = 20.dp) }
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(mode: StandingsMode, onModeSelected: (StandingsMode) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StandingsMode.entries.forEach { candidate ->
            val isSelected = candidate == mode
            Box(
                modifier = Modifier
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onModeSelected(candidate) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    if (candidate == StandingsMode.DRIVERS) "Drivers" else "Teams",
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


private fun winsLabel(wins: Int): String? = when (wins) {
    0 -> null
    1 -> "1 win"
    else -> "$wins wins"
}
