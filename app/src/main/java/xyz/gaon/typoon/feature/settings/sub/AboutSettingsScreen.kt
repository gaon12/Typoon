package xyz.gaon.typoon.feature.settings.sub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.gaon.typoon.BuildConfig
import xyz.gaon.typoon.R
import xyz.gaon.typoon.feature.settings.SettingsHeroCard
import xyz.gaon.typoon.feature.settings.SettingsNavigationRow
import xyz.gaon.typoon.feature.settings.SettingsPageScaffold
import xyz.gaon.typoon.feature.settings.SettingsPanel
import xyz.gaon.typoon.feature.settings.SettingsRowDivider
import xyz.gaon.typoon.feature.settings.SettingsSectionTitle

@Composable
fun AboutSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOpenSource: () -> Unit,
    onNavigateToContributors: () -> Unit,
    onNavigateToDonors: () -> Unit,
    onNavigateToEasterEgg: () -> Unit,
) {
    val versionTapCountState = rememberSaveable { mutableIntStateOf(0) }
    val lastVersionTapAtState = rememberSaveable { mutableLongStateOf(0L) }
    val versionTapCount = versionTapCountState.intValue

    SettingsPageScaffold(
        title = stringResource(R.string.about_title),
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
                chip = stringResource(R.string.about_chip),
                title = stringResource(R.string.about_hero_title),
                description = stringResource(R.string.about_hero_description),
                primaryIcon = Icons.Default.Info,
                secondaryIcon = Icons.Default.Verified,
            )

            AppVersionSection(
                onVersionTap = {
                    val now = System.currentTimeMillis()
                    val isContinuous = now - lastVersionTapAtState.longValue <= VERSION_TAP_TIMEOUT_MS
                    lastVersionTapAtState.longValue = now
                    val nextCount = versionTapCount + 1
                    if (!isContinuous) {
                        versionTapCountState.intValue = 1
                        return@AppVersionSection
                    }
                    if (nextCount >= 7) {
                        versionTapCountState.intValue = 0
                        lastVersionTapAtState.longValue = 0L
                        onNavigateToEasterEgg()
                    } else {
                        versionTapCountState.intValue = nextCount
                    }
                },
            )

            AboutLinksSection(
                onNavigateToOpenSource = onNavigateToOpenSource,
                onNavigateToContributors = onNavigateToContributors,
                onNavigateToDonors = onNavigateToDonors,
            )
        }
    }
}

private const val VERSION_TAP_TIMEOUT_MS = 1_500L

@Composable
private fun AppVersionSection(onVersionTap: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsSectionTitle(stringResource(R.string.about_version_section))
        Surface(
            modifier = Modifier.clickable(onClick = onVersionTap),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.about_version_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.about_version_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AboutLinksSection(
    onNavigateToOpenSource: () -> Unit,
    onNavigateToContributors: () -> Unit,
    onNavigateToDonors: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsSectionTitle(stringResource(R.string.about_more_section))
        SettingsPanel {
            SettingsNavigationRow(
                title = stringResource(R.string.about_open_source_title),
                description = stringResource(R.string.about_open_source_description),
                onClick = onNavigateToOpenSource,
            )
            SettingsRowDivider()
            SettingsNavigationRow(
                title = stringResource(R.string.about_contributors_title),
                description = stringResource(R.string.about_contributors_description),
                onClick = onNavigateToContributors,
            )
            SettingsRowDivider()
            SettingsNavigationRow(
                title = stringResource(R.string.about_donors_title),
                description = stringResource(R.string.about_donors_description),
                onClick = onNavigateToDonors,
            )
        }
    }
}
