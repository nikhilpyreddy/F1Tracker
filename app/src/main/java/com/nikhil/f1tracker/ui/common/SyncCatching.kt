package com.nikhil.f1tracker.ui.common

import kotlinx.coroutines.CancellationException

/**
 * Runs a network sync and captures any failure as a [Result] instead of letting it escape.
 *
 * Catches more than network errors on purpose: a malformed or changed API response surfaces as
 * a `SerializationException` or `NumberFormatException` (from the mappers), and either one
 * escaping `viewModelScope` would crash the app. [CancellationException] is rethrown so that
 * leaving a screen still cancels its sync.
 */
suspend fun syncCatching(block: suspend () -> Unit): Result<Unit> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
