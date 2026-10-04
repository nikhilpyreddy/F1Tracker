package com.nikhil.f1tracker.ui.weekend.track

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhil.f1tracker.domain.stats.CornerSpeed
import com.nikhil.f1tracker.domain.stats.TrackPoint
import com.nikhil.f1tracker.domain.stats.TrackProfile
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val SLOW_COLOR = Color(0xFFD93025)
private val MID_COLOR = Color(0xFFF2B705)
private val FAST_COLOR = Color(0xFF2E9E5B)
private const val MAP_PADDING_FRACTION = 0.06f

@Composable
fun TrackTab(modifier: Modifier = Modifier, viewModel: TrackViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isLoading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.loadErrorMessage?.let { message ->
            item {
                Row {
                    Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::retry) { Text("Retry") }
                }
            }
        }
        state.circuitType?.let { type -> item { InfoCard(type.label, type.note) } }
        if (state.weather.isNotEmpty() || state.weatherNote != null) item { WeatherCard(state) }
        state.profile?.let { profile ->
            item { MapCard(profile, state.poleLapLabel) }
            item { ProfileCard(profile, state.raceLaps) }
        }
        if (!state.isLoading && state.profile == null && state.loadErrorMessage == null) {
            item { InfoCard("No telemetry yet", "OpenF1 has no qualifying data at this circuit (it covers 2023 onwards).") }
        }
        item {
            Text(
                "Telemetry: OpenF1 (openf1.org). Weather: Open-Meteo (open-meteo.com).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MapCard(profile: TrackProfile, label: String?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Speed map", style = MaterialTheme.typography.titleSmall)
            label?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TrackMap(profile.map, Modifier.fillMaxWidth().aspectRatio(1.2f).padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Legend(SLOW_COLOR, "${profile.minSpeedKph} km/h")
                Legend(MID_COLOR, "~${(profile.minSpeedKph + profile.topSpeedKph) / 2}")
                Legend(FAST_COLOR, "${profile.topSpeedKph} km/h")
            }
        }
    }
}

@Composable
private fun Legend(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(end = 4.dp).size(10.dp).background(color, CircleShape))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

/** The pole lap's racing line, coloured slow (red) to fast (green). */
@Composable
private fun TrackMap(points: List<TrackPoint>, modifier: Modifier = Modifier) {
    if (points.size < 2) return
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    val minSpeed = points.minOf { it.speedKph }
    val speedRange = (points.maxOf { it.speedKph } - minSpeed).coerceAtLeast(1)
    Canvas(modifier) {
        val pad = size.minDimension * MAP_PADDING_FRACTION
        val scale = minOf((size.width - 2 * pad) / (maxX - minX).coerceAtLeast(1f), (size.height - 2 * pad) / (maxY - minY).coerceAtLeast(1f))
        val offsetX = (size.width - (maxX - minX) * scale) / 2
        val offsetY = (size.height - (maxY - minY) * scale) / 2
        // Screen y grows downwards, track y upwards, so flip it.
        fun toScreen(p: TrackPoint) = Offset(offsetX + (p.x - minX) * scale, size.height - (offsetY + (p.y - minY) * scale))
        points.zipWithNext().forEach { (a, b) ->
            drawLine(speedColor((a.speedKph - minSpeed).toFloat() / speedRange), toScreen(a), toScreen(b), strokeWidth = 10f, cap = StrokeCap.Round)
        }
    }
}

private fun speedColor(fraction: Float): Color =
    if (fraction < 0.5f) lerp(SLOW_COLOR, MID_COLOR, fraction * 2) else lerp(MID_COLOR, FAST_COLOR, (fraction - 0.5f) * 2)

@Composable
private fun ProfileCard(profile: TrackProfile, raceLaps: Int?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Track profile", style = MaterialTheme.typography.titleSmall)
            Fact("Lap length", "%.2f km".format(Locale.US, profile.lapLengthMeters / 1000.0) + (raceLaps?.let { " · $it laps" } ?: ""))
            Fact("Full throttle", "${(profile.fullThrottleShare * 100).roundToInt()}% of the lap")
            Fact(
                "Longest flat-out run",
                "%.1f s · ~%d m".format(Locale.US, profile.longestFullThrottleSeconds, profile.longestFullThrottleMeters),
            )
            Fact("Top speed / slowest point", "${profile.topSpeedKph} / ${profile.minSpeedKph} km/h")
            Fact(
                "Braking corners",
                CornerSpeed.entries.joinToString(" · ") { "${profile.cornerCount(it)} ${it.label.lowercase()}" },
            )
            Fact("Braking zones", "${profile.brakingZones}")
            Text(
                "Slow < 130 km/h ≤ medium < 210 km/h ≤ fast. Flat-out kinks aren't braking corners. " +
                    "From ~4 telemetry samples per second, so speeds are approximate.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeatherCard(state: TrackUiState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Race weekend forecast", style = MaterialTheme.typography.titleSmall)
            Text("Days are local to the circuit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            state.weather.forEach { day ->
                Fact(
                    day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    listOfNotNull(
                        day.rainChance?.let { "🌧 $it%" },
                        day.maxTemperature?.let { "${it.roundToInt()}°C" },
                        day.maxWind?.let { "wind ${it.roundToInt()} km/h" },
                    ).joinToString(" · "),
                )
            }
            state.weatherNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall)
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
