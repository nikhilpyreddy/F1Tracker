package com.nikhil.f1tracker.ui.common

import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonSyncTest {

    @Test
    fun `syncs the current season first, then the rest in order`() = runTest {
        // Arrange
        val synced = mutableListOf<Int>()

        // Act
        syncSeasonsCurrentFirst(listOf(2023, 2024, 2025, 2026), currentSeason = 2026) { synced += it }

        // Assert
        assertEquals(listOf(2026, 2023, 2024, 2025), synced)
    }

    @Test
    fun `skips a backfill season whose response is malformed and keeps going`() = runTest {
        // Arrange
        val synced = mutableListOf<Int>()

        // Act
        syncSeasonsCurrentFirst(listOf(2024, 2025, 2026), currentSeason = 2026) { year ->
            if (year == 2024) throw SerializationException("bad JSON")
            synced += year
        }

        // Assert
        assertEquals(listOf(2026, 2025), synced)
    }

    @Test(expected = IOException::class)
    fun `propagates a current-season failure`() = runTest {
        syncSeasonsCurrentFirst(listOf(2025, 2026), currentSeason = 2026) { throw IOException("offline") }
    }
}
