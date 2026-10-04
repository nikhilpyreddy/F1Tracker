package com.nikhil.f1tracker.domain.stats

import com.nikhil.f1tracker.data.local.entity.ResultEntity

/** A count out of a sample, kept as both numbers so the UI can always show the sample size. */
data class Rate(val hits: Int, val total: Int) {
    val fraction: Double? get() = if (total == 0) null else hits.toDouble() / total
}

internal fun <T> List<T>.rateOf(predicate: (T) -> Boolean) = Rate(count(predicate), size)

// Jolpica positionText codes: R = retired, N = not classified, W = withdrawn (did not start),
// F = failed to qualify. Retired drivers still get a numeric `position` in classified order.
private val RETIREMENT_CODES = setOf("R", "N")
private val NON_STARTER_CODES = setOf("W", "F")
private const val PIT_LANE_GRID = Int.MAX_VALUE

internal val ResultEntity.isRetirement: Boolean get() = positionText in RETIREMENT_CODES
internal val ResultEntity.didStart: Boolean get() = positionText !in NON_STARTER_CODES
internal val ResultEntity.isWinner: Boolean get() = positionText == "1"

/** Grid 0 means a pit-lane start, which is behind every grid slot. */
internal val ResultEntity.effectiveGrid: Int get() = if (grid > 0) grid else PIT_LANE_GRID

/** Official classified order: retirements already rank behind finishers; missing ranks go last. */
internal val ResultEntity.classifiedOrder: Int get() = position ?: Int.MAX_VALUE
