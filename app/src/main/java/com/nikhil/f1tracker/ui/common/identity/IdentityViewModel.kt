package com.nikhil.f1tracker.ui.common.identity

import androidx.compose.runtime.compositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.f1tracker.data.repository.IdentityRepository
import com.nikhil.f1tracker.domain.model.F1Identities
import com.nikhil.f1tracker.ui.common.syncCatching
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Colours and faces for every screen, provided once at the app root via [LocalF1Identities]. */
val LocalF1Identities = compositionLocalOf { F1Identities() }

@HiltViewModel
class IdentityViewModel @Inject constructor(
    private val identityRepository: IdentityRepository,
) : ViewModel() {

    val identities: StateFlow<F1Identities> = identityRepository.identities
        .stateIn(viewModelScope, SharingStarted.Eagerly, F1Identities())

    init {
        viewModelScope.launch {
            // Failure is non-fatal: cached identities and the built-in team colours still apply.
            syncCatching { identityRepository.syncIdentities() }
        }
    }
}
