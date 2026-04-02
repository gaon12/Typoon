package xyz.gaon.typoon.feature.settings.sub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.gaon.typoon.R
import xyz.gaon.typoon.feature.settings.SettingsHeroCard
import xyz.gaon.typoon.feature.settings.SettingsPageScaffold
import xyz.gaon.typoon.feature.settings.SettingsSectionTitle
import xyz.gaon.typoon.feature.settings.SettingsStepperCard
import xyz.gaon.typoon.feature.settings.SettingsToggleRow
import xyz.gaon.typoon.feature.settings.SettingsViewModel

@Composable
fun GeneralSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()

    SettingsPageScaffold(
        title = stringResource(R.string.settings_general_title),
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
                chip = stringResource(R.string.settings_general_chip),
                title = stringResource(R.string.settings_general_headline),
                description = stringResource(R.string.settings_general_description),
                primaryIcon = Icons.AutoMirrored.Filled.ManageSearch,
                secondaryIcon = Icons.Default.Tune,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSectionTitle(stringResource(R.string.settings_general_history_section))
                SettingsStepperCard(
                    title = stringResource(R.string.settings_general_history_title),
                    description = stringResource(R.string.settings_general_history_description),
                    valueText = settings.maxHistoryCount.toString(),
                    canDecrease = settings.maxHistoryCount > 10,
                    canIncrease = settings.maxHistoryCount < 200,
                    onDecrease = { viewModel.onMaxHistoryCountChange(-10) },
                    onIncrease = { viewModel.onMaxHistoryCountChange(10) },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSectionTitle(stringResource(R.string.settings_general_quality_section))
                SettingsStepperCard(
                    title = stringResource(R.string.settings_general_quality_title),
                    description = stringResource(R.string.settings_general_quality_description),
                    valueText = "${(settings.confidenceWarningThreshold * 100).toInt()}%",
                    canDecrease = settings.confidenceWarningThreshold > 0.3f,
                    canIncrease = settings.confidenceWarningThreshold < 0.9f,
                    onDecrease = { viewModel.onConfidenceThresholdChange(settings.confidenceWarningThreshold - 0.1f) },
                    onIncrease = { viewModel.onConfidenceThresholdChange(settings.confidenceWarningThreshold + 0.1f) },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSectionTitle(stringResource(R.string.settings_general_share_section))
                SettingsToggleRow(
                    title = stringResource(R.string.settings_general_share_title),
                    description = stringResource(R.string.settings_general_share_description),
                    checked = settings.appendShareCredit,
                    onCheckedChange = viewModel::onAppendShareCreditToggle,
                )
            }
        }
    }
}
