package xyz.gaon.typoon.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.gaon.typoon.R
import xyz.gaon.typoon.core.data.datastore.AppLanguage
import xyz.gaon.typoon.core.data.datastore.AppLocaleManager
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.data.datastore.AppSettings
import xyz.gaon.typoon.core.data.datastore.ThemeMode
import xyz.gaon.typoon.core.data.repository.ExceptionRepository
import xyz.gaon.typoon.core.data.repository.HistoryRepository
import xyz.gaon.typoon.core.export.CsvExporter
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val appPreferences: AppPreferences,
        private val historyRepository: HistoryRepository,
        private val exceptionRepository: ExceptionRepository,
        private val csvExporter: CsvExporter,
        @param:ApplicationContext private val appContext: Context,
    ) : ViewModel() {
        private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
        val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

        private val _resetEvent = Channel<Unit>(Channel.BUFFERED)
        val resetEvent = _resetEvent.receiveAsFlow()

        fun onExportCsv(uri: Uri) {
            viewModelScope.launch {
                _exportState.value = ExportState.Exporting
                try {
                    val outputStream =
                        appContext.contentResolver.openOutputStream(uri)
                            ?: throw IOException(appContext.getString(R.string.settings_export_open_stream_error))
                    outputStream.use { stream ->
                        csvExporter.exportToCsv(stream)
                    }
                    _exportState.value = ExportState.Success
                } catch (e: IOException) {
                    _exportState.value =
                        ExportState.Error(
                            e.message ?: appContext.getString(R.string.settings_export_failed),
                        )
                } catch (e: SecurityException) {
                    _exportState.value =
                        ExportState.Error(
                            e.message ?: appContext.getString(R.string.settings_export_failed),
                        )
                }
            }
        }

        fun onExportStateDismissed() {
            _exportState.value = ExportState.Idle
        }

        val settings: StateFlow<AppSettings> =
            appPreferences.settings
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

        fun onThemeModeChange(themeMode: ThemeMode) {
            viewModelScope.launch {
                appPreferences.update { it.copy(themeMode = themeMode) }
            }
        }

        fun onAppLanguageChange(appLanguage: AppLanguage) {
            viewModelScope.launch {
                appPreferences.update { it.copy(appLanguage = appLanguage) }
                AppLocaleManager.apply(appLanguage)
            }
        }

        fun onSaveHistoryToggle(enabled: Boolean) {
            viewModelScope.launch {
                appPreferences.update { it.copy(saveHistory = enabled) }
            }
        }

        fun onHapticToggle(enabled: Boolean) {
            viewModelScope.launch {
                appPreferences.update { it.copy(hapticEnabled = enabled) }
            }
        }

        fun onAutoConvertClipboardToggle(enabled: Boolean) {
            viewModelScope.launch {
                appPreferences.update { it.copy(autoConvertAfterClipboardRead = enabled) }
            }
        }

        fun onAutoReadClipboardToggle(enabled: Boolean) {
            viewModelScope.launch {
                appPreferences.update { it.copy(autoReadClipboardOnLaunch = enabled) }
            }
        }

        fun onMaxHistoryCountChange(delta: Int) {
            viewModelScope.launch {
                appPreferences.update {
                    val updated = (it.maxHistoryCount + delta).coerceIn(10, 200)
                    it.copy(maxHistoryCount = updated)
                }
            }
        }

        fun onConfidenceThresholdChange(value: Float) {
            viewModelScope.launch {
                appPreferences.update { it.copy(confidenceWarningThreshold = value.coerceIn(0.3f, 0.9f)) }
            }
        }

        fun onAppendShareCreditToggle(enabled: Boolean) {
            viewModelScope.launch {
                appPreferences.update { it.copy(appendShareCredit = enabled) }
            }
        }

        fun onDeleteAllHistory() {
            viewModelScope.launch {
                historyRepository.deleteAll()
            }
        }

        fun onResetApp() {
            viewModelScope.launch {
                historyRepository.deleteAll()
                exceptionRepository.deleteAll()
                appPreferences.reset()
                AppLocaleManager.apply(AppLanguage.SYSTEM)
                _resetEvent.send(Unit)
            }
        }
    }

sealed interface ExportState {
    data object Idle : ExportState

    data object Exporting : ExportState

    data object Success : ExportState

    data class Error(
        val message: String,
    ) : ExportState
}
