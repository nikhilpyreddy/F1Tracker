package com.nikhil.f1tracker.ui.home

import com.nikhil.f1tracker.MainDispatcherRule
import com.nikhil.f1tracker.data.local.entity.RaceEntity
import com.nikhil.f1tracker.data.repository.fakes.FakeF1Repository
import com.nikhil.f1tracker.data.repository.fakes.FakeFavoritesRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-10-05T12:00:00Z"), ZoneOffset.UTC)

    private fun viewModelWith(races: List<RaceEntity>) = HomeViewModel(
        FakeF1Repository().apply { allRaces.value = races },
        FakeFavoritesRepository(),
        clock,
    )

    @Test
    fun `calendar lists every race of the season in round order with its status`() = runTest {
        // Arrange
        val viewModel = viewModelWith(
            listOf(
                RaceEntity(2026, 19, "United States Grand Prix", "americas", "2026-10-25", null),
                RaceEntity(2026, 17, "Singapore Grand Prix", "marina_bay", "2026-10-04", null),
                RaceEntity(2026, 18, "Japanese Grand Prix", "suzuka", "2026-10-11", null),
                RaceEntity(2025, 18, "Japanese Grand Prix", "suzuka", "2025-10-12", null),
            ),
        )

        // Act
        val state = viewModel.uiState.first { !it.isLoading && it.calendar.isNotEmpty() }

        // Assert
        assertEquals(listOf(17, 18, 19), state.calendar.map { it.round })
        assertEquals(
            listOf(RaceStatus.COMPLETED, RaceStatus.NEXT, RaceStatus.UPCOMING),
            state.calendar.map { it.status },
        )
        assertEquals("suzuka", state.nextRace?.circuitId)
    }

    @Test
    fun `a race on today's date is still the next race`() = runTest {
        // Arrange
        val viewModel = viewModelWith(
            listOf(RaceEntity(2026, 17, "Singapore Grand Prix", "marina_bay", "2026-10-05", null)),
        )

        // Act
        val state = viewModel.uiState.first { !it.isLoading && it.calendar.isNotEmpty() }

        // Assert
        assertEquals(RaceStatus.NEXT, state.calendar.single().status)
    }

    @Test
    fun `when the season is over every race is completed and there is no next race`() = runTest {
        // Arrange
        val viewModel = viewModelWith(
            listOf(
                RaceEntity(2026, 1, "Australian Grand Prix", "albert_park", "2026-03-08", null),
                RaceEntity(2026, 2, "Chinese Grand Prix", "shanghai", "2026-03-15", null),
            ),
        )

        // Act
        val state = viewModel.uiState.first { !it.isLoading && it.calendar.isNotEmpty() }

        // Assert
        assertEquals(listOf(RaceStatus.COMPLETED, RaceStatus.COMPLETED), state.calendar.map { it.status })
        assertNull(state.nextRace)
    }
}
