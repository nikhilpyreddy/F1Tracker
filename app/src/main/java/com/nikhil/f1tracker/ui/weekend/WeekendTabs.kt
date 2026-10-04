package com.nikhil.f1tracker.ui.weekend

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikhil.f1tracker.domain.stats.CircuitStats
import com.nikhil.f1tracker.domain.stats.Rate
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CircuitTab(uiState: WeekendUiState, onCircuitHistoryClick: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize()) {
        val stats = uiState.circuitStats
        item {
            Text(
                text = stats?.let { "Base rates from the last ${it.raceCount} races here (${it.seasonRange()})" }
                    ?: if (uiState.isLoading) "Loading circuit history…" else "No recent race history at this circuit.",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(16.dp),
            )
        }
        if (stats != null) {
            item { RateRow("Won from pole", stats.winsFromPole, "races") }
            item { RateRow("Won from the front row", stats.winsFromFrontRow, "races") }
            item { RateRow("Won from the top 3", stats.winsFromTopThree, "races") }
            item {
                ValueRow("Winner's average grid", stats.winnerAverageGrid?.let { "P%.1f".format(Locale.US, it) })
            }
            item { RateRow("Podium finishers who started top 3", stats.podiumsFromTopThreeGrid, "podiums") }
            item {
                ValueRow(
                    "Top-10 finishers from outside the top-10 grid",
                    stats.topTenFinishersFromOutsideTopTenPerRace?.let { "%.1f per race".format(Locale.US, it) },
                )
            }
            item { RateRow("Retirements", stats.retirements, "starters") }
        }
        item {
            OutlinedButton(onClick = onCircuitHistoryClick, modifier = Modifier.padding(16.dp)) {
                Text("Driver history at this circuit")
            }
        }
    }
}

@Composable
fun FormTab(uiState: WeekendUiState, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize()) {
        if (uiState.driverForms.isEmpty()) {
            item {
                Text(
                    if (uiState.isLoading) "Loading season results…" else "No results yet this season.",
                    modifier = Modifier.padding(16.dp),
                )
            }
            return@LazyColumn
        }
        item { SectionTitle("${uiState.season} form · last 5 races, newest first") }
        items(uiState.driverForms, key = { it.driverId }) { DriverFormItem(it) }
        if (uiState.headToHeads.isNotEmpty()) {
            item { SectionTitle("Teammate head-to-head") }
            items(uiState.headToHeads, key = { "${it.teamName}-${it.firstDriverName}-${it.secondDriverName}" }) {
                HeadToHeadItem(it)
            }
        }
    }
}

@Composable
private fun DriverFormItem(row: DriverFormRow) {
    ListItem(
        headlineContent = { Text(row.driverName) },
        supportingContent = {
            Column {
                Text("Finish  ${row.recent.joinToString("  ") { it.positionText.padStart(2) }}", fontFamily = FontFamily.Monospace)
                Text("Grid    ${row.recent.joinToString("  ") { gridLabel(it.grid).padStart(2) }}", fontFamily = FontFamily.Monospace)
            }
        },
        overlineContent = {
            Text("${row.teamName} · ${formatPoints(row.seasonPoints)} pts · DNF ${row.retirements.hits}/${row.retirements.total}")
        },
    )
}

@Composable
private fun HeadToHeadItem(row: HeadToHeadRow) {
    ListItem(
        overlineContent = { Text(row.teamName) },
        headlineContent = { Text("${row.firstDriverName} vs ${row.secondDriverName}") },
        supportingContent = {
            Text("Grid ${row.gridWins.first}–${row.gridWins.second} · Finish ${row.finishWins.first}–${row.finishWins.second}")
        },
    )
}

@Composable
private fun RateRow(label: String, rate: Rate, unit: String) {
    ValueRow(label, rate.fraction?.let { "${(it * 100).roundToInt()}%" }, "${rate.hits} of ${rate.total} $unit")
}

@Composable
private fun ValueRow(label: String, value: String?, detail: String? = null) {
    ListItem(
        headlineContent = { Text(label) },
        supportingContent = detail?.let { { Text(it) } },
        trailingContent = {
            Text(value ?: "—", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
    )
    HorizontalDivider()
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

private fun CircuitStats.seasonRange(): String =
    if (seasons.size == 1) "${seasons.first()}" else "${seasons.first()}–${seasons.last()}"

private fun gridLabel(grid: Int): String = if (grid > 0) "$grid" else "PL"

private fun formatPoints(points: Double): String =
    if (points == points.toLong().toDouble()) points.toLong().toString() else points.toString()
