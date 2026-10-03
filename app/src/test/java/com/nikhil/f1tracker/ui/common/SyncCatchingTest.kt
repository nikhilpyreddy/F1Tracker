package com.nikhil.f1tracker.ui.common

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncCatchingTest {

    @Test
    fun `returns success when the block completes`() = runTest {
        // Act
        val result = syncCatching { }

        // Assert
        assertTrue(result.isSuccess)
    }

    @Test
    fun `returns failure for a network error`() = runTest {
        // Arrange
        val error = IOException("offline")

        // Act
        val result = syncCatching { throw error }

        // Assert
        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun `returns failure for a malformed response`() = runTest {
        // Arrange
        val error = SerializationException("unexpected JSON")

        // Act
        val result = syncCatching { throw error }

        // Assert
        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun `returns failure for an unparseable number in a response`() = runTest {
        // Arrange
        val error = NumberFormatException("For input string: \"abc\"")

        // Act
        val result = syncCatching { throw error }

        // Assert
        assertSame(error, result.exceptionOrNull())
    }

    @Test(expected = CancellationException::class)
    fun `rethrows cancellation so structured concurrency still works`() = runTest {
        syncCatching { throw CancellationException("cancelled") }
    }
}
