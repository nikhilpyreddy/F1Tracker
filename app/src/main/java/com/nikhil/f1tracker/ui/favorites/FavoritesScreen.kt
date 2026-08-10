package com.nikhil.f1tracker.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.domain.model.MAX_FAVORITE_DRIVERS
import com.nikhil.f1tracker.domain.model.MAX_FAVORITE_TEAMS
import com.nikhil.f1tracker.ui.theme.F1TrackerTheme

@Composable
fun FavoritesRoute(viewModel: FavoritesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FavoritesScreen(
        uiState = uiState,
        onDriverClick = viewModel::toggleDriver,
        onTeamClick = viewModel::toggleTeam,
        onRetry = viewModel::retryRosterSync,
        onLimitMessageShown = viewModel::limitMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    uiState: FavoritesUiState,
    onDriverClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    onRetry: () -> Unit,
    onLimitMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.limitReachedMessage) {
        uiState.limitReachedMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onLimitMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("Favorites") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(innerPadding))
            uiState.loadErrorMessage != null ->
                ErrorState(uiState.loadErrorMessage, onRetry, Modifier.padding(innerPadding))
            else -> FavoritesContent(
                uiState = uiState,
                onDriverClick = onDriverClick,
                onTeamClick = onTeamClick,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun FavoritesContent(
    uiState: FavoritesUiState,
    onDriverClick: (String) -> Unit,
    onTeamClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { SectionHeader("Favorite drivers (${uiState.selection.driverIds.size}/$MAX_FAVORITE_DRIVERS)") }
        items(uiState.drivers, key = { it.driverId }) { driver ->
            FavoriteRow(
                title = "${driver.givenName} ${driver.familyName}",
                subtitle = driver.code,
                isSelected = driver.driverId in uiState.selection.driverIds,
                onClick = { onDriverClick(driver.driverId) },
            )
        }
        item { SectionHeader("Favorite teams (${uiState.selection.teamIds.size}/$MAX_FAVORITE_TEAMS)") }
        items(uiState.teams, key = { it.constructorId }) { team ->
            FavoriteRow(
                title = team.name,
                subtitle = team.nationality,
                isSelected = team.constructorId in uiState.selection.teamIds,
                onClick = { onTeamClick(team.constructorId) },
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun FavoriteRow(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Checkbox(checked = isSelected, onCheckedChange = { onClick() }) },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@Preview(showBackground = true)
@Composable
private fun FavoritesScreenPreview() {
    F1TrackerTheme {
        FavoritesScreen(
            uiState = FavoritesUiState(
                isLoading = false,
                drivers = listOf(
                    DriverEntity("max_verstappen", 3, "VER", "Max", "Verstappen", "1997-09-30", "Dutch"),
                    DriverEntity("norris", 4, "NOR", "Lando", "Norris", "1999-11-13", "British"),
                ),
                teams = listOf(
                    ConstructorEntity("red_bull", "Red Bull", "Austrian"),
                    ConstructorEntity("mclaren", "McLaren", "British"),
                ),
                selection = com.nikhil.f1tracker.domain.model.FavoriteSelection(
                    driverIds = setOf("max_verstappen"),
                    teamIds = setOf("red_bull"),
                ),
            ),
            onDriverClick = {},
            onTeamClick = {},
            onRetry = {},
            onLimitMessageShown = {},
        )
    }
}
