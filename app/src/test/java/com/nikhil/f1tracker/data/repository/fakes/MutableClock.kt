package com.nikhil.f1tracker.data.repository.fakes

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class MutableClock(
    private var currentInstant: Instant = Instant.EPOCH,
    private val zone: ZoneId = ZoneOffset.UTC,
) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(currentInstant, zone)
    override fun instant(): Instant = currentInstant

    fun advanceBy(duration: Duration) {
        currentInstant = currentInstant.plus(duration)
    }
}
