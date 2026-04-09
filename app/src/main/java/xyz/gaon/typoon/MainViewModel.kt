package xyz.gaon.typoon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.data.datastore.AppSettings
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val appPreferences: AppPreferences,
    ) : ViewModel() {
        val settings: StateFlow<AppSettings> =
            appPreferences.settings
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

        fun onAdBlockNoticeDismissed(dontShowAgain: Boolean) {
            if (!dontShowAgain) return

            viewModelScope.launch {
                appPreferences.update { it.copy(adBlockNoticeDismissed = true) }
            }
        }
    }
