package com.nikhil.f1tracker.ui.common

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SeriesBlueLight = Color(0xFF2A78D6)
private val SeriesBlueDark = Color(0xFF3987E5)
private val SeriesOrangeLight = Color(0xFFEB6834)
private val SeriesOrangeDark = Color(0xFFD95926)

/**
 * Fixed categorical slots from the validated reference palette (blue, orange) —
 * always assigned in this order so identity never depends on which series a
 * filter happens to leave on screen.
 */
@Composable
fun chartSeriesColor(index: Int): Color {
    val dark = isSystemInDarkTheme()
    return when (index) {
        0 -> if (dark) SeriesBlueDark else SeriesBlueLight
        else -> if (dark) SeriesOrangeDark else SeriesOrangeLight
    }
}
