package com.nikhil.f1tracker.data.repository

import com.nikhil.f1tracker.domain.model.F1Identities
import kotlinx.coroutines.flow.Flow

interface IdentityRepository {
    val identities: Flow<F1Identities>
    suspend fun syncIdentities()
}
