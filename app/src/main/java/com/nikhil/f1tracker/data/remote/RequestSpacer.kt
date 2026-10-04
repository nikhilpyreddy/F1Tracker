package com.nikhil.f1tracker.data.remote

import java.time.Duration
import okhttp3.Interceptor

/**
 * Keeps outgoing requests at least [minInterval] apart, so bursts (a multi-season sync, a
 * circuit history backfill) stay under an API's per-second rate limit instead of getting 429s.
 *
 * Blocking is fine here: OkHttp runs interceptors on its own dispatcher threads, never on main.
 */
class RequestSpacer(
    private val minInterval: Duration,
    private val nanoTime: () -> Long = System::nanoTime,
    private val sleepMillis: (Long) -> Unit = Thread::sleep,
) {
    private var nextSlotNanos = Long.MIN_VALUE

    fun awaitTurn() {
        val waitNanos = reserveSlot()
        sleepMillis(Duration.ofNanos(waitNanos).toMillis())
    }

    // Reserving under the lock and sleeping outside it lets concurrent callers queue up in
    // order without holding the lock while they wait.
    @Synchronized
    private fun reserveSlot(): Long {
        val now = nanoTime()
        val slot = if (nextSlotNanos == Long.MIN_VALUE) now else maxOf(now, nextSlotNanos)
        nextSlotNanos = slot + minInterval.toNanos()
        return slot - now
    }

    fun asInterceptor(): Interceptor = Interceptor { chain ->
        awaitTurn()
        chain.proceed(chain.request())
    }
}
