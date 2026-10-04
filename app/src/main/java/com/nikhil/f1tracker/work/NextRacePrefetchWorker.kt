package com.nikhil.f1tracker.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nikhil.f1tracker.data.repository.F1Repository
import com.nikhil.f1tracker.data.repository.OpenF1Repository
import com.nikhil.f1tracker.data.repository.QualifyingRepository
import com.nikhil.f1tracker.domain.model.APP_ZONE
import com.nikhil.f1tracker.domain.model.CIRCUIT_HISTORY_SEASONS
import com.nikhil.f1tracker.domain.model.isOver
import com.nikhil.f1tracker.ui.common.syncCatching
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Clock
import java.time.Instant
import java.time.Year
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first

/**
 * Warms every cache the next race's Weekend tabs read (results, circuit history, qualifying,
 * OpenF1 pole-lap telemetry and past-race strategy), so they open instantly on race weekend
 * instead of waiting ~40 s on OpenF1's rate limit. Runs on unmetered Wi-Fi only.
 */
@HiltWorker
class NextRacePrefetchWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val f1Repository: F1Repository,
    private val qualifyingRepository: QualifyingRepository,
    private val openF1Repository: OpenF1Repository,
    private val clock: Clock,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val season = Year.now(clock.withZone(APP_ZONE)).value
        val sinceSeason = season - CIRCUIT_HISTORY_SEASONS
        val failures = listOf(
            syncCatching { f1Repository.syncSchedule(season) },
            syncCatching { f1Repository.syncSeason(season) },
            syncCatching { qualifyingRepository.syncSeason(season) },
        ).count { it.isFailure }
        val next = f1Repository.getRacesForSeason(season).first()
            .sortedBy { it.round }
            .firstOrNull { !it.isOver(Instant.now(clock)) }
            ?: return if (failures == 0) Result.success() else Result.retry()

        val circuitFailures = listOf(
            syncCatching { f1Repository.syncCircuitHistory(next.circuitId, sinceSeason) },
            syncCatching { qualifyingRepository.syncCircuit(next.circuitId, sinceSeason) },
            syncCatching { openF1Repository.poleLap(next.circuitId) },
            syncCatching { openF1Repository.raceSessions(next.circuitId).collect() },
        ).onEach { result -> result.exceptionOrNull()?.let { Log.w(TAG, "Prefetch step failed for ${next.circuitId}", it) } }
            .count { it.isFailure }
        return if (failures + circuitFailures == 0) Result.success() else Result.retry()
    }

    companion object {
        private const val TAG = "NextRacePrefetch"
        private const val UNIQUE_NAME = "next-race-prefetch"
        private const val INTERVAL_HOURS = 12L

        /** Idempotent: keeps an existing schedule rather than restarting it on every launch. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NextRacePrefetchWorker>(INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
