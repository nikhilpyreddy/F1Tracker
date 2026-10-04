package com.nikhil.f1tracker.ui.weekend

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nikhil.f1tracker.ui.common.identity.DriverAvatar
import com.nikhil.f1tracker.ui.common.identity.FinishBadge
import com.nikhil.f1tracker.ui.common.identity.driverColor
import com.nikhil.f1tracker.ui.common.identity.formatPoints
import com.nikhil.f1tracker.ui.common.identity.teamStripe

/** "Last time here": the most recent podium at this circuit, with a way into the full results. */
@Composable
fun LastPodiumCard(season: Int, podium: List<ClassificationRow>, onDriverClick: (String) -> Unit, onOpenResults: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Last time here · $season", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                TextButton(onClick = onOpenResults) { Text("Full results") }
            }
            podium.forEach { row -> ClassificationItem(row, onClick = { onDriverClick(row.driverId) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaceResultsSheet(
    sheet: ResultsSheet,
    raceName: String,
    onSeasonSelected: (Int) -> Unit,
    onDriverClick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            "$raceName · ${sheet.season}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(sheet.seasons) { season ->
                FilterChip(
                    selected = season == sheet.season,
                    onClick = { onSeasonSelected(season) },
                    label = { Text("$season") },
                )
            }
        }
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            items(sheet.rows, key = { it.driverId }) { row -> ClassificationItem(row, onClick = { onDriverClick(row.driverId) }) }
        }
    }
}

@Composable
private fun ClassificationItem(row: ClassificationRow, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).teamStripe(driverColor(row.driverId, row.constructorId))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FinishBadge(row.positionText, size = 32.dp)
        Spacer(Modifier.width(10.dp))
        DriverAvatar(row.driverId, row.constructorId, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(row.driverName, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${row.teamName} · " + (if (row.grid > 0) "started P${row.grid}" else "pit-lane start") +
                    if (row.positionText.toIntOrNull() == null) " · ${row.status}" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (row.points > 0) Text("${formatPoints(row.points)} pts", style = MaterialTheme.typography.labelMedium)
    }
}
