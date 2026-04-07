package xyz.gaon.typoon.core.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

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
)

class AppPreferences(
    private val context: Context,
) {
    private object PreferencesKeys {
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val MAX_HISTORY_COUNT = intPreferencesKey("max_history_count")
        val AUTO_READ_CLIPBOARD = booleanPreferencesKey("auto_read_clipboard")
        val AUTO_CONVERT_CLIPBOARD = booleanPreferencesKey("auto_convert_clipboard")
        val CLIPBOARD_SUGGESTION_ENABLED = booleanPreferencesKey("clipboard_suggestion_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val SHOW_HELP_BANNER = booleanPreferencesKey("show_help_banner")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CONFIDENCE_WARNING_THRESHOLD = floatPreferencesKey("confidence_warning_threshold")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val APPEND_SHARE_CREDIT = booleanPreferencesKey("append_share_credit")
        val REVIEW_REQUESTED = booleanPreferencesKey("review_requested")
    }

    val settings: Flow<AppSettings> =
        context.dataStore.data.map { preferences ->
            AppSettings(
                saveHistory = preferences[PreferencesKeys.SAVE_HISTORY] ?: true,
                maxHistoryCount = preferences[PreferencesKeys.MAX_HISTORY_COUNT] ?: 50,
                autoReadClipboardOnLaunch = preferences[PreferencesKeys.AUTO_READ_CLIPBOARD] ?: false,
                autoConvertAfterClipboardRead = preferences[PreferencesKeys.AUTO_CONVERT_CLIPBOARD] ?: false,
                clipboardSuggestionEnabled = preferences[PreferencesKeys.CLIPBOARD_SUGGESTION_ENABLED] ?: true,
                hapticEnabled = preferences[PreferencesKeys.HAPTIC_ENABLED] ?: true,
                showHelpBanner = preferences[PreferencesKeys.SHOW_HELP_BANNER] ?: true,
                onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                confidenceWarningThreshold = preferences[PreferencesKeys.CONFIDENCE_WARNING_THRESHOLD] ?: 0.6f,
                themeMode = parseThemeMode(preferences[PreferencesKeys.THEME_MODE]),
                appLanguage = parseAppLanguage(preferences[PreferencesKeys.APP_LANGUAGE]),
                appendShareCredit = preferences[PreferencesKeys.APPEND_SHARE_CREDIT] ?: false,
                reviewRequested = preferences[PreferencesKeys.REVIEW_REQUESTED] ?: false,
            )
        }

    suspend fun reset() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { preferences ->
            val themeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            val languageStr = preferences[PreferencesKeys.APP_LANGUAGE] ?: AppLanguage.SYSTEM.name
            val currentSettings =
                AppSettings(
                    saveHistory = preferences[PreferencesKeys.SAVE_HISTORY] ?: true,
                    maxHistoryCount = preferences[PreferencesKeys.MAX_HISTORY_COUNT] ?: 50,
                    autoReadClipboardOnLaunch = preferences[PreferencesKeys.AUTO_READ_CLIPBOARD] ?: false,
                    autoConvertAfterClipboardRead = preferences[PreferencesKeys.AUTO_CONVERT_CLIPBOARD] ?: false,
                    clipboardSuggestionEnabled = preferences[PreferencesKeys.CLIPBOARD_SUGGESTION_ENABLED] ?: true,
                    hapticEnabled = preferences[PreferencesKeys.HAPTIC_ENABLED] ?: true,
                    showHelpBanner = preferences[PreferencesKeys.SHOW_HELP_BANNER] ?: true,
                    onboardingCompleted = preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false,
                    confidenceWarningThreshold = preferences[PreferencesKeys.CONFIDENCE_WARNING_THRESHOLD] ?: 0.6f,
                    themeMode = parseThemeMode(themeStr),
                    appLanguage = parseAppLanguage(languageStr),
                    appendShareCredit = preferences[PreferencesKeys.APPEND_SHARE_CREDIT] ?: false,
                    reviewRequested = preferences[PreferencesKeys.REVIEW_REQUESTED] ?: false,
                )
            val newSettings = transform(currentSettings)
            preferences[PreferencesKeys.SAVE_HISTORY] = newSettings.saveHistory
            preferences[PreferencesKeys.MAX_HISTORY_COUNT] = newSettings.maxHistoryCount
            preferences[PreferencesKeys.AUTO_READ_CLIPBOARD] = newSettings.autoReadClipboardOnLaunch
            preferences[PreferencesKeys.AUTO_CONVERT_CLIPBOARD] = newSettings.autoConvertAfterClipboardRead
            preferences[PreferencesKeys.CLIPBOARD_SUGGESTION_ENABLED] = newSettings.clipboardSuggestionEnabled
            preferences[PreferencesKeys.HAPTIC_ENABLED] = newSettings.hapticEnabled
            preferences[PreferencesKeys.SHOW_HELP_BANNER] = newSettings.showHelpBanner
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = newSettings.onboardingCompleted
            preferences[PreferencesKeys.CONFIDENCE_WARNING_THRESHOLD] = newSettings.confidenceWarningThreshold
            preferences[PreferencesKeys.THEME_MODE] = newSettings.themeMode.name
            preferences[PreferencesKeys.APP_LANGUAGE] = newSettings.appLanguage.name
            preferences[PreferencesKeys.APPEND_SHARE_CREDIT] = newSettings.appendShareCredit
            preferences[PreferencesKeys.REVIEW_REQUESTED] = newSettings.reviewRequested
        }
    }
}
