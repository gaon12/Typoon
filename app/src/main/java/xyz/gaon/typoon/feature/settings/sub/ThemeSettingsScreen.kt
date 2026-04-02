package xyz.gaon.typoon.feature.settings.sub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.gaon.typoon.R
import xyz.gaon.typoon.core.data.datastore.ThemeMode
import xyz.gaon.typoon.feature.settings.SettingsHeroCard
import xyz.gaon.typoon.feature.settings.SettingsPageScaffold
import xyz.gaon.typoon.feature.settings.SettingsPanel
import xyz.gaon.typoon.feature.settings.SettingsRowDivider
import xyz.gaon.typoon.feature.settings.SettingsSectionTitle
import xyz.gaon.typoon.feature.settings.SettingsViewModel

@Composable
fun ThemeSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()

    SettingsPageScaffold(
        title = stringResource(R.string.settings_theme_title),
        onNavigateBack = onNavigateBack,
    ) { modifier ->
        Column(
            modifier =
                modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsHeroCard(
                chip = stringResource(R.string.theme_hero_chip),
                title = stringResource(R.string.theme_hero_title),
                description = stringResource(R.string.theme_hero_description),
                primaryIcon = Icons.Default.Palette,
                secondaryIcon = Icons.Default.Tune,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSectionTitle(stringResource(R.string.theme_section_mode))
                SettingsPanel {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        ThemeModeRow(
                            mode = mode,
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.onThemeModeChange(mode) },
                        )
                        if (index < ThemeMode.entries.lastIndex) {
                            SettingsRowDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModeRow(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val icon =
        when (mode) {
            ThemeMode.SYSTEM -> Icons.Default.Tune
            ThemeMode.LIGHT -> Icons.Default.LightMode
            ThemeMode.DARK -> Icons.Default.DarkMode
        }
    val title =
        when (mode) {
            ThemeMode.SYSTEM -> stringResource(R.string.theme_system_title)
            ThemeMode.LIGHT -> stringResource(R.string.theme_light_title)
            ThemeMode.DARK -> stringResource(R.string.theme_dark_title)
        }
    val description =
        when (mode) {
            ThemeMode.SYSTEM -> stringResource(R.string.theme_system_description)
            ThemeMode.LIGHT -> stringResource(R.string.theme_light_description)
            ThemeMode.DARK -> stringResource(R.string.theme_dark_description)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
    }
}
