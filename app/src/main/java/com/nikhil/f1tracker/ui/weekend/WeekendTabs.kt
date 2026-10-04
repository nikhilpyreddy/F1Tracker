package com.nikhil.f1tracker.ui.weekend

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikhil.f1tracker.domain.stats.CircuitStat
import com.nikhil.f1tracker.domain.stats.CircuitStats
import com.nikhil.f1tracker.domain.stats.Rate
import com.nikhil.f1tracker.ui.common.identity.DriverAvatar
import com.nikhil.f1tracker.ui.common.identity.FormStrip
import com.nikhil.f1tracker.ui.common.identity.FractionBar
import com.nikhil.f1tracker.ui.common.identity.SplitBar
import com.nikhil.f1tracker.ui.common.identity.TeamDot
import com.nikhil.f1tracker.ui.common.identity.driverColor
import com.nikhil.f1tracker.ui.common.identity.formatPoints
import com.nikhil.f1tracker.ui.common.identity.teamColor
import com.nikhil.f1tracker.ui.common.identity.teamStripe
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CircuitTab(
    uiState: WeekendUiState,
    onStatClick: (CircuitStat) -> Unit,
    onCircuitHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = uiState.circuitStats
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = stats?.let { "Last ${it.raceCount} races here (${it.seasonRange()}) · tap a stat for the races" }
                    ?: if (uiState.isLoading) "Loading circuit history…" else "No recent race history at this circuit.",
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (stats != null) {
            items(statCards(stats), key = { it.stat }) { card -> StatCard(card, onClick = { onStatClick(card.stat) }) }
        }
        item {
            OutlinedButton(onClick = onCircuitHistoryClick) { Text("Driver history at this circuit") }
        }
    }
}

private data class StatCardModel(val stat: CircuitStat, val value: String, val detail: String?, val fraction: Float?)

private fun statCards(stats: CircuitStats): List<StatCardModel> = listOf(
    rateCard(CircuitStat.WINS_FROM_POLE, stats.winsFromPole, "races"),
    rateCard(CircuitStat.WINS_FROM_FRONT_ROW, stats.winsFromFrontRow, "races"),
    rateCard(CircuitStat.WINS_FROM_TOP_THREE, stats.winsFromTopThree, "races"),
    StatCardModel(
        CircuitStat.WINNER_GRID,
        stats.winnerAverageGrid?.let { "P%.1f".format(Locale.US, it) } ?: "—",
        "Where winners started, on average",
        null,
    ),
    rateCard(CircuitStat.PODIUM_STARTS, stats.podiumsFromTopThreeGrid, "podiums"),
    StatCardModel(
        CircuitStat.TOP_TEN_FROM_OUTSIDE,
        stats.topTenFinishersFromOutsideTopTenPerRace?.let { "%.1f".format(Locale.US, it) } ?: "—",
        "Per race, on average",
        null,
    ),
    rateCard(CircuitStat.RETIREMENTS, stats.retirements, "starters"),
)

private fun rateCard(stat: CircuitStat, rate: Rate, unit: String) = StatCardModel(
    stat = stat,
    value = rate.fraction?.let { "${(it * 100).roundToInt()}%" } ?: "—",
    detail = "${rate.hits} of ${rate.total} $unit",
    fraction = rate.fraction?.toFloat(),
)

@Composable
private fun StatCard(card: StatCardModel, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(card.stat.title, style = MaterialTheme.typography.bodyMedium)
                card.detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                card.fraction?.let {
                    FractionBar(it, MaterialTheme.colorScheme.primary, Modifier.padding(top = 8.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(card.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Show races")
        }
    }
}

@Composable
fun FormTab(uiState: WeekendUiState, onDriverClick: (String) -> Unit, modifier: Modifier = Modifier) {
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
        items(uiState.driverForms, key = { it.driverId }) { DriverFormItem(it, onClick = { onDriverClick(it.driverId) }) }
        if (uiState.headToHeads.isNotEmpty()) {
            item { SectionTitle("Teammate head-to-head · solid = first named") }
            items(uiState.headToHeads, key = { "${it.constructorId}-${it.firstDriverId}-${it.secondDriverId}" }) {
                HeadToHeadItem(it)
            }
        }
    }
}

@Composable
private fun DriverFormItem(row: DriverFormRow, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).teamStripe(driverColor(row.driverId, row.constructorId))
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DriverAvatar(row.driverId, row.constructorId, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.driverName, style = MaterialTheme.typography.titleSmall)
            Text(
                "${row.teamName} · ${formatPoints(row.seasonPoints)} pts · DNF ${row.retirements.hits}/${row.retirements.total}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FormStrip(row.recent.map { it.positionText to it.grid }, Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun HeadToHeadItem(row: HeadToHeadRow) {
    val color = teamColor(row.constructorId)
    Column(
        Modifier.fillMaxWidth().teamStripe(color).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamDot(row.constructorId)
            Spacer(Modifier.width(8.dp))
            Text(row.teamName, style = MaterialTheme.typography.labelLarge)
        }
        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            DriverAvatar(row.firstDriverId, row.constructorId, size = 28.dp)
            Text(
                " ${row.firstDriverName}  vs  ${row.secondDriverName} ",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            DriverAvatar(row.secondDriverId, row.constructorId, size = 28.dp)
        }
        SplitBar("Grid", row.gridWins.first, row.gridWins.second, color)
        SplitBar("Finish", row.finishWins.first, row.finishWins.second, color, Modifier.padding(top = 4.dp))
    }
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
