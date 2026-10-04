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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.nikhil.f1tracker.ui.common.identity.DriverAvatar
import com.nikhil.f1tracker.ui.common.identity.FinishBadge
import com.nikhil.f1tracker.ui.common.identity.LocalF1Identities
import com.nikhil.f1tracker.ui.common.identity.TeamDot
import com.nikhil.f1tracker.ui.common.identity.formatPoints

private const val UNSELECTED_ALPHA = 0.45f

/** Pick a current driver to see every result they've had at this circuit in the history window. */
@Composable
fun DriverHistoryCard(uiState: WeekendUiState, onDriverSelected: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp)) {
            Text("Driver history here", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.historyDrivers, key = { it.driverId }) { driver ->
                    val isSelected = driver.driverId == uiState.selectedHistoryDriverId
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onDriverSelected(driver.driverId) }
                            .alpha(if (isSelected || uiState.selectedHistoryDriverId == null) 1f else UNSELECTED_ALPHA),
                    ) {
                        DriverAvatar(driver.driverId, size = if (isSelected) 48.dp else 40.dp)
                        Text(
                            LocalF1Identities.current.driver(driver.driverId)?.code ?: driver.name.takeLast(3).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            val selected = uiState.historyDrivers.find { it.driverId == uiState.selectedHistoryDriverId }
            when {
                selected == null -> Hint("Tap a driver to see their results at this circuit.")
                uiState.driverHistory.isEmpty() -> Hint("${selected.name} hasn't raced here in the last 10 seasons.")
                else -> uiState.driverHistory.forEach { HistoryRow(it) }
            }
        }
    }
}

@Composable
private fun HistoryRow(result: DriverCircuitResult) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${result.season}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(44.dp))
        FinishBadge(result.positionText, size = 30.dp)
        Spacer(Modifier.width(10.dp))
        TeamDot(result.constructorId)
        Text(
            "  " + (if (result.grid > 0) "Started P${result.grid}" else "Pit-lane start") +
                if (result.positionText.toIntOrNull() == null) " · ${result.status}" else "",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        if (result.points > 0) Text("${formatPoints(result.points)} pts", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
