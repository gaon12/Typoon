package xyz.gaon.typoon.feature.settings.sub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.gaon.typoon.R
import xyz.gaon.typoon.feature.settings.SettingsHeroCard
import xyz.gaon.typoon.feature.settings.SettingsInfoBlock
import xyz.gaon.typoon.feature.settings.SettingsPageScaffold
import xyz.gaon.typoon.feature.settings.SettingsSectionTitle

@Composable
fun PrivacySettingsScreen(onNavigateBack: () -> Unit) {
    SettingsPageScaffold(
        title = stringResource(R.string.settings_privacy_title),
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
                chip = stringResource(R.string.privacy_chip),
                title = stringResource(R.string.privacy_headline),
                description = stringResource(R.string.privacy_description),
                primaryIcon = Icons.Default.Lock,
                secondaryIcon = Icons.Default.Shield,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSectionTitle(stringResource(R.string.privacy_section_process))
                SettingsInfoBlock(
                    title = stringResource(R.string.privacy_process_title),
                    body = stringResource(R.string.privacy_process_body),
                )
                SettingsInfoBlock(
                    title = stringResource(R.string.privacy_offline_title),
                    body = stringResource(R.string.privacy_offline_body),
                )
            }

            Column(
                modifier = Modifier.padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingsSectionTitle(stringResource(R.string.privacy_section_storage))
                SettingsInfoBlock(
                    title = stringResource(R.string.privacy_storage_title),
                    body = stringResource(R.string.privacy_storage_body),
                )
            }
        }
    }
}
