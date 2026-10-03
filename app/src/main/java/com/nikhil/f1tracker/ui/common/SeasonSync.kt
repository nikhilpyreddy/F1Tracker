package com.nikhil.f1tracker.ui.common

/**
 * Syncs [currentSeason] first, then backfills the rest of [seasons] best-effort.
 *
 * A failure syncing [currentSeason] is propagated, since that's the data the screen actually
 * shows first. A failure backfilling an older season is swallowed and skipped rather than
 * aborting the whole batch — otherwise a single rate-limited request on, say, four-year-old
 * data would starve the current season of ever being synced (it used to be fetched last).
 * Backfill failures include malformed responses, not just network errors — see [syncCatching].
 */
suspend fun syncSeasonsCurrentFirst(
    seasons: List<Int>,
    currentSeason: Int,
    sync: suspend (Int) -> Unit,
) {
    sync(currentSeason)
    seasons.filter { it != currentSeason }.forEach { year ->
        // Best-effort backfill; current season already synced above.
        syncCatching { sync(year) }
    }
}
