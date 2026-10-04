package com.nikhil.f1tracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nikhil.f1tracker.domain.model.formatCountdown
import com.nikhil.f1tracker.domain.model.formatDateTime
import com.nikhil.f1tracker.domain.model.formatRaceWhen
import java.time.Instant
import kotlinx.coroutines.delay

private const val COUNTDOWN_TICK_MILLIS = 60_000L

@Composable
fun NextRaceCard(nextRace: UpcomingRace?, onClick: (UpcomingRace) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp)
            .let { if (nextRace != null) it.clickable { onClick(nextRace) } else it },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Next race", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                nextRace?.raceStart?.let { Countdown(it) }
            }
            Text(nextRace?.raceName ?: "Season complete", style = MaterialTheme.typography.titleLarge)
            if (nextRace == null) return@Column
            Text("Round ${nextRace.round}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val now = Instant.now()
            nextRace.sprintStart?.let { SessionRow("Sprint", formatDateTime(it), isPast = it < now) }
            nextRace.qualifyingStart?.let { SessionRow("Qualifying", formatDateTime(it), isPast = it < now) }
            SessionRow(
                "Race",
                nextRace.raceStart?.let(::formatDateTime) ?: formatRaceWhen(nextRace.date, null),
                isPast = false,
            )
        }
    }
}

/** Ticks once a minute so "in 2d 5h" stays accurate while the screen is open. */
@Composable
private fun Countdown(start: Instant) {
    val label by produceState(formatCountdown(Instant.now(), start), start) {
        while (true) {
            delay(COUNTDOWN_TICK_MILLIS)
            value = formatCountdown(Instant.now(), start)
        }
    }
    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun SessionRow(label: String, time: String, isPast: Boolean) {
    val color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.width(96.dp))
        Text(time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}
