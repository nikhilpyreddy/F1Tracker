package com.nikhil.f1tracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteSelectionTest {

    @Test
    fun `toggleDriver adds a driver when under the limit`() {
        // Arrange
        val selection = FavoriteSelection()

        // Act
        val result = selection.toggleDriver("max_verstappen")

        // Assert
        assertTrue(result is FavoriteToggleResult.Updated)
        assertEquals(setOf("max_verstappen"), (result as FavoriteToggleResult.Updated).selection.driverIds)
    }

    @Test
    fun `toggleDriver removes an already-selected driver`() {
        // Arrange
        val selection = FavoriteSelection(driverIds = setOf("max_verstappen"))

        // Act
        val result = selection.toggleDriver("max_verstappen")

        // Assert
        assertEquals(
            emptySet<String>(),
            (result as FavoriteToggleResult.Updated).selection.driverIds,
        )
    }

    @Test
    fun `toggleDriver allows exactly four favorite drivers`() {
        // Arrange
        val selection = FavoriteSelection(driverIds = setOf("d1", "d2", "d3"))

        // Act
        val result = selection.toggleDriver("d4")

        // Assert
        assertEquals(setOf("d1", "d2", "d3", "d4"), (result as FavoriteToggleResult.Updated).selection.driverIds)
    }

    @Test
    fun `toggleDriver rejects a fifth favorite driver`() {
        // Arrange
        val selection = FavoriteSelection(driverIds = setOf("d1", "d2", "d3", "d4"))

        // Act
        val result = selection.toggleDriver("d5")

        // Assert
        assertEquals(FavoriteToggleResult.LimitReached, result)
    }

    @Test
    fun `toggleTeam allows exactly two favorite teams`() {
        // Arrange
        val selection = FavoriteSelection(teamIds = setOf("red_bull"))

        // Act
        val result = selection.toggleTeam("mclaren")

        // Assert
        assertEquals(setOf("red_bull", "mclaren"), (result as FavoriteToggleResult.Updated).selection.teamIds)
    }

    @Test
    fun `toggleTeam rejects a third favorite team`() {
        // Arrange
        val selection = FavoriteSelection(teamIds = setOf("red_bull", "mclaren"))

        // Act
        val result = selection.toggleTeam("ferrari")

        // Assert
        assertEquals(FavoriteToggleResult.LimitReached, result)
    }

    @Test
    fun `toggleTeam removes an already-selected team even when at the limit`() {
        // Arrange
        val selection = FavoriteSelection(teamIds = setOf("red_bull", "mclaren"))

        // Act
        val result = selection.toggleTeam("mclaren")

        // Assert
        assertEquals(setOf("red_bull"), (result as FavoriteToggleResult.Updated).selection.teamIds)
    }

    @Test
    fun `toggleDriver does not mutate the original selection`() {
        // Arrange
        val original = FavoriteSelection(driverIds = setOf("max_verstappen"))

        // Act
        original.toggleDriver("norris")

        // Assert
        assertEquals(setOf("max_verstappen"), original.driverIds)
    }
}
