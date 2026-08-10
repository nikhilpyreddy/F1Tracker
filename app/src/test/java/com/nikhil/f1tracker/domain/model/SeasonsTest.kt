package com.nikhil.f1tracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonsTest {

    @Test
    fun `lastFourSeasons returns the current year and the three before it in order`() {
        // Act
        val seasons = lastFourSeasons(2026)

        // Assert
        assertEquals(listOf(2023, 2024, 2025, 2026), seasons)
    }
}
