package com.nikhil.f1tracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.nikhil.f1tracker.domain.model.FavoriteToggleResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Exercises FavoritesRepositoryImpl against a real Preferences DataStore backed by a temp
 * file, per the project's testing rules preferring real dependencies over mocks.
 */
class FavoritesRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createRepository(): FavoritesRepositoryImpl {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tempFolder.root, "test_favorites.preferences_pb") },
        )
        return FavoritesRepositoryImpl(dataStore)
    }

    @Test
    fun `toggleDriver persists the selection so it can be read back`() = runBlocking {
        // Arrange
        val repository = createRepository()

        // Act
        repository.toggleDriver("max_verstappen")
        val selection = repository.favoriteSelection.first()

        // Assert
        assertEquals(setOf("max_verstappen"), selection.driverIds)
    }

    @Test
    fun `toggleDriver rejects a fifth driver and leaves the first four persisted`() = runBlocking {
        // Arrange
        val repository = createRepository()
        listOf("d1", "d2", "d3", "d4").forEach { repository.toggleDriver(it) }

        // Act
        val result = repository.toggleDriver("d5")
        val selection = repository.favoriteSelection.first()

        // Assert
        assertEquals(FavoriteToggleResult.LimitReached, result)
        assertEquals(setOf("d1", "d2", "d3", "d4"), selection.driverIds)
    }

    @Test
    fun `toggleTeam rejects a third team and leaves the first two persisted`() = runBlocking {
        // Arrange
        val repository = createRepository()
        repository.toggleTeam("red_bull")
        repository.toggleTeam("mclaren")

        // Act
        val result = repository.toggleTeam("ferrari")
        val selection = repository.favoriteSelection.first()

        // Assert
        assertEquals(FavoriteToggleResult.LimitReached, result)
        assertEquals(setOf("red_bull", "mclaren"), selection.teamIds)
    }

    @Test
    fun `toggling the same driver twice removes it again`() = runBlocking {
        // Arrange
        val repository = createRepository()
        repository.toggleDriver("max_verstappen")

        // Act
        repository.toggleDriver("max_verstappen")
        val selection = repository.favoriteSelection.first()

        // Assert
        assertEquals(emptySet<String>(), selection.driverIds)
    }

    @Test
    fun `driver and team selections are independent of each other`() = runBlocking {
        // Arrange
        val repository = createRepository()

        // Act
        repository.toggleDriver("max_verstappen")
        repository.toggleTeam("red_bull")
        val selection = repository.favoriteSelection.first()

        // Assert
        assertEquals(setOf("max_verstappen"), selection.driverIds)
        assertEquals(setOf("red_bull"), selection.teamIds)
    }
}
