@file:Suppress("LongParameterList")

package xyz.gaon.typoon.feature.settings.sub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.gaon.typoon.R
import xyz.gaon.typoon.feature.settings.SettingsHeroCard
import xyz.gaon.typoon.feature.settings.SettingsInfoBlock
import xyz.gaon.typoon.feature.settings.SettingsPageScaffold
import xyz.gaon.typoon.feature.settings.SettingsSectionTitle
import xyz.gaon.typoon.feature.settings.SettingsToggleRow
import xyz.gaon.typoon.feature.settings.SettingsViewModel

@Composable
fun SaveHistorySettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    TogglePreferenceDetailScreen(
        title = stringResource(R.string.toggle_save_history_title),
        chip = stringResource(R.string.toggle_save_history_chip),
        headline = stringResource(R.string.toggle_save_history_headline),
        description = stringResource(R.string.toggle_save_history_description),
        icon = Icons.Default.History,
        checked = settings.saveHistory,
        onCheckedChange = viewModel::onSaveHistoryToggle,
        noteTitle = stringResource(R.string.toggle_save_history_note_title),
        noteBody = stringResource(R.string.toggle_save_history_note_body),
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun AutoReadClipboardSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    TogglePreferenceDetailScreen(
        title = stringResource(R.string.toggle_auto_read_title),
        chip = stringResource(R.string.toggle_auto_read_chip),
        headline = stringResource(R.string.toggle_auto_read_headline),
        description = stringResource(R.string.toggle_auto_read_description),
        icon = Icons.Default.ContentPaste,
        checked = settings.autoReadClipboardOnLaunch,
        onCheckedChange = viewModel::onAutoReadClipboardToggle,
        noteTitle = stringResource(R.string.toggle_auto_read_note_title),
        noteBody = stringResource(R.string.toggle_auto_read_note_body),
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun AutoConvertClipboardSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    TogglePreferenceDetailScreen(
        title = stringResource(R.string.toggle_auto_convert_title),
        chip = stringResource(R.string.toggle_auto_convert_chip),
        headline = stringResource(R.string.toggle_auto_convert_headline),
        description = stringResource(R.string.toggle_auto_convert_description),
        icon = Icons.Default.PlayArrow,
        checked = settings.autoConvertAfterClipboardRead,
        onCheckedChange = viewModel::onAutoConvertClipboardToggle,
        noteTitle = stringResource(R.string.toggle_auto_convert_note_title),
        noteBody = stringResource(R.string.toggle_auto_convert_note_body),
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun ClipboardSuggestionSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    TogglePreferenceDetailScreen(
        title = stringResource(R.string.toggle_clipboard_suggestion_title),
        chip = stringResource(R.string.toggle_clipboard_suggestion_chip),
        headline = stringResource(R.string.toggle_clipboard_suggestion_headline),
        description = stringResource(R.string.toggle_clipboard_suggestion_description),
        icon = Icons.Default.AutoFixHigh,
        checked = settings.clipboardSuggestionEnabled,
        onCheckedChange = viewModel::onClipboardSuggestionToggle,
        noteTitle = stringResource(R.string.toggle_clipboard_suggestion_note_title),
        noteBody = stringResource(R.string.toggle_clipboard_suggestion_note_body),
        onNavigateBack = onNavigateBack,
    )
}

@Composable
fun HapticSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val settings by viewModel.settings.collectAsState()
    TogglePreferenceDetailScreen(
        title = stringResource(R.string.toggle_haptic_title),
        chip = stringResource(R.string.toggle_haptic_chip),
        headline = stringResource(R.string.toggle_haptic_headline),
        description = stringResource(R.string.toggle_haptic_description),
        icon = Icons.Default.TouchApp,
        checked = settings.hapticEnabled,
        onCheckedChange = viewModel::onHapticToggle,
        noteTitle = stringResource(R.string.toggle_haptic_note_title),
        noteBody = stringResource(R.string.toggle_haptic_note_body),
        onNavigateBack = onNavigateBack,
    )
}

@Composable
private fun TogglePreferenceDetailScreen(
    title: String,
    chip: String,
    headline: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    noteTitle: String,
    noteBody: String,
    onNavigateBack: () -> Unit,
) {
    SettingsPageScaffold(
        title = title,
        onNavigateBack = onNavigateBack,
    ) { baseModifier ->
        Column(
            modifier =
                baseModifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsHeroCard(
                chip = chip,
                title = headline,
                description = description,
                primaryIcon = icon,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSectionTitle(stringResource(R.string.toggle_current_section))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 1.dp,
                ) {
                    Column {
                        SettingsToggleRow(
                            title = title,
                            description =
                                if (checked) {
                                    stringResource(R.string.toggle_state_on)
                                } else {
                                    stringResource(R.string.toggle_state_off)
                                },
                            checked = checked,
                            onCheckedChange = onCheckedChange,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingsSectionTitle(stringResource(R.string.toggle_info_section))
                SettingsInfoBlock(
                    title = noteTitle,
                    body = noteBody,
                )
            }
        }
    }
}
