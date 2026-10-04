package com.nikhil.f1tracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val COMPLETED_RACES_SHOWN = 1
private const val UPCOMING_RACES_SHOWN_AFTER_NEXT = 3
private val CALENDAR_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

/**
 * The slice of the calendar shown before "Show all": the most recent completed race, the next
 * race, and a few after it. Once the season is over, the final few races instead.
 */
fun calendarWindow(calendar: List<CalendarRace>): List<CalendarRace> {
    val windowSize = COMPLETED_RACES_SHOWN + 1 + UPCOMING_RACES_SHOWN_AFTER_NEXT
    val nextIndex = calendar.indexOfFirst { it.status == RaceStatus.NEXT }
    if (nextIndex == -1) return calendar.takeLast(windowSize)
    val start = (nextIndex - COMPLETED_RACES_SHOWN).coerceAtLeast(0)
    val end = (nextIndex + UPCOMING_RACES_SHOWN_AFTER_NEXT + 1).coerceAtMost(calendar.size)
    return calendar.subList(start, end)
}

fun LazyListScope.calendarSection(
    calendar: List<CalendarRace>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onRaceClick: (CalendarRace) -> Unit,
) {
    if (calendar.isEmpty()) return
    item(key = "calendar-header") { SectionHeader("Calendar") }
    val shown = if (isExpanded) calendar else calendarWindow(calendar)
    items(shown, key = { "calendar-${it.round}" }) { race ->
        CalendarRow(race, onClick = { onRaceClick(race) })
    }
    if (shown.size < calendar.size || isExpanded) {
        item(key = "calendar-toggle") {
            TextButton(onClick = onToggleExpanded) {
                Text(if (isExpanded) "Show less" else "Show all ${calendar.size} races")
            }
        }
    }
}

@Composable
private fun CalendarRow(race: CalendarRace, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isNext = race.status == RaceStatus.NEXT
    val contentColor = if (race.status == RaceStatus.COMPLETED) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    ListItem(
        leadingContent = { Text("R${race.round}", style = MaterialTheme.typography.labelLarge) },
        headlineContent = {
            Text(race.raceName, fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal)
        },
        supportingContent = { Text(formatRaceDate(race.date)) },
        trailingContent = {
            when (race.status) {
                RaceStatus.COMPLETED -> Text("Done")
                RaceStatus.NEXT -> Text("Next", color = MaterialTheme.colorScheme.primary)
                RaceStatus.UPCOMING -> Unit
            }
        },
        colors = ListItemDefaults.colors(headlineColor = contentColor),
        modifier = modifier.clickable(onClick = onClick),
    )
}

private fun formatRaceDate(date: String): String =
    runCatching { LocalDate.parse(date).format(CALENDAR_DATE_FORMAT) }.getOrDefault(date)
