package xyz.gaon.typoon.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.io.File

private fun parseThemeMode(rawValue: String?): ThemeMode =
    ThemeMode.entries.firstOrNull {
        it.name == rawValue
    } ?: ThemeMode.SYSTEM

private fun parseAppLanguage(rawValue: String?): AppLanguage =
    AppLanguage.entries.firstOrNull {
        it.name == rawValue
    } ?: AppLanguage.SYSTEM

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AppLanguage {
    SYSTEM,
    KOREAN,
    ENGLISH,
}

data class AppSettings(
    val saveHistory: Boolean = true,
    val maxHistoryCount: Int = 50,
    val autoReadClipboardOnLaunch: Boolean = false,
    val autoConvertAfterClipboardRead: Boolean = false,
    val clipboardSuggestionEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val showHelpBanner: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val confidenceWarningThreshold: Float = 0.6f,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val appendShareCredit: Boolean = false,
    val reviewRequested: Boolean = false,
    val adBlockNoticeDismissed: Boolean = false,
)

class AppPreferences(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generalDataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { preferenceFile("settings.preferences_pb") },
        )
    private val localOnlyDataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { File(appContext.noBackupFilesDir, "local_settings.preferences_pb") },
        )

    private object GeneralPreferencesKeys {
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val MAX_HISTORY_COUNT = intPreferencesKey("max_history_count")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val SHOW_HELP_BANNER = booleanPreferencesKey("show_help_banner")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CONFIDENCE_WARNING_THRESHOLD = floatPreferencesKey("confidence_warning_threshold")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val APPEND_SHARE_CREDIT = booleanPreferencesKey("append_share_credit")
        val REVIEW_REQUESTED = booleanPreferencesKey("review_requested")
        val AD_BLOCK_NOTICE_DISMISSED = booleanPreferencesKey("ad_block_notice_dismissed")
    }

    private object LocalOnlyPreferencesKeys {
        val AUTO_READ_CLIPBOARD = booleanPreferencesKey("auto_read_clipboard")
        val AUTO_CONVERT_CLIPBOARD = booleanPreferencesKey("auto_convert_clipboard")
        val CLIPBOARD_SUGGESTION_ENABLED = booleanPreferencesKey("clipboard_suggestion_enabled")
    }

    val settings: Flow<AppSettings> =
        combine(generalDataStore.data, localOnlyDataStore.data) { generalPreferences, localPreferences ->
            toAppSettings(
                generalPreferences = generalPreferences,
                localPreferences = localPreferences,
            )
        }

    suspend fun reset() {
        generalDataStore.edit { it.clear() }
        localOnlyDataStore.edit { it.clear() }
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val newSettings = transform(settings.first())
        generalDataStore.edit { preferences ->
            preferences[GeneralPreferencesKeys.SAVE_HISTORY] = newSettings.saveHistory
            preferences[GeneralPreferencesKeys.MAX_HISTORY_COUNT] = newSettings.maxHistoryCount
            preferences[GeneralPreferencesKeys.HAPTIC_ENABLED] = newSettings.hapticEnabled
            preferences[GeneralPreferencesKeys.SHOW_HELP_BANNER] = newSettings.showHelpBanner
            preferences[GeneralPreferencesKeys.ONBOARDING_COMPLETED] = newSettings.onboardingCompleted
            preferences[GeneralPreferencesKeys.CONFIDENCE_WARNING_THRESHOLD] = newSettings.confidenceWarningThreshold
            preferences[GeneralPreferencesKeys.THEME_MODE] = newSettings.themeMode.name
            preferences[GeneralPreferencesKeys.APP_LANGUAGE] = newSettings.appLanguage.name
            preferences[GeneralPreferencesKeys.APPEND_SHARE_CREDIT] = newSettings.appendShareCredit
            preferences[GeneralPreferencesKeys.REVIEW_REQUESTED] = newSettings.reviewRequested
            preferences[GeneralPreferencesKeys.AD_BLOCK_NOTICE_DISMISSED] = newSettings.adBlockNoticeDismissed
        }
        localOnlyDataStore.edit { preferences ->
            preferences[LocalOnlyPreferencesKeys.AUTO_READ_CLIPBOARD] = newSettings.autoReadClipboardOnLaunch
            preferences[LocalOnlyPreferencesKeys.AUTO_CONVERT_CLIPBOARD] = newSettings.autoConvertAfterClipboardRead
            preferences[LocalOnlyPreferencesKeys.CLIPBOARD_SUGGESTION_ENABLED] = newSettings.clipboardSuggestionEnabled
        }
    }

    private fun preferenceFile(fileName: String): File = File(appContext.filesDir, "datastore/$fileName")

    private fun toAppSettings(
        generalPreferences: Preferences,
        localPreferences: Preferences,
    ): AppSettings =
        AppSettings(
            saveHistory = generalPreferences[GeneralPreferencesKeys.SAVE_HISTORY] ?: true,
            maxHistoryCount = generalPreferences[GeneralPreferencesKeys.MAX_HISTORY_COUNT] ?: 50,
            autoReadClipboardOnLaunch = localPreferences[LocalOnlyPreferencesKeys.AUTO_READ_CLIPBOARD] ?: false,
            autoConvertAfterClipboardRead = localPreferences[LocalOnlyPreferencesKeys.AUTO_CONVERT_CLIPBOARD] ?: false,
            clipboardSuggestionEnabled =
                localPreferences[LocalOnlyPreferencesKeys.CLIPBOARD_SUGGESTION_ENABLED] ?: true,
            hapticEnabled = generalPreferences[GeneralPreferencesKeys.HAPTIC_ENABLED] ?: true,
            showHelpBanner = generalPreferences[GeneralPreferencesKeys.SHOW_HELP_BANNER] ?: true,
            onboardingCompleted = generalPreferences[GeneralPreferencesKeys.ONBOARDING_COMPLETED] ?: false,
            confidenceWarningThreshold =
                generalPreferences[GeneralPreferencesKeys.CONFIDENCE_WARNING_THRESHOLD] ?: 0.6f,
            themeMode = parseThemeMode(generalPreferences[GeneralPreferencesKeys.THEME_MODE]),
            appLanguage = parseAppLanguage(generalPreferences[GeneralPreferencesKeys.APP_LANGUAGE]),
            appendShareCredit = generalPreferences[GeneralPreferencesKeys.APPEND_SHARE_CREDIT] ?: false,
            reviewRequested = generalPreferences[GeneralPreferencesKeys.REVIEW_REQUESTED] ?: false,
            adBlockNoticeDismissed = generalPreferences[GeneralPreferencesKeys.AD_BLOCK_NOTICE_DISMISSED] ?: false,
        )
}
