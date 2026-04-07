package xyz.gaon.typoon.core.data.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppPreferencesTest {
    private lateinit var appPreferences: AppPreferences

    @Before
    fun setUp() =
        runTest {
            val context = ApplicationProvider.getApplicationContext<Context>()
            appPreferences = AppPreferences(context)
            appPreferences.reset()
        }

    @Test
    fun update_persistsValues() =
        runTest {
            appPreferences.update {
                it.copy(
                    maxHistoryCount = 120,
                    autoReadClipboardOnLaunch = true,
                    autoConvertAfterClipboardRead = true,
                    clipboardSuggestionEnabled = false,
                    hapticEnabled = false,
                    themeMode = ThemeMode.DARK,
                    appLanguage = AppLanguage.KOREAN,
                )
            }

            val settings = appPreferences.settings.first()

            assertEquals(120, settings.maxHistoryCount)
            assertEquals(true, settings.autoReadClipboardOnLaunch)
            assertEquals(true, settings.autoConvertAfterClipboardRead)
            assertEquals(false, settings.clipboardSuggestionEnabled)
            assertEquals(false, settings.hapticEnabled)
            assertEquals(ThemeMode.DARK, settings.themeMode)
            assertEquals(AppLanguage.KOREAN, settings.appLanguage)
        }

    @Test
    fun reset_restoresDefaultValues() =
        runTest {
            appPreferences.update { it.copy(maxHistoryCount = 150, clipboardSuggestionEnabled = false) }
            appPreferences.reset()

            val settings = appPreferences.settings.first()

            assertEquals(50, settings.maxHistoryCount)
            assertEquals(true, settings.clipboardSuggestionEnabled)
            assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        }
}
