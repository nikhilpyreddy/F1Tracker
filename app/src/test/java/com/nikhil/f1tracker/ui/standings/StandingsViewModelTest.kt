package com.nikhil.f1tracker.ui.standings

import com.nikhil.f1tracker.MainDispatcherRule
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorStandingEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.DriverStandingEntity
import com.nikhil.f1tracker.data.repository.fakes.FakeF1Repository
import java.time.Year
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StandingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val currentSeason = Year.now().value

    @Test
    fun `uiState exposes driver standings sorted by position with driver and team names resolved`() = runTest {
        // Arrange
        val repository = FakeF1Repository().apply {
            allDrivers.value = listOf(
                DriverEntity("norris", 4, "NOR", "Lando", "Norris", "1999-11-13", "British"),
                DriverEntity("max_verstappen", 3, "VER", "Max", "Verstappen", "1997-09-30", "Dutch"),
            )
            allConstructors.value = listOf(
                ConstructorEntity("mclaren", "McLaren", "British"),
                ConstructorEntity("red_bull", "Red Bull", "Austrian"),
            )
            allDriverStandings.value = listOf(
                DriverStandingEntity(currentSeason, "norris", "mclaren", 1, 374.0, 4),
                DriverStandingEntity(currentSeason, "max_verstappen", "red_bull", 2, 356.0, 8),
            )
        }
        val viewModel = StandingsViewModel(repository)

        // Act
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals(listOf("Lando Norris", "Max Verstappen"), state.driverStandings.map { it.driverName })
        assertEquals(listOf("McLaren", "Red Bull"), state.driverStandings.map { it.teamName })
    }

    @Test
    fun `switching mode exposes constructor standings`() = runTest {
        // Arrange
        val repository = FakeF1Repository().apply {
            allConstructors.value = listOf(ConstructorEntity("mclaren", "McLaren", "British"))
            allConstructorStandings.value = listOf(
                ConstructorStandingEntity(currentSeason, "mclaren", 1, 650.0, 12),
            )
        }
        val viewModel = StandingsViewModel(repository)
        viewModel.uiState.first { !it.isLoading }

        // Act
        viewModel.setMode(StandingsMode.TEAMS)
        val state = viewModel.uiState.first { it.mode == StandingsMode.TEAMS }

        // Assert
        assertEquals(1, state.constructorStandings.size)
        assertEquals("McLaren", state.constructorStandings.single().teamName)
    }

    @Test
    fun `refresh force-refreshes both standings sources`() = runTest {
        // Arrange
        val repository = FakeF1Repository()
        val viewModel = StandingsViewModel(repository)
        viewModel.uiState.first { !it.isLoading }

        // Act
        viewModel.refresh()
        viewModel.uiState.first { !it.isRefreshing }

        // Assert
        assertEquals(listOf(currentSeason, currentSeason), repository.syncedDriverStandingSeasons)
        assertEquals(listOf(currentSeason, currentSeason), repository.syncedConstructorStandingSeasons)
    }
}
