package com.nikhil.f1tracker.domain.model

const val YEARS_OF_HISTORY = 4

/** Seasons of circuit history behind the Weekend screen's base rates. */
const val CIRCUIT_HISTORY_SEASONS = 10

fun lastNSeasons(currentYear: Int, count: Int): List<Int> =
    (currentYear - (count - 1)..currentYear).toList()

fun lastFourSeasons(currentYear: Int): List<Int> = lastNSeasons(currentYear, YEARS_OF_HISTORY)
