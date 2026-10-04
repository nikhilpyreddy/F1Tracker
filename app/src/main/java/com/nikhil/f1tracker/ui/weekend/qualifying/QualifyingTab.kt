package com.nikhil.f1tracker.ui.weekend.qualifying

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhil.f1tracker.domain.stats.PoleRecord
import com.nikhil.f1tracker.domain.stats.QualifyingForm
import com.nikhil.f1tracker.domain.stats.QualifyingLine
import com.nikhil.f1tracker.domain.stats.TeammateHeadToHead
import com.nikhil.f1tracker.ui.common.identity.DriverAvatar
import com.nikhil.f1tracker.ui.common.identity.FinishBadge
import com.nikhil.f1tracker.ui.common.identity.SplitBar
import com.nikhil.f1tracker.ui.common.identity.TeamDot
import com.nikhil.f1tracker.ui.common.identity.driverColor
import com.nikhil.f1tracker.ui.common.identity.teamColor
import com.nikhil.f1tracker.ui.common.identity.teamStripe
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun QualifyingTab(onDriverClick: (String) -> Unit, modifier: Modifier = Modifier, viewModel: QualifyingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val name = { id: String -> state.driverNames[id] ?: id }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.loadErrorMessage?.let { item { ErrorRow(it, viewModel::refresh) } }

        item {
            Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Title("This weekend's qualifying", Modifier.weight(1f))
                TextButton(onClick = viewModel::refresh) { Text("Refresh") }
            }
        }
        if (state.weekend.isEmpty()) {
            item { Note("Not run yet. Results appear here shortly after the session (tap Refresh).") }
        } else {
            items(state.weekend, key = { "q-${it.driverId}" }) { line ->
                QualifyingLineRow(line, name(line.driverId), onClick = { onDriverClick(line.driverId) })
            }
        }

        if (state.poles.isNotEmpty()) {
            item {
                Title("Pole history here", Modifier.padding(start = 16.dp, top = 20.dp))
                state.poleToWin.fraction?.let { fraction ->
                    Note("Pole converted to the win ${(fraction * 100).roundToInt()}% · ${state.poleToWin.hits} of ${state.poleToWin.total}")
                }
            }
            items(state.poles, key = { "p-${it.season}" }) { pole -> PoleRow(pole, name(pole.driverId), onClick = { onDriverClick(pole.driverId) }) }
        }

        if (state.forms.isNotEmpty()) {
            item { Title("${state.season} qualifying form · newest first", Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)) }
            items(state.forms, key = { "f-${it.driverId}" }) { form ->
                FormRow(form, name(form.driverId), onClick = { onDriverClick(form.driverId) })
            }
        }

        if (state.headToHeads.isNotEmpty()) {
            item { Title("Teammate qualifying head-to-head", Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)) }
            items(state.headToHeads, key = { "h-${it.constructorId}-${it.firstDriverId}-${it.secondDriverId}" }) { h2h ->
                HeadToHeadRow(h2h, name, state.teamNames[h2h.constructorId] ?: h2h.constructorId)
            }
        }
    }
}

@Composable
private fun QualifyingLineRow(line: QualifyingLine, driverName: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).teamStripe(driverColor(line.driverId, line.constructorId))
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FinishBadge("${line.position}", size = 30.dp)
        Spacer(Modifier.width(10.dp))
        DriverAvatar(line.driverId, line.constructorId, size = 32.dp)
        Text(" $driverName", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(line.time ?: "—", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            Text(
                "${line.segment} " + (line.gapToPole?.let { if (it == 0.0) "pole" else "+%.3f".format(Locale.US, it) } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PoleRow(pole: PoleRecord, driverName: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).teamStripe(driverColor(pole.driverId, pole.constructorId))
            .padding(horizontal = 16.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${pole.season}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(44.dp))
        DriverAvatar(pole.driverId, pole.constructorId, size = 32.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(driverName, style = MaterialTheme.typography.bodyMedium)
            pole.poleTime?.let { Text(it, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace) }
        }
        Text("Race ", style = MaterialTheme.typography.labelSmall)
        FinishBadge(pole.racePositionText ?: "–", size = 28.dp)
    }
}

@Composable
private fun FormRow(form: QualifyingForm, driverName: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).teamStripe(driverColor(form.driverId, form.constructorId))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DriverAvatar(form.driverId, form.constructorId, size = 36.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(driverName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "Avg P%.1f · poles %d · front row %d · Q3 %d/%d".format(
                    Locale.US, form.averagePosition, form.poles, form.frontRows, form.q3Appearances.hits, form.q3Appearances.total,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                form.recentPositions.forEach { FinishBadge("$it", size = 24.dp) }
            }
        }
    }
}

@Composable
private fun HeadToHeadRow(h2h: TeammateHeadToHead, name: (String) -> String, teamName: String) {
    val color = teamColor(h2h.constructorId)
    Column(Modifier.fillMaxWidth().teamStripe(color).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TeamDot(h2h.constructorId)
            Text("  $teamName · ${name(h2h.firstDriverId)} vs ${name(h2h.secondDriverId)}", style = MaterialTheme.typography.bodyMedium)
        }
        SplitBar("Quali", h2h.gridWins.first, h2h.gridWins.second, color, Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun Title(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = modifier)
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}

@Composable
private fun ErrorRow(message: String, onRetry: () -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}
