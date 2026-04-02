package xyz.gaon.typoon.feature.settings.sub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.VolunteerActivism
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
fun DonorsScreen(onNavigateBack: () -> Unit) {
    SettingsPageScaffold(
        title = stringResource(R.string.donors_title),
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
                chip = stringResource(R.string.donors_chip),
                title = stringResource(R.string.donors_hero_title),
                description = stringResource(R.string.donors_hero_description),
                primaryIcon = Icons.Default.VolunteerActivism,
                secondaryIcon = Icons.Default.Favorite,
            )

            Column(
                modifier = Modifier.padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingsSectionTitle(stringResource(R.string.donors_section))
                SettingsInfoBlock(
                    title = stringResource(R.string.donors_empty_title),
                    body = stringResource(R.string.donors_empty_body),
                )
            }
        }
    }
}
