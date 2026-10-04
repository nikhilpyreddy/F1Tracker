package com.nikhil.f1tracker.domain.stats

import com.nikhil.f1tracker.data.local.entity.ResultEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CircuitStatsTest {

    private fun result(season: Int, driverId: String, grid: Int, positionText: String) = ResultEntity(
        season = season, round = 1, driverId = driverId, constructorId = "team",
        position = positionText.toIntOrNull(), positionText = positionText, points = 0.0,
        grid = grid, laps = 50, status = "Finished",
        finishTimeMillis = null, fastestLapRank = null, fastestLapTime = null,
    )

    // 2023: pole-sitter wins. 2024: P2 starter wins, one retirement, one non-starter.
    // 2025: P5 starter wins, P12 starter finishes 9th.
    private val history = listOf(
        result(2023, "a", grid = 1, positionText = "1"),
        result(2023, "b", grid = 2, positionText = "2"),
        result(2023, "c", grid = 3, positionText = "3"),
        result(2024, "a", grid = 2, positionText = "1"),
        result(2024, "b", grid = 1, positionText = "2"),
        result(2024, "c", grid = 4, positionText = "3"),
        result(2024, "d", grid = 3, positionText = "R"),
        result(2024, "e", grid = 0, positionText = "W"),
        result(2025, "a", grid = 5, positionText = "1"),
        result(2025, "b", grid = 1, positionText = "2"),
        result(2025, "c", grid = 2, positionText = "3"),
        result(2025, "d", grid = 12, positionText = "9"),
    )

    @Test
    fun `counts each race once`() {
        assertEquals(3, circuitStats(history).raceCount)
    }

    @Test
    fun `win rates by starting position are counted over races`() {
        // Act
        val stats = circuitStats(history)

        // Assert
        assertEquals(Rate(1, 3), stats.winsFromPole)
        assertEquals(Rate(2, 3), stats.winsFromFrontRow)
        assertEquals(Rate(2, 3), stats.winsFromTopThree)
    }

    @Test
    fun `winner average grid averages the winners' starting positions`() {
        assertEquals((1 + 2 + 5) / 3.0, circuitStats(history).winnerAverageGrid!!, 0.0001)
    }

    @Test
    fun `podium starts counts podium finishers who started in the top three`() {
        // 2023: 3/3. 2024: P1, P2 starters yes, P4 starter no. 2025: P5 no, P1, P2 yes.
        assertEquals(Rate(7, 9), circuitStats(history).podiumsFromTopThreeGrid)
    }

    @Test
    fun `top-ten finishers from outside the top-ten grid are averaged per race`() {
        assertEquals(1 / 3.0, circuitStats(history).topTenFinishersFromOutsideTopTenPerRace!!, 0.0001)
    }

    @Test
    fun `retirement rate counts retirements among starters only`() {
        // 11 starters (the withdrawn driver didn't start), 1 retirement.
        assertEquals(Rate(1, 11), circuitStats(history).retirements)
    }

    @Test
    fun `no history gives empty rates rather than dividing by zero`() {
        // Act
        val stats = circuitStats(emptyList())

        // Assert
        assertEquals(0, stats.raceCount)
        assertNull(stats.winsFromPole.fraction)
        assertNull(stats.winnerAverageGrid)
        assertNull(stats.topTenFinishersFromOutsideTopTenPerRace)
    }
}
