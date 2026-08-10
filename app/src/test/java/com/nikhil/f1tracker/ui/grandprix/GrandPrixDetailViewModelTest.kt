package com.nikhil.f1tracker.ui.grandprix

import androidx.lifecycle.SavedStateHandle
import com.nikhil.f1tracker.MainDispatcherRule
import com.nikhil.f1tracker.data.local.entity.CircuitEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity
import com.nikhil.f1tracker.data.repository.fakes.FakeF1Repository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class GrandPrixDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun fakeRepository() = FakeF1Repository().apply {
        allDrivers.value = listOf(
            DriverEntity("max_verstappen", 3, "VER", "Max", "Verstappen", "1997-09-30", "Dutch"),
        )
        allCircuits.value = listOf(
            CircuitEntity("bahrain", "Bahrain International Circuit", "Sakhir", "Bahrain", 26.0, 50.5),
            CircuitEntity("jeddah", "Jeddah Corniche Circuit", "Jeddah", "Saudi Arabia", 21.6, 39.1),
        )
        allRaces.value = listOf(
            RaceEntity(2025, 1, "Bahrain Grand Prix", "bahrain", "2025-03-02", null),
            RaceEntity(2026, 1, "Bahrain Grand Prix", "bahrain", "2026-03-01", null),
            RaceEntity(2026, 2, "Saudi Arabian Grand Prix", "jeddah", "2026-03-08", null),
        )
        allResults.value = listOf(
            ResultEntity(2025, 1, "max_verstappen", "red_bull", 1, "1", 25.0, 1, 57, "Finished", null, null, null),
            ResultEntity(2026, 1, "max_verstappen", "red_bull", 6, "6", 8.0, 3, 57, "Finished", null, null, null),
            ResultEntity(2026, 2, "max_verstappen", "red_bull", 2, "2", 18.0, 2, 50, "Finished", null, null, null),
        )
    }

    @Test
    fun `uiState pre-selects the driver passed in from navigation`() = runTest {
        // Arrange
        val viewModel = GrandPrixDetailViewModel(
            fakeRepository(),
            SavedStateHandle(mapOf("circuitId" to "bahrain", "driverId" to "max_verstappen")),
        )

        // Act
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals("Bahrain International Circuit", state.circuitName)
        assertEquals("Max Verstappen", state.selectedDriverName)
        assertEquals(listOf(2026, 2025), state.history.map { it.season })
    }

    @Test
    fun `uiState has no driver selected when navigated to without one`() = runTest {
        // Arrange
        val viewModel = GrandPrixDetailViewModel(
            fakeRepository(),
            SavedStateHandle(mapOf("circuitId" to "bahrain")),
        )

        // Act
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertNull(state.selectedDriverId)
        assertEquals(emptyList<GrandPrixResultRow>(), state.history)
    }

    @Test
    fun `selecting a different Grand Prix switches the circuit and its history`() = runTest {
        // Arrange
        val viewModel = GrandPrixDetailViewModel(
            fakeRepository(),
            SavedStateHandle(mapOf("circuitId" to "bahrain", "driverId" to "max_verstappen")),
        )
        viewModel.uiState.first { !it.isLoading }

        // Act
        viewModel.selectGrandPrix("jeddah")
        val state = viewModel.uiState.first { it.circuitId == "jeddah" }

        // Assert
        assertEquals("Jeddah Corniche Circuit", state.circuitName)
        assertEquals(listOf(2026), state.history.map { it.season })
    }

    @Test
    fun `selecting a driver updates the history for the current circuit`() = runTest {
        // Arrange
        val repository = fakeRepository().apply {
            allDrivers.value = allDrivers.value + DriverEntity(
                "norris", 4, "NOR", "Lando", "Norris", "1999-11-13", "British",
            )
        }
        val viewModel = GrandPrixDetailViewModel(repository, SavedStateHandle(mapOf("circuitId" to "bahrain")))
        viewModel.uiState.first { !it.isLoading }

        // Act
        viewModel.selectDriver("max_verstappen")
        val state = viewModel.uiState.first { it.selectedDriverId == "max_verstappen" }

        // Assert
        assertEquals("Max Verstappen", state.selectedDriverName)
        assertEquals(listOf(2026, 2025), state.history.map { it.season })
    }
}
