package com.nikhil.f1tracker.ui.compare

import com.nikhil.f1tracker.MainDispatcherRule
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorStandingEntity
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.DriverStandingEntity
import com.nikhil.f1tracker.data.repository.fakes.FakeF1Repository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class CompareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun fakeRepository() = FakeF1Repository().apply {
        allDrivers.value = listOf(
            DriverEntity("max_verstappen", 3, "VER", "Max", "Verstappen", "1997-09-30", "Dutch"),
            DriverEntity("norris", 4, "NOR", "Lando", "Norris", "1999-11-13", "British"),
        )
        allConstructors.value = listOf(
            ConstructorEntity("red_bull", "Red Bull", "Austrian"),
            ConstructorEntity("mclaren", "McLaren", "British"),
        )
        allDriverStandings.value = listOf(
            DriverStandingEntity(2026, "max_verstappen", "red_bull", 6, 109.0, 1),
            DriverStandingEntity(2026, "norris", "mclaren", 1, 219.0, 5),
        )
        allConstructorStandings.value = listOf(
            ConstructorStandingEntity(2026, "red_bull", 3, 210.0, 2),
            ConstructorStandingEntity(2026, "mclaren", 1, 350.0, 8),
        )
    }

    @Test
    fun `selecting two drivers populates both trend series`() = runTest {
        // Arrange
        val viewModel = CompareViewModel(fakeRepository())
        viewModel.uiState.first { !it.isLoading }

        // Act
        viewModel.select("max_verstappen")
        viewModel.select("norris")
        val state = viewModel.uiState.first { it.selectedFirstId != null && it.selectedSecondId != null }

        // Assert
        assertEquals("Max Verstappen", state.firstName)
        assertEquals("Lando Norris", state.secondName)
        assertEquals(109f, state.firstTrend.single { it.xLabel == "2026" }.value)
        assertEquals(219f, state.secondTrend.single { it.xLabel == "2026" }.value)
    }

    @Test
    fun `selecting an already-selected item clears it`() = runTest {
        // Arrange
        val viewModel = CompareViewModel(fakeRepository())
        viewModel.uiState.first { !it.isLoading }
        viewModel.select("max_verstappen")
        viewModel.uiState.first { it.selectedFirstId == "max_verstappen" }

        // Act
        viewModel.select("max_verstappen")
        val state = viewModel.uiState.first { it.selectedFirstId == null }

        // Assert
        assertNull(state.selectedFirstId)
    }

    @Test
    fun `selecting a third item replaces the second selection while keeping the first`() = runTest {
        // Arrange
        val repository = fakeRepository().apply {
            allDrivers.value = allDrivers.value + DriverEntity(
                "leclerc", 16, "LEC", "Charles", "Leclerc", "1997-10-16", "Monegasque",
            )
        }
        val viewModel = CompareViewModel(repository)
        viewModel.uiState.first { !it.isLoading }
        viewModel.select("max_verstappen")
        viewModel.select("norris")
        viewModel.uiState.first { it.selectedSecondId == "norris" }

        // Act
        viewModel.select("leclerc")
        val state = viewModel.uiState.first { it.selectedSecondId == "leclerc" }

        // Assert
        assertEquals("max_verstappen", state.selectedFirstId)
        assertEquals("leclerc", state.selectedSecondId)
    }

    @Test
    fun `switching mode clears the current selection`() = runTest {
        // Arrange
        val viewModel = CompareViewModel(fakeRepository())
        viewModel.uiState.first { !it.isLoading }
        viewModel.select("max_verstappen")
        viewModel.uiState.first { it.selectedFirstId == "max_verstappen" }

        // Act
        viewModel.setMode(CompareMode.TEAMS)
        val state = viewModel.uiState.first { it.mode == CompareMode.TEAMS }

        // Assert
        assertNull(state.selectedFirstId)
        assertNull(state.selectedSecondId)
    }
}
