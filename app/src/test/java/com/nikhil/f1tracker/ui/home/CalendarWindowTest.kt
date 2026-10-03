package com.nikhil.f1tracker.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarWindowTest {

    private fun race(round: Int, status: RaceStatus) =
        CalendarRace(round, "Race $round", "2026-01-01", "circuit$round", status)

    private fun season(nextRound: Int?, size: Int = 10) = (1..size).map { round ->
        when {
            nextRound == null || round < nextRound -> race(round, RaceStatus.COMPLETED)
            round == nextRound -> race(round, RaceStatus.NEXT)
            else -> race(round, RaceStatus.UPCOMING)
        }
    }

    @Test
    fun `shows the last completed race, the next race and the three after it`() {
        // Act
        val window = calendarWindow(season(nextRound = 5))

        // Assert
        assertEquals(listOf(4, 5, 6, 7, 8), window.map { it.round })
    }

    @Test
    fun `at the start of the season the window begins at round one`() {
        // Act
        val window = calendarWindow(season(nextRound = 1))

        // Assert
        assertEquals(listOf(1, 2, 3, 4), window.map { it.round })
    }

    @Test
    fun `near the end of the season the window is cut off at the last race`() {
        // Act
        val window = calendarWindow(season(nextRound = 10))

        // Assert
        assertEquals(listOf(9, 10), window.map { it.round })
    }

    @Test
    fun `when the season is over it shows the final races`() {
        // Act
        val window = calendarWindow(season(nextRound = null))

        // Assert
        assertEquals(listOf(6, 7, 8, 9, 10), window.map { it.round })
    }
}
