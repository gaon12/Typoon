package xyz.gaon.typoon.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val appPreferences: AppPreferences,
    ) : ViewModel() {
        fun onComplete() {
            viewModelScope.launch {
                appPreferences.update { it.copy(onboardingCompleted = true) }
            }
        }

        fun markOnboardingComplete() {
            onComplete()
        }
    }
