package xyz.gaon.typoon.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import javax.inject.Inject

@HiltViewModel
class SplashViewModel
    @Inject
    constructor(
        appPreferences: AppPreferences,
    ) : ViewModel() {
        val onboardingCompleted =
            appPreferences.settings
                .map { it.onboardingCompleted }
                .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }
