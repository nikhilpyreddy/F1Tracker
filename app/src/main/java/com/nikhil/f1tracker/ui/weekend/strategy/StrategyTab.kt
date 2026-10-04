package com.nikhil.f1tracker.ui.weekend.strategy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhil.f1tracker.domain.stats.DriverStrategy
import com.nikhil.f1tracker.domain.stats.RaceStrategy
import com.nikhil.f1tracker.domain.stats.Rate
import com.nikhil.f1tracker.domain.stats.StrategySummary
import com.nikhil.f1tracker.ui.common.identity.CodeBadge
import com.nikhil.f1tracker.ui.common.identity.driverColor
import com.nikhil.f1tracker.ui.common.identity.finishColor
import com.nikhil.f1tracker.ui.common.identity.onColor
import java.util.Locale
import kotlin.math.roundToInt

private val COMPOUND_COLORS = mapOf(
    "SOFT" to Color(0xFFE8002D),
    "MEDIUM" to Color(0xFFFFD12E),
    "HARD" to Color(0xFFF0F0EC),
    "INTERMEDIATE" to Color(0xFF43B02A),
    "WET" to Color(0xFF0067AD),
)
private val UNKNOWN_COMPOUND = Color(0xFF888888)

private fun compoundColor(compound: String) = COMPOUND_COLORS[compound] ?: UNKNOWN_COMPOUND

@Composable
fun StrategyTab(modifier: Modifier = Modifier, viewModel: StrategyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = state.summary
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp)) {
        if (state.isLoading) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        "Loading past races from OpenF1 (about 10 s each the first time)…",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        state.loadErrorMessage?.let { message ->
            item {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::retry) { Text("Retry") }
                }
            }
        }
        if (summary == null) {
            if (!state.isLoading && state.loadErrorMessage == null) {
                item { Text("No OpenF1 race data at this circuit (it covers 2023 onwards).", Modifier.padding(16.dp)) }
            }
            return@LazyColumn
        }
        item { OverviewCard(summary) }
        item { CommonStrategiesCard(summary) }
        item { CompoundCard(summary) }
        summary.races.forEach { race ->
            item(key = "header-${race.year}") { RaceHeader(race) }
            items(race.drivers, key = { "${race.year}-${it.code}" }) { driver ->
                StintRow(driver, totalLaps = race.drivers.maxOfOrNull { d -> d.stints.sumOf { it.laps } } ?: 1)
            }
        }
        item {
            Text(
                "Tyre, incident and overtake data: OpenF1 (openf1.org).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun OverviewCard(summary: StrategySummary) {
    SectionCard("At this circuit · ${summary.races.size} races since ${summary.races.minOf { it.year }}") {
        Fact("Safety Car", rateText(summary.racesWithSafetyCar))
        Fact("Virtual Safety Car", rateText(summary.racesWithVirtualSafetyCar))
        Fact("Red flag", rateText(summary.racesWithRedFlag))
        summary.averageOvertakes?.let { Fact("Overtakes per race", "%.0f".format(Locale.US, it)) }
        summary.races.mapNotNull { it.medianPitLaneSeconds }.takeIf { it.isNotEmpty() }?.let {
            Fact("Pit lane time (median)", "%.1f s".format(Locale.US, it.average()))
        }
        summary.medianFirstStopLap?.let { Fact("Typical first stop", "Lap $it") }
        Fact(
            "Stops (finishers)",
            summary.stopShares.entries.joinToString(" · ") { (stops, share) -> "$stops: ${(share * 100).roundToInt()}%" },
        )
    }
}

@Composable
private fun CommonStrategiesCard(summary: StrategySummary) {
    SectionCard("Most common strategies (finishers)") {
        summary.commonStrategies.forEach { strategy ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                    strategy.sequence.split("–").forEach { letter -> CompoundDot(letter) }
                    Text(
                        "  ${strategy.stops}-stop",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${strategy.drivers} drivers" + if (strategy.winners > 0) " · ${strategy.winners} win" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CompoundCard(summary: StrategySummary) {
    SectionCard("Stint length by compound") {
        summary.compoundStints.forEach { compound ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CompoundDot(compound.compound.take(1))
                Text(
                    " ${compound.compound.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "avg %.0f laps · max %d".format(Locale.US, compound.averageLaps, compound.longestLaps),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun RaceHeader(race: RaceStrategy) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)) {
        Text("${race.year}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            listOfNotNull(
                "SC ${race.safetyCars}",
                "VSC ${race.virtualSafetyCars}",
                "Red ${race.redFlags}",
                "${race.overtakes} overtakes",
                race.medianPitLaneSeconds?.let { "pit lane %.1f s".format(Locale.US, it) },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Finish, driver badge, then the race's stints as a bar proportional to laps. */
@Composable
private fun StintRow(driver: DriverStrategy, totalLaps: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val position = driver.positionText
        val chip = position?.let { finishColor(it) } ?: UNKNOWN_COMPOUND
        Box(Modifier.width(30.dp).background(chip, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
            Text(position ?: "–", color = chip.onColor(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        CodeBadge(driver.code, driver.driverId?.let { driverColor(it) } ?: UNKNOWN_COMPOUND, Modifier.padding(horizontal = 6.dp).width(46.dp))
        Row(Modifier.weight(1f).height(16.dp)) {
            driver.stints.forEach { stint ->
                Box(
                    Modifier.weight(stint.laps.toFloat().coerceAtLeast(1f)).height(16.dp)
                        .padding(end = 1.dp).background(compoundColor(stint.compound), RoundedCornerShape(2.dp)),
                )
            }
            val missing = totalLaps - driver.stints.sumOf { it.laps }
            if (missing > 0) Box(Modifier.weight(missing.toFloat()).height(16.dp))
        }
        Text(
            "${driver.stops}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(20.dp).padding(start = 6.dp),
        )
    }
}

@Composable
private fun CompoundDot(letter: String) {
    val compound = COMPOUND_COLORS.keys.firstOrNull { it.startsWith(letter) } ?: ""
    val color = compoundColor(compound)
    Box(
        Modifier.size(18.dp).background(color, RoundedCornerShape(9.dp)).border(1.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, color = color.onColor(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

private fun rateText(rate: Rate): String =
    rate.fraction?.let { "${(it * 100).roundToInt()}% · ${rate.hits} of ${rate.total} races" } ?: "—"
