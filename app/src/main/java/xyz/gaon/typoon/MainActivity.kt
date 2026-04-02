package xyz.gaon.typoon

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
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
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
        )
        setContent {
            val shortcutClipboardToken by autoClipboardRequestToken
            val settings by viewModel.settings.collectAsState()

            val isDarkTheme =
                when (settings.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }

            TypoonTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavigation(shortcutClipboardToken = shortcutClipboardToken)
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

    companion object {
        const val ACTION_CLIPBOARD_CONVERT = "xyz.gaon.typoon.action.CLIPBOARD_CONVERT"
    }
}
