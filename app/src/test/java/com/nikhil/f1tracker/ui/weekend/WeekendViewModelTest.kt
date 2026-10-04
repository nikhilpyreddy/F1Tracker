package com.nikhil.f1tracker.ui.weekend

import androidx.lifecycle.SavedStateHandle
import com.nikhil.f1tracker.MainDispatcherRule
import com.nikhil.f1tracker.data.local.entity.CircuitEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity
import com.nikhil.f1tracker.data.repository.fakes.FakeF1Repository
import com.nikhil.f1tracker.domain.stats.Rate
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class WeekendViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun result(season: Int, round: Int, driverId: String, constructorId: String, grid: Int, position: Int, points: Double = 0.0) =
        ResultEntity(
            season = season, round = round, driverId = driverId, constructorId = constructorId,
            position = position, positionText = "$position", points = points, grid = grid, laps = 50,
            status = "Finished", finishTimeMillis = null, fastestLapRank = null, fastestLapTime = null,
        )

    private fun repository() = FakeF1Repository().apply {
        allDrivers.value = listOf(
            DriverEntity("max_verstappen", 3, "VER", "Max", "Verstappen", null, null),
            DriverEntity("tsunoda", 22, "TSU", "Yuki", "Tsunoda", null, null),
        )
        allConstructors.value = listOf(ConstructorEntity("red_bull", "Red Bull", "Austrian"))
        allCircuits.value = listOf(CircuitEntity("marina_bay", "Marina Bay Street Circuit", "Singapore", "Singapore", 1.29, 103.86))
        allRaces.value = listOf(
            RaceEntity(2010, 15, "Singapore Grand Prix", "marina_bay", "2010-09-26", null),
            RaceEntity(2024, 18, "Singapore Grand Prix", "marina_bay", "2024-09-22", null),
            RaceEntity(2025, 18, "Singapore Grand Prix", "marina_bay", "2025-10-05", null),
            RaceEntity(2026, 1, "Australian Grand Prix", "albert_park", "2026-03-08", null),
            RaceEntity(2026, 17, "Singapore Grand Prix", "marina_bay", "2026-10-11", null),
        )
        allResults.value = listOf(
            // Outside the 10-season window: must not count.
            result(2010, 15, "alonso", "ferrari", grid = 1, position = 1),
            // In the window: winner from P2, then from pole.
            result(2024, 18, "norris", "mclaren", grid = 2, position = 1),
            result(2025, 18, "russell", "mercedes", grid = 1, position = 1),
            // This season's form (a different circuit).
            result(2026, 1, "max_verstappen", "red_bull", grid = 1, position = 2, points = 18.0),
            result(2026, 1, "tsunoda", "red_bull", grid = 3, position = 1, points = 25.0),
        )
    }

    private fun viewModel(repository: FakeF1Repository) = WeekendViewModel(
        repository,
        SavedStateHandle(mapOf("season" to 2026, "round" to 17, "circuitId" to "marina_bay")),
    )

    @Test
    fun `shows the race and circuit names`() = runTest {
        // Act
        val state = viewModel(repository()).uiState.first { !it.isLoading }

        // Assert
        assertEquals("Singapore Grand Prix", state.raceName)
        assertEquals("Marina Bay Street Circuit", state.circuitName)
    }

    @Test
    fun `circuit stats only use this circuit's races from the last ten seasons`() = runTest {
        // Act
        val state = viewModel(repository()).uiState.first { !it.isLoading && it.circuitStats != null }

        // Assert
        val stats = state.circuitStats!!
        assertEquals(listOf(2024, 2025), stats.seasons)
        assertEquals(Rate(1, 2), stats.winsFromPole)
    }

    @Test
    fun `form lists this season's drivers by points with names resolved`() = runTest {
        // Act
        val state = viewModel(repository()).uiState.first { !it.isLoading && it.driverForms.isNotEmpty() }

        // Assert
        assertEquals(listOf("Yuki Tsunoda", "Max Verstappen"), state.driverForms.map { it.driverName })
        assertEquals("Red Bull", state.driverForms.first().teamName)
        assertEquals("Red Bull", state.headToHeads.single().teamName)
    }

    @Test
    fun `syncs the season and the circuit's history`() = runTest {
        // Arrange
        val repository = repository()

        // Act
        viewModel(repository).uiState.first { !it.isLoading }

        // Assert
        assertEquals(listOf(2026), repository.syncedSeasons)
        assertEquals(listOf("marina_bay"), repository.syncedCircuitHistories)
    }

    @Test
    fun `a failed sync shows an error`() = runTest {
        // Arrange
        val repository = repository().apply { syncFailure = IOException("offline") }

        // Act
        val state = viewModel(repository).uiState.first { !it.isLoading }

        // Assert
        assertNotNull(state.loadErrorMessage)
    }
}
