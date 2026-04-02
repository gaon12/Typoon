package xyz.gaon.typoon.core.data.datastore

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLocaleManager {
    fun apply(language: AppLanguage) {
        val locales =
            when (language) {
                AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                AppLanguage.KOREAN -> LocaleListCompat.forLanguageTags("ko")
                AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
            }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
