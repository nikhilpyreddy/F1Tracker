package com.nikhil.f1tracker.domain.stats

import com.nikhil.f1tracker.data.local.entity.ResultEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class DriverFormTest {

    private fun result(
        round: Int,
        driverId: String,
        constructorId: String,
        grid: Int,
        positionText: String,
        points: Double = 0.0,
        // Jolpica gives retired drivers a numeric position too (their official classified order).
        position: Int? = positionText.toIntOrNull(),
    ) = ResultEntity(
        season = 2026, round = round, driverId = driverId, constructorId = constructorId,
        position = position, positionText = positionText, points = points,
        grid = grid, laps = 50, status = "Finished",
        finishTimeMillis = null, fastestLapRank = null, fastestLapTime = null,
    )

    @Test
    fun `recent form keeps only the last races, newest first`() {
        // Arrange
        val results = (1..7).map { round -> result(round, "ver", "red_bull", grid = round, positionText = "$round") }

        // Act
        val form = driverForms(results, recentRaces = 5).single()

        // Assert
        assertEquals(listOf(7, 6, 5, 4, 3), form.recent.map { it.round })
    }

    @Test
    fun `retirements are counted over all races started this season`() {
        // Arrange
        val results = listOf(
            result(1, "ver", "red_bull", grid = 1, positionText = "1"),
            result(2, "ver", "red_bull", grid = 1, positionText = "R"),
            result(3, "ver", "red_bull", grid = 2, positionText = "2"),
        )

        // Act
        val form = driverForms(results, recentRaces = 5).single()

        // Assert
        assertEquals(Rate(1, 3), form.retirements)
    }

    @Test
    fun `drivers are ordered by season points and carry their latest team`() {
        // Arrange
        val results = listOf(
            result(1, "ham", "mercedes", grid = 2, positionText = "2", points = 18.0),
            result(1, "ver", "red_bull", grid = 1, positionText = "1", points = 25.0),
            result(2, "ham", "ferrari", grid = 1, positionText = "1", points = 25.0),
            result(2, "ver", "red_bull", grid = 5, positionText = "R"),
        )

        // Act
        val forms = driverForms(results, recentRaces = 5)

        // Assert
        assertEquals(listOf("ham", "ver"), forms.map { it.driverId })
        assertEquals("ferrari", forms.first().constructorId)
    }

    @Test
    fun `teammate head-to-head compares grid and finish race by race`() {
        // Arrange: ham starts ahead every time (a pit-lane start counts as behind). Finishes follow
        // the official classified order, so in the double retirement whoever lasted longer wins.
        val results = listOf(
            result(1, "ham", "ferrari", grid = 1, positionText = "2"),
            result(1, "lec", "ferrari", grid = 2, positionText = "1"),
            result(2, "ham", "ferrari", grid = 3, positionText = "R", position = 15),
            result(2, "lec", "ferrari", grid = 4, positionText = "R", position = 16),
            result(3, "ham", "ferrari", grid = 6, positionText = "5"),
            result(3, "lec", "ferrari", grid = 0, positionText = "8"),
        )

        // Act
        val h2h = teammateHeadToHeads(results).single()

        // Assert
        assertEquals("ferrari", h2h.constructorId)
        assertEquals(listOf("ham", "lec"), listOf(h2h.firstDriverId, h2h.secondDriverId))
        assertEquals(3 to 0, h2h.gridWins)
        assertEquals(2 to 1, h2h.finishWins)
    }

    @Test
    fun `a classified finish beats a retirement in the head-to-head`() {
        // Arrange
        val results = listOf(
            result(1, "alo", "aston_martin", grid = 8, positionText = "10"),
            result(1, "str", "aston_martin", grid = 9, positionText = "R", position = 18),
        )

        // Act
        val h2h = teammateHeadToHeads(results).single()

        // Assert
        assertEquals(1 to 0, h2h.finishWins)
    }
}
