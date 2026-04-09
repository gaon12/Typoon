package xyz.gaon.typoon

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import xyz.gaon.typoon.core.data.datastore.ThemeMode
import xyz.gaon.typoon.navigation.AppNavigation
import xyz.gaon.typoon.ui.theme.TypoonTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val autoClipboardRequestToken = mutableIntStateOf(0)
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { it.remove() }
        }
        handleIntentAction()
        configureEdgeToEdgeWindow()
        setContent {
            val shortcutClipboardToken by autoClipboardRequestToken
            val settings by viewModel.settings.collectAsState()
            val view = LocalView.current

            val isDarkTheme =
                when (settings.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }

            SideEffect {
                WindowCompat
                    .getInsetsController(window, view)
                    .apply {
                        isAppearanceLightStatusBars = !isDarkTheme
                        isAppearanceLightNavigationBars = !isDarkTheme
                    }
            }

            TypoonTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavigation(
                        shortcutClipboardToken = shortcutClipboardToken,
                        settings = settings,
                        onAdBlockNoticeDismissed = viewModel::onAdBlockNoticeDismissed,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentAction()
    }

    private fun handleIntentAction() {
        if (intent?.action == ACTION_CLIPBOARD_CONVERT) {
            autoClipboardRequestToken.intValue += 1
            intent =
                Intent(intent).apply {
                    action = Intent.ACTION_MAIN
                }
        }
    }

    private fun configureEdgeToEdgeWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes =
                window.attributes.apply {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                }
        }
    }

    companion object {
        const val ACTION_CLIPBOARD_CONVERT = "xyz.gaon.typoon.action.CLIPBOARD_CONVERT"
    }
}
