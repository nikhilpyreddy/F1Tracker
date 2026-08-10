package com.nikhil.f1tracker.ui.grandprix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.nikhil.f1tracker.domain.model.GRAND_PRIX_HISTORY_YEARS
import com.nikhil.f1tracker.ui.theme.F1TrackerTheme

@Composable
fun GrandPrixDetailRoute(onBackClick: () -> Unit, viewModel: GrandPrixDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GrandPrixDetailScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onGrandPrixSelected = viewModel::selectGrandPrix,
        onDriverSelected = viewModel::selectDriver,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrandPrixDetailScreen(
    uiState: GrandPrixDetailUiState,
    onBackClick: () -> Unit,
    onGrandPrixSelected: (String) -> Unit,
    onDriverSelected: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(uiState.circuitName.ifEmpty { "Grand Prix" }) },
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        "Loading race history across the last $GRAND_PRIX_HISTORY_YEARS seasons…",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
            uiState.loadErrorMessage != null -> Box(Modifier.padding(innerPadding).fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.loadErrorMessage, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) { Text("Retry") }
                }
            }
            else -> GrandPrixDetailContent(
                uiState = uiState,
                onGrandPrixSelected = onGrandPrixSelected,
                onDriverSelected = onDriverSelected,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun GrandPrixDetailContent(
    uiState: GrandPrixDetailUiState,
    onGrandPrixSelected: (String) -> Unit,
    onDriverSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(top = 16.dp)) {
                Text(
                    "Other Grand Prix this season",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                )
                GrandPrixPicker(uiState.otherGrandPrix, uiState.circuitId, onGrandPrixSelected)

                Text(
                    "Driver history (last $GRAND_PRIX_HISTORY_YEARS seasons)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                )
                if (uiState.selectedDriverId == null) {
                    Text(
                        "Pick a driver below to see their results here.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                } else if (uiState.history.isEmpty()) {
                    Text(
                        "${uiState.selectedDriverName} hasn't raced here in the last $GRAND_PRIX_HISTORY_YEARS years.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
        if (uiState.selectedDriverId != null && uiState.history.isNotEmpty()) {
            items(uiState.history, key = { it.season }) { row ->
                ListItem(
                    headlineContent = { Text(row.season.toString()) },
                    supportingContent = { Text(row.status) },
                    trailingContent = { Text("${row.positionText} · ${row.points.formatPoints()} pts") },
                )
            }
        }
        item {
            Text(
                "Select a driver",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
            )
        }
        items(uiState.drivers, key = { it.driverId }) { driver ->
            val isSelected = driver.driverId == uiState.selectedDriverId
            ListItem(
                headlineContent = { Text(driver.driverName) },
                trailingContent = if (isSelected) {
                    { Text("Selected", style = MaterialTheme.typography.labelMedium) }
                } else {
                    null
                },
                modifier = Modifier.clickable { onDriverSelected(driver.driverId) },
            )
        }
    }
}

@Composable
private fun GrandPrixPicker(
    options: List<GrandPrixOption>,
    selectedCircuitId: String,
    onSelected: (String) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(options, key = { it.circuitId }) { option ->
            val isSelected = option.circuitId == selectedCircuitId
            Box(
                modifier = Modifier
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { onSelected(option.circuitId) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    option.raceName,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun Double.formatPoints(): String =
    if (this == this.toLong().toDouble()) this.toLong().toString() else this.toString()

@Preview(showBackground = true)
@Composable
private fun GrandPrixDetailScreenPreview() {
    F1TrackerTheme {
        GrandPrixDetailScreen(
            uiState = GrandPrixDetailUiState(
                isLoading = false,
                circuitId = "bahrain",
                circuitName = "Bahrain International Circuit",
                otherGrandPrix = listOf(
                    GrandPrixOption("bahrain", "Bahrain Grand Prix"),
                    GrandPrixOption("jeddah", "Saudi Arabian Grand Prix"),
                ),
                drivers = listOf(
                    DriverOption("max_verstappen", "Max Verstappen"),
                    DriverOption("norris", "Lando Norris"),
                ),
                selectedDriverId = "max_verstappen",
                selectedDriverName = "Max Verstappen",
                history = listOf(
                    GrandPrixResultRow(2026, "6", 8.0, "Finished"),
                    GrandPrixResultRow(2025, "1", 26.0, "Finished"),
                ),
            ),
            onBackClick = {},
            onGrandPrixSelected = {},
            onDriverSelected = {},
            onRetry = {},
        )
    }
}
