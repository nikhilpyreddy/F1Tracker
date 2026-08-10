package com.nikhil.f1tracker.ui.driver

import androidx.lifecycle.SavedStateHandle
import com.nikhil.f1tracker.MainDispatcherRule
import com.nikhil.f1tracker.data.local.entity.DriverEntity
import com.nikhil.f1tracker.data.local.entity.DriverStandingEntity
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.local.entity.ResultEntity
import com.nikhil.f1tracker.data.repository.fakes.FakeF1Repository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DriverDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val driver = DriverEntity(
        driverId = "max_verstappen",
        permanentNumber = 3,
        code = "VER",
        givenName = "Max",
        familyName = "Verstappen",
        dateOfBirth = "1997-09-30",
        nationality = "Dutch",
    )

    @Test
    fun `uiState exposes the points trend across the last four seasons`() = runTest {
        // Arrange
        val repository = FakeF1Repository().apply {
            allDrivers.value = listOf(driver)
            allDriverStandings.value = listOf(
                DriverStandingEntity(2023, "max_verstappen", "red_bull", 1, 575.0, 19),
                DriverStandingEntity(2024, "max_verstappen", "red_bull", 1, 437.0, 9),
                DriverStandingEntity(2025, "max_verstappen", "red_bull", 2, 396.0, 6),
                DriverStandingEntity(2026, "max_verstappen", "red_bull", 6, 109.0, 1),
            )
        }
        val viewModel = DriverDetailViewModel(repository, SavedStateHandle(mapOf("driverId" to "max_verstappen")))

        // Act
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals("Max Verstappen", state.driverName)
        assertEquals(listOf(575f, 437f, 396f, 109f), state.pointsTrend.map { it.value })
        assertEquals(listOf("2023", "2024", "2025", "2026"), state.pointsTrend.map { it.xLabel })
    }

    @Test
    fun `selecting a season updates the race results for that season only`() = runTest {
        // Arrange
        val repository = FakeF1Repository().apply {
            allDrivers.value = listOf(driver)
            allRaces.value = listOf(
                RaceEntity(2025, 1, "Bahrain Grand Prix", "bahrain", "2025-03-02", null),
                RaceEntity(2026, 1, "Bahrain Grand Prix", "bahrain", "2026-03-01", null),
            )
            allResults.value = listOf(
                ResultEntity(2025, 1, "max_verstappen", "red_bull", 1, "1", 25.0, 1, 57, "Finished", null, null, null),
                ResultEntity(2026, 1, "max_verstappen", "red_bull", 6, "6", 8.0, 3, 57, "Finished", null, null, null),
            )
        }
        val viewModel = DriverDetailViewModel(repository, SavedStateHandle(mapOf("driverId" to "max_verstappen")))
        viewModel.uiState.first { !it.isLoading }

        // Act
        viewModel.selectSeason(2025)
        val state = viewModel.uiState.first { it.selectedSeason == 2025 }

        // Assert
        assertEquals(1, state.seasonResults.size)
        assertEquals("1", state.seasonResults.single().positionText)
        assertEquals(true, repository.syncedSeasons.contains(2025))
    }
}
