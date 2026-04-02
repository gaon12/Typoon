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
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
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
import xyz.gaon.typoon.core.data.datastore.AppLanguage
import xyz.gaon.typoon.feature.settings.SettingsHeroCard
import xyz.gaon.typoon.feature.settings.SettingsPageScaffold
import xyz.gaon.typoon.feature.settings.SettingsPanel
import xyz.gaon.typoon.feature.settings.SettingsRowDivider
import xyz.gaon.typoon.feature.settings.SettingsSectionTitle
import xyz.gaon.typoon.feature.settings.SettingsViewModel

@Composable
fun LanguageSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()

    SettingsPageScaffold(
        title = stringResource(R.string.settings_language_title),
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
                chip = stringResource(R.string.settings_language_chip),
                title = stringResource(R.string.settings_language_headline),
                description = stringResource(R.string.settings_language_description),
                primaryIcon = Icons.Default.Language,
                secondaryIcon = Icons.Default.Translate,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSectionTitle(stringResource(R.string.settings_language_section))
                SettingsPanel {
                    AppLanguage.entries.forEachIndexed { index, appLanguage ->
                        LanguageModeRow(
                            mode = appLanguage,
                            selected = settings.appLanguage == appLanguage,
                            onClick = { viewModel.onAppLanguageChange(appLanguage) },
                        )
                        if (index < AppLanguage.entries.lastIndex) {
                            SettingsRowDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageModeRow(
    mode: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val titleRes =
        when (mode) {
            AppLanguage.SYSTEM -> R.string.settings_language_system
            AppLanguage.KOREAN -> R.string.settings_language_korean
            AppLanguage.ENGLISH -> R.string.settings_language_english
        }
    val descriptionRes =
        when (mode) {
            AppLanguage.SYSTEM -> R.string.settings_language_system_description
            AppLanguage.KOREAN -> R.string.settings_language_korean_description
            AppLanguage.ENGLISH -> R.string.settings_language_english_description
        }
    val icon = if (mode == AppLanguage.SYSTEM) Icons.Default.Tune else Icons.Default.Language

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
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
            Text(
                text = stringResource(descriptionRes),
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
