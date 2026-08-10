package com.nikhil.f1tracker.ui.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhil.f1tracker.ui.common.ChartSeries
import com.nikhil.f1tracker.ui.common.LineChart
import com.nikhil.f1tracker.ui.common.chartSeriesColor
import com.nikhil.f1tracker.ui.theme.F1TrackerTheme

@Composable
fun CompareRoute(viewModel: CompareViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CompareScreen(
        uiState = uiState,
        onModeSelected = viewModel::setMode,
        onItemSelected = viewModel::select,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    uiState: CompareUiState,
    onModeSelected: (CompareMode) -> Unit,
    onItemSelected: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Compare") }) },
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
            else -> CompareContent(uiState, onModeSelected, onItemSelected, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun CompareContent(
    uiState: CompareUiState,
    onModeSelected: (CompareMode) -> Unit,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorA = chartSeriesColor(0)
    val colorB = chartSeriesColor(1)

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(16.dp)) {
                ModeToggle(uiState.mode, onModeSelected)
                if (uiState.selectedFirstId != null || uiState.selectedSecondId != null) {
                    val series = buildList {
                        uiState.firstName?.let { add(ChartSeries(it, colorA, uiState.firstTrend)) }
                        uiState.secondName?.let { add(ChartSeries(it, colorB, uiState.secondTrend)) }
                    }
                    Text(
                        "Points by season",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                    )
                    LineChart(series = series, modifier = Modifier.fillMaxWidth())
                } else {
                    Text(
                        "Pick two ${if (uiState.mode == CompareMode.DRIVERS) "drivers" else "teams"} to compare.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 24.dp),
                    )
                }
            }
        }
        when (uiState.mode) {
            CompareMode.DRIVERS -> items(uiState.availableDrivers, key = { it.driverId }) { driver ->
                CompareRow(
                    title = "${driver.givenName} ${driver.familyName}",
                    subtitle = driver.code,
                    badge = badgeFor(driver.driverId, uiState.selectedFirstId, uiState.selectedSecondId),
                    colorA = colorA,
                    colorB = colorB,
                    onClick = { onItemSelected(driver.driverId) },
                )
            }
            CompareMode.TEAMS -> items(uiState.availableTeams, key = { it.constructorId }) { team ->
                CompareRow(
                    title = team.name,
                    subtitle = team.nationality,
                    badge = badgeFor(team.constructorId, uiState.selectedFirstId, uiState.selectedSecondId),
                    colorA = colorA,
                    colorB = colorB,
                    onClick = { onItemSelected(team.constructorId) },
                )
            }
        }
    }
}

private enum class SelectionBadge { NONE, A, B }

private fun badgeFor(id: String, firstId: String?, secondId: String?): SelectionBadge = when (id) {
    firstId -> SelectionBadge.A
    secondId -> SelectionBadge.B
    else -> SelectionBadge.NONE
}

@Composable
private fun CompareRow(
    title: String,
    subtitle: String?,
    badge: SelectionBadge,
    colorA: Color,
    colorB: Color,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {
            if (badge != SelectionBadge.NONE) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(if (badge == SelectionBadge.A) colorA else colorB, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (badge == SelectionBadge.A) "A" else "B", color = Color.White)
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun ModeToggle(mode: CompareMode, onModeSelected: (CompareMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CompareMode.entries.forEach { candidate ->
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
                    if (candidate == CompareMode.DRIVERS) "Drivers" else "Teams",
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CompareScreenPreview() {
    F1TrackerTheme {
        CompareScreen(
            uiState = CompareUiState(
                isLoading = false,
                mode = CompareMode.DRIVERS,
                firstName = "Max Verstappen",
                secondName = "Lando Norris",
                firstTrend = listOf(
                    com.nikhil.f1tracker.ui.common.ChartPoint("2023", 575f),
                    com.nikhil.f1tracker.ui.common.ChartPoint("2024", 437f),
                ),
                secondTrend = listOf(
                    com.nikhil.f1tracker.ui.common.ChartPoint("2023", 205f),
                    com.nikhil.f1tracker.ui.common.ChartPoint("2024", 374f),
                ),
                selectedFirstId = "max_verstappen",
                selectedSecondId = "norris",
            ),
            onModeSelected = {},
            onItemSelected = {},
            onRetry = {},
        )
    }
}
