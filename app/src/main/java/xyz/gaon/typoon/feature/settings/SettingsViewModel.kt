package xyz.gaon.typoon.feature.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val appPreferences: AppPreferences,
        private val historyRepository: HistoryRepository,
        private val exceptionRepository: ExceptionRepository,
        private val csvExporter: CsvExporter,
        private val releaseNotesService: ReleaseNotesService,
        @param:ApplicationContext private val appContext: Context,
    ) : ViewModel() {
        private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
        val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

        private val _resetEvent = Channel<Unit>(Channel.BUFFERED)
        val resetEvent = _resetEvent.receiveAsFlow()

        private val _messageEvent = Channel<String>(Channel.BUFFERED)
        val messageEvent = _messageEvent.receiveAsFlow()

        private val _releaseNotesDialogState = MutableStateFlow<ReleaseNotesDialogState?>(null)
        val releaseNotesDialogState: StateFlow<ReleaseNotesDialogState?> = _releaseNotesDialogState.asStateFlow()

        private var hasCheckedUpdate = false

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

        fun ensureUpdateChecked() {
            if (hasCheckedUpdate) return
            hasCheckedUpdate = true

            viewModelScope.launch {
                val appUpdateInfo =
                    runCatching {
                        AppUpdateManagerFactory
                            .create(appContext)
                            .appUpdateInfo
                            .await()
                    }.getOrNull() ?: return@launch

                val updateAvailable =
                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                        appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS

                if (!updateAvailable) return@launch

                showReleaseNotesDialog(
                    preferredVersionCode = appUpdateInfo.availableVersionCode(),
                    updateRecommended = true,
                )
            }
        }

        fun onViewReleaseNotes() {
            viewModelScope.launch {
                showReleaseNotesDialog(updateRecommended = false)
            }
        }

        fun onDismissReleaseNotesDialog() {
            _releaseNotesDialogState.value = null
        }

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

        private suspend fun showReleaseNotesDialog(
            preferredVersionCode: Int? = null,
            updateRecommended: Boolean,
        ) {
            val release =
                runCatching {
                    releaseNotesService.fetchLatestRelease(preferredVersionCode)
                }.getOrNull()

            if (release == null) {
                _messageEvent.send(appContext.getString(R.string.settings_release_notes_load_failed))
                return
            }

            val appLanguage = appPreferences.settings.first().appLanguage
            val languageCode =
                when (appLanguage) {
                    AppLanguage.KOREAN -> "ko"
                    AppLanguage.ENGLISH -> "en"
                    AppLanguage.SYSTEM -> {
                        if (Locale.getDefault().language.startsWith("ko")) "ko" else "en"
                    }
                }
            val localizedNotes = ReleaseNotesLanguageParser.parse(release.body, languageCode)
            val fallbackNotes =
                if (localizedNotes.isBlank()) {
                    appContext.getString(R.string.settings_release_notes_empty)
                } else {
                    localizedNotes
                }

            _releaseNotesDialogState.value =
                ReleaseNotesDialogState(
                    versionName = release.tagName.ifBlank { release.name },
                    notes = fallbackNotes,
                    htmlUrl = release.htmlUrl,
                    updateRecommended = updateRecommended,
                )
        }

        private suspend fun <T> Task<T>.await(): T =
            suspendCancellableCoroutine { continuation ->
                addOnSuccessListener { continuation.resume(it) }
                addOnFailureListener { continuation.resumeWithException(it) }
            }
    }

data class ReleaseNotesDialogState(
    val versionName: String,
    val notes: String,
    val htmlUrl: String,
    val updateRecommended: Boolean,
)

sealed interface ExportState {
    data object Idle : ExportState

    data object Exporting : ExportState

    data object Success : ExportState

    data class Error(
        val message: String,
    ) : ExportState
}
