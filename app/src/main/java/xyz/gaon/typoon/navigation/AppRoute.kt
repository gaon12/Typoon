package xyz.gaon.typoon.navigation

sealed class AppRoute(
    val route: String,
) {
    data object Splash : AppRoute("splash")

    data object Onboarding : AppRoute("onboarding")

    data object Home : AppRoute("home")

    data object History : AppRoute("history")

    data object SettingsHistory : AppRoute("settings/history")

    data object Result : AppRoute("result")

    data object Settings : AppRoute("settings")

    data object SettingsTheme : AppRoute("settings/theme")

    data object SettingsLanguage : AppRoute("settings/language")

    data object SettingsGeneral : AppRoute("settings/general")

    data object SettingsSaveHistory : AppRoute("settings/save_history")

    data object SettingsAutoReadClipboard : AppRoute("settings/auto_read_clipboard")

    data object SettingsAutoConvertClipboard : AppRoute("settings/auto_convert_clipboard")

    data object SettingsClipboardSuggestion : AppRoute("settings/clipboard_suggestion")

    data object SettingsHaptic : AppRoute("settings/haptic")

    data object SettingsPrivacy : AppRoute("settings/privacy")

    data object SettingsAbout : AppRoute("settings/about")

    data object SettingsOpenSource : AppRoute("settings/open_source")

    data object SettingsContributors : AppRoute("settings/contributors")

    data object SettingsDonors : AppRoute("settings/donors")

    data object SettingsEasterEgg : AppRoute("settings/easter_egg")

    data object Dictionary : AppRoute("dictionary")
}
