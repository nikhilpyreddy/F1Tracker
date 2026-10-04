package com.nikhil.f1tracker.domain.stats

import com.nikhil.f1tracker.data.remote.openf1.OpenF1CarDataDto
import com.nikhil.f1tracker.data.remote.openf1.OpenF1LocationDto
import java.time.OffsetDateTime

private const val FULL_THROTTLE = 98
private const val SLOW_CORNER_MAX_KPH = 130
private const val MEDIUM_CORNER_MAX_KPH = 210

// A corner is a speed low point that's this much below the speed before it; filters out noise
// and small lifts on straights at OpenF1's ~4 Hz sample rate.
private const val CORNER_MIN_DROP_KPH = 25
private const val KPH_TO_MPS = 1 / 3.6
private const val MILLIS_PER_SECOND = 1000.0

enum class CornerSpeed(val label: String) { SLOW("Slow"), MEDIUM("Medium"), FAST("Fast") }

data class Corner(val minSpeedKph: Int, val speed: CornerSpeed)

/** A point on the track map, with the car's speed there. */
data class TrackPoint(val x: Float, val y: Float, val speedKph: Int)

/** What a circuit asks of a car, from one pole lap. Speeds are approximate (~4 samples/s). */
data class TrackProfile(
    val lapLengthMeters: Int,
    val topSpeedKph: Int,
    val minSpeedKph: Int,
    val fullThrottleShare: Double,
    val longestFullThrottleSeconds: Double,
    val longestFullThrottleMeters: Int,
    val brakingZones: Int,
    val corners: List<Corner>,
    val map: List<TrackPoint>,
) {
    fun cornerCount(speed: CornerSpeed) = corners.count { it.speed == speed }
}

fun trackProfile(car: List<OpenF1CarDataDto>, location: List<OpenF1LocationDto>): TrackProfile? {
    if (car.size < 2) return null
    val times = car.map { OffsetDateTime.parse(it.date).toInstant().toEpochMilli() }
    // Seconds each sample "covers": the gap to the next sample (the last reuses the previous gap).
    val durations = times.zipWithNext { a, b -> (b - a) / MILLIS_PER_SECOND }.let { it + it.last() }
    val meters = car.indices.map { car[it].speed * KPH_TO_MPS * durations[it] }

    val fullThrottle = car.map { it.throttle >= FULL_THROTTLE }
    val longestRun = longestRun(fullThrottle, durations, meters)
    return TrackProfile(
        lapLengthMeters = meters.sum().toInt(),
        topSpeedKph = car.maxOf { it.speed },
        minSpeedKph = car.minOf { it.speed },
        fullThrottleShare = car.indices.filter { fullThrottle[it] }.sumOf { durations[it] } / durations.sum(),
        longestFullThrottleSeconds = longestRun.first,
        longestFullThrottleMeters = longestRun.second.toInt(),
        brakingZones = car.zipWithNext().count { (a, b) -> a.brake == 0 && b.brake > 0 },
        corners = corners(car.map { it.speed }),
        map = trackMap(car, times, location),
    )
}

private fun longestRun(flags: List<Boolean>, durations: List<Double>, meters: List<Double>): Pair<Double, Double> {
    var best = 0.0 to 0.0
    var current = 0.0 to 0.0
    flags.indices.forEach { i ->
        current = if (flags[i]) (current.first + durations[i]) to (current.second + meters[i]) else 0.0 to 0.0
        if (current.first > best.first) best = current
    }
    return best
}

/** Local speed minima that follow a big enough drop count as corners, classified by apex speed. */
private fun corners(speeds: List<Int>): List<Corner> {
    val corners = mutableListOf<Corner>()
    var peakSinceLastCorner = speeds.first()
    for (i in 1 until speeds.lastIndex) {
        val speed = speeds[i]
        peakSinceLastCorner = maxOf(peakSinceLastCorner, speed)
        val isLowPoint = speed <= speeds[i - 1] && speed < speeds[i + 1]
        if (isLowPoint && peakSinceLastCorner - speed >= CORNER_MIN_DROP_KPH) {
            corners += Corner(speed, classify(speed))
            peakSinceLastCorner = speed
        }
    }
    return corners
}

private fun classify(speed: Int) = when {
    speed < SLOW_CORNER_MAX_KPH -> CornerSpeed.SLOW
    speed < MEDIUM_CORNER_MAX_KPH -> CornerSpeed.MEDIUM
    else -> CornerSpeed.FAST
}

/** Pairs each position sample with the speed from the nearest-in-time telemetry sample. */
private fun trackMap(car: List<OpenF1CarDataDto>, carTimes: List<Long>, location: List<OpenF1LocationDto>): List<TrackPoint> {
    var cursor = 0
    return location.map { point ->
        val time = OffsetDateTime.parse(point.date).toInstant().toEpochMilli()
        while (cursor < carTimes.lastIndex && carTimes[cursor + 1] <= time) cursor++
        val nearest = if (cursor < carTimes.lastIndex && carTimes[cursor + 1] - time < time - carTimes[cursor]) {
            cursor + 1
        } else {
            cursor
        }
        TrackPoint(point.x.toFloat(), point.y.toFloat(), car[nearest].speed)
    }
}
