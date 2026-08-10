package com.nikhil.f1tracker.domain.model

const val YEARS_OF_HISTORY = 4
const val GRAND_PRIX_HISTORY_YEARS = 6

fun lastNSeasons(currentYear: Int, count: Int): List<Int> =
    (currentYear - (count - 1)..currentYear).toList()

fun lastFourSeasons(currentYear: Int): List<Int> = lastNSeasons(currentYear, YEARS_OF_HISTORY)
