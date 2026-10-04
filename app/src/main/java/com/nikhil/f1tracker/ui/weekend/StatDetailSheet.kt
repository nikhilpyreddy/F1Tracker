package com.nikhil.f1tracker.ui.weekend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikhil.f1tracker.domain.stats.StatDetailLine
import com.nikhil.f1tracker.domain.stats.StatDetailRace
import com.nikhil.f1tracker.ui.common.identity.DriverAvatar
import com.nikhil.f1tracker.ui.common.identity.finishColor
import com.nikhil.f1tracker.ui.common.identity.onColor

private val HIT_GREEN = Color(0xFF2E9E5B)
private val MISS_GREY = Color(0xFF5F6368)
private val PILL_SHAPE = RoundedCornerShape(4.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatDetailSheet(sheet: StatSheet, onDismiss: () -> Unit, onDriverClick: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            sheet.stat.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Text(
            "Race by race, newest first",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            items(sheet.races, key = { "${it.season}-${it.round}" }) { race ->
                RaceBlock(race, sheet.driverNames, onDriverClick)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RaceBlock(race: StatDetailRace, names: Map<String, String>, onDriverClick: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${race.season}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            race.counts?.let { HitLabel(it) }
        }
        if (race.lines.isEmpty()) {
            Text("None", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
            race.lines.forEach { line ->
                DetailLine(line, names[line.driverId] ?: line.driverId, onClick = { onDriverClick(line.driverId) })
            }
        }
    }
}

@Composable
private fun DetailLine(line: StatDetailLine, name: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        DriverAvatar(line.driverId, line.constructorId, size = 32.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyMedium)
            Text(
                buildString {
                    append(if (line.grid > 0) "Started P${line.grid}" else "Pit-lane start")
                    if (line.positionText.toIntOrNull() == null) append(" · ${line.status}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FinishPill(line.positionText)
    }
}

@Composable
private fun FinishPill(positionText: String) {
    val color = finishColor(positionText)
    Text(
        if (positionText.toIntOrNull() != null) "P$positionText" else "DNF",
        color = color.onColor(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(start = 8.dp)
            .background(color, PILL_SHAPE)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun HitLabel(counts: Boolean) {
    val color = if (counts) HIT_GREEN else MISS_GREY
    Text(
        if (counts) "Counts" else "Doesn't count",
        color = color.onColor(),
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.background(color, PILL_SHAPE).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
