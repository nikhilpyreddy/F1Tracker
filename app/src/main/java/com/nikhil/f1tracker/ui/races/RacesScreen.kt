package com.nikhil.f1tracker.ui.races

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhil.f1tracker.domain.model.RaceStatus
import com.nikhil.f1tracker.domain.model.formatRaceWhen
import com.nikhil.f1tracker.ui.common.identity.DriverCodeBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RacesRoute(
    onRaceClick: (season: Int, round: Int, circuitId: String) -> Unit,
    viewModel: RacesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    // Land on the next race (or the season's end) rather than round 1.
    LaunchedEffect(state.season, state.races.size) {
        val next = state.races.indexOfFirst { it.status == RaceStatus.NEXT }
        if (next > 0) listState.scrollToItem((next - 1).coerceAtLeast(0))
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Races") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.seasons) { season ->
                    FilterChip(selected = season == state.season, onClick = { viewModel.selectSeason(season) }, label = { Text("$season") })
                }
            }
            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.loadErrorMessage?.let { message ->
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::retry) { Text("Retry") }
                }
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(state.races, key = { "${it.season}-${it.round}" }) { race ->
                    RaceItem(race, onClick = { onRaceClick(race.season, race.round, race.circuitId) })
                }
            }
        }
    }
}

@Composable
private fun RaceItem(race: RaceRow, onClick: () -> Unit) {
    val isNext = race.status == RaceStatus.NEXT
    val dim = race.status == RaceStatus.COMPLETED && race.podium.isEmpty()
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("R${race.round}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(40.dp))
        Column(Modifier.weight(1f)) {
            Text(
                race.raceName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                color = if (dim) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            Text(formatRaceWhen(race.date, race.time), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when {
            race.podium.isNotEmpty() -> Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                race.podium.forEach { DriverCodeBadge(it.driverId, it.constructorId) }
            }
            isNext -> Text("Next", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}
