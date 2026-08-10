package com.nikhil.f1tracker.ui.team

import androidx.lifecycle.SavedStateHandle
import com.nikhil.f1tracker.MainDispatcherRule
import com.nikhil.f1tracker.data.local.entity.ConstructorEntity
import com.nikhil.f1tracker.data.local.entity.ConstructorStandingEntity
import com.nikhil.f1tracker.data.repository.fakes.FakeF1Repository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TeamDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `uiState exposes team name and points trend across the last four seasons`() = runTest {
        // Arrange
        val repository = FakeF1Repository().apply {
            allConstructors.value = listOf(ConstructorEntity("red_bull", "Red Bull", "Austrian"))
            allConstructorStandings.value = listOf(
                ConstructorStandingEntity(2023, "red_bull", 1, 860.0, 21),
                ConstructorStandingEntity(2024, "red_bull", 2, 589.0, 9),
                ConstructorStandingEntity(2025, "red_bull", 4, 402.0, 3),
                ConstructorStandingEntity(2026, "red_bull", 3, 210.0, 2),
            )
        }
        val viewModel = TeamDetailViewModel(repository, SavedStateHandle(mapOf("constructorId" to "red_bull")))

        // Act
        val state = viewModel.uiState.first { !it.isLoading }

        // Assert
        assertEquals("Red Bull", state.teamName)
        assertEquals("Austrian", state.nationality)
        assertEquals(listOf(860f, 589f, 402f, 210f), state.pointsTrend.map { it.value })
        assertEquals(4, state.seasonStandings.size)
        assertEquals(true, repository.syncedConstructorStandingSeasons.containsAll(listOf(2023, 2024, 2025, 2026)))
    }
}
