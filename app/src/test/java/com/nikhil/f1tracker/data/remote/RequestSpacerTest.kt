package com.nikhil.f1tracker.data.remote

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Test

class RequestSpacerTest {

    private var nowNanos = 0L
    private val sleeps = mutableListOf<Long>()

    private fun spacer(interval: Duration) = RequestSpacer(
        minInterval = interval,
        nanoTime = { nowNanos },
        sleepMillis = { millis ->
            sleeps += millis
            nowNanos += Duration.ofMillis(millis).toNanos()
        },
    )

    @Test
    fun `the first request goes out immediately`() {
        // Arrange
        val spacer = spacer(Duration.ofMillis(250))

        // Act
        spacer.awaitTurn()

        // Assert
        assertEquals(listOf(0L), sleeps)
    }

    @Test
    fun `back-to-back requests are spaced by the minimum interval`() {
        // Arrange
        val spacer = spacer(Duration.ofMillis(250))

        // Act
        repeat(3) { spacer.awaitTurn() }

        // Assert
        assertEquals(listOf(0L, 250L, 250L), sleeps)
    }

    @Test
    fun `a request after a long enough pause does not wait`() {
        // Arrange
        val spacer = spacer(Duration.ofMillis(250))
        spacer.awaitTurn()

        // Act
        nowNanos += Duration.ofSeconds(1).toNanos()
        spacer.awaitTurn()

        // Assert
        assertEquals(listOf(0L, 0L), sleeps)
    }

    @Test
    fun `a request part-way through the interval waits only for the remainder`() {
        // Arrange
        val spacer = spacer(Duration.ofMillis(250))
        spacer.awaitTurn()

        // Act
        nowNanos += Duration.ofMillis(100).toNanos()
        spacer.awaitTurn()

        // Assert
        assertEquals(listOf(0L, 150L), sleeps)
    }
}
