@file:Suppress("LongMethod", "LongParameterList")

package xyz.gaon.typoon.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import xyz.gaon.typoon.BuildConfig
import xyz.gaon.typoon.R
import xyz.gaon.typoon.core.data.datastore.ThemeMode

@Composable
@Suppress("UnusedParameter")
fun SettingsScreen(
    scrollToTopTrigger: Int = 0,
    onNavigateBack: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToGeneral: () -> Unit,
    onNavigateToSaveHistory: () -> Unit,
    onNavigateToAutoReadClipboard: () -> Unit,
    onNavigateToAutoConvertClipboard: () -> Unit,
    onNavigateToHaptic: () -> Unit,
    onNavigateToDictionary: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onResetApp: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val context = LocalContext.current
    val exportSuccessMessage = stringResource(R.string.settings_export_success)
    val exportErrorMessage =
        when (val state = exportState) {
            is ExportState.Error -> stringResource(R.string.settings_export_error, state.message)
            else -> null
        }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showResetConfirm1 by remember { mutableStateOf(false) }
    var showResetConfirm2 by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) scrollState.animateScrollTo(0)
    }

    LaunchedEffect(viewModel) {
        viewModel.resetEvent.collect { onResetApp() }
    }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/csv"),
        ) { uri ->
            uri?.let { viewModel.onExportCsv(it) }
        }

    LaunchedEffect(exportState) {
        when (exportState) {
            is ExportState.Success -> {
                snackbarHostState.showSnackbar(exportSuccessMessage)
                viewModel.onExportStateDismissed()
            }
            is ExportState.Error -> {
                exportErrorMessage?.let { snackbarHostState.showSnackbar(it) }
                viewModel.onExportStateDismissed()
            }
            else -> Unit
        }
    }

    if (showDeleteConfirm) {
        DeleteHistoryConfirmDialog(
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                viewModel.onDeleteAllHistory()
                showDeleteConfirm = false
            },
        )
    }

    if (showResetConfirm1) {
        AppResetConfirmDialog1(
            onDismiss = { showResetConfirm1 = false },
            onConfirm = {
                showResetConfirm1 = false
                showResetConfirm2 = true
            },
        )
    }

    if (showResetConfirm2) {
        AppResetConfirmDialog2(
            onDismiss = { showResetConfirm2 = false },
            onConfirm = {
                showResetConfirm2 = false
                viewModel.onResetApp()
            },
        )
    }

    SettingsPageScaffold(
        title = stringResource(R.string.settings_title),
        onNavigateBack = null,
    ) { modifier ->
        Column(
            modifier =
                modifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsHeroCard(
                chip = stringResource(R.string.settings_hero_chip),
                title = stringResource(R.string.settings_hero_title),
                description = stringResource(R.string.settings_hero_body),
                primaryIcon = Icons.Default.Translate,
                secondaryIcon = Icons.Default.SettingsSuggest,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSectionTitle(stringResource(R.string.settings_section_quick))
                SettingsPanel {
                    SettingsToggleNavigationRow(
                        title = stringResource(R.string.settings_toggle_save_history_title),
                        description = stringResource(R.string.settings_toggle_save_history_summary),
                        checked = settings.saveHistory,
                        onCheckedChange = viewModel::onSaveHistoryToggle,
                        onOpenDetail = onNavigateToSaveHistory,
                    )
                    SettingsRowDivider()
                    SettingsToggleNavigationRow(
                        title = stringResource(R.string.settings_toggle_auto_read_title),
                        description = stringResource(R.string.settings_toggle_auto_read_summary),
                        checked = settings.autoReadClipboardOnLaunch,
                        onCheckedChange = viewModel::onAutoReadClipboardToggle,
                        onOpenDetail = onNavigateToAutoReadClipboard,
                    )
                    SettingsRowDivider()
                    SettingsToggleNavigationRow(
                        title = stringResource(R.string.settings_toggle_auto_convert_title),
                        description = stringResource(R.string.settings_toggle_auto_convert_summary),
                        checked = settings.autoConvertAfterClipboardRead,
                        onCheckedChange = viewModel::onAutoConvertClipboardToggle,
                        onOpenDetail = onNavigateToAutoConvertClipboard,
                    )
                    SettingsRowDivider()
                    SettingsToggleNavigationRow(
                        title = stringResource(R.string.settings_toggle_haptic_title),
                        description = stringResource(R.string.settings_toggle_haptic_summary),
                        checked = settings.hapticEnabled,
                        onCheckedChange = viewModel::onHapticToggle,
                        onOpenDetail = onNavigateToHaptic,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSectionTitle(stringResource(R.string.settings_section_detail))
                SettingsPanel {
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_theme_title),
                        description = stringResource(R.string.settings_theme_description),
                        value = themeLabel(settings.themeMode),
                        onClick = onNavigateToTheme,
                    )
                    SettingsRowDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_language_title),
                        description = stringResource(R.string.settings_language_summary),
                        value = languageLabel(settings.appLanguage),
                        onClick = onNavigateToLanguage,
                    )
                    SettingsRowDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_general_title),
                        description = stringResource(R.string.settings_general_summary),
                        value =
                            buildString {
                                append(settings.maxHistoryCount)
                                append(" · ")
                                append("${(settings.confidenceWarningThreshold * 100).toInt()}%")
                            },
                        onClick = onNavigateToGeneral,
                    )
                    SettingsRowDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_dictionary_title),
                        description = stringResource(R.string.settings_dictionary_summary),
                        onClick = onNavigateToDictionary,
                    )
                    SettingsRowDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_history_title),
                        description = stringResource(R.string.settings_history_summary),
                        onClick = onNavigateToHistory,
                    )
                    SettingsRowDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_privacy_title),
                        description = stringResource(R.string.settings_privacy_summary),
                        onClick = onNavigateToPrivacy,
                    )
                    SettingsRowDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_about_title),
                        description = stringResource(R.string.settings_about_summary),
                        value = BuildConfig.VERSION_NAME,
                        onClick = onNavigateToAbout,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSectionTitle(stringResource(R.string.settings_section_data))
                SettingsPanel {
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_export_csv_title),
                        description = stringResource(R.string.settings_export_csv_summary),
                        onClick = {
                            val timestamp = System.currentTimeMillis()
                            exportLauncher.launch("typoon_history_$timestamp.csv")
                        },
                    )
                    SettingsRowDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_delete_all_title),
                        description = stringResource(R.string.settings_delete_all_summary),
                        onClick = { showDeleteConfirm = true },
                        destructive = true,
                    )
                    SettingsRowDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_reset_title),
                        description = stringResource(R.string.settings_reset_summary),
                        onClick = { showResetConfirm1 = true },
                        destructive = true,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSectionTitle(stringResource(R.string.settings_section_project))
                SettingsPanel {
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_github_title),
                        description = stringResource(R.string.settings_github_summary),
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://github.com/gaon12/Typoon".toUri()),
                            )
                        },
                    )
                    SettingsRowDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.settings_donate_title),
                        description = stringResource(R.string.settings_donate_summary),
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://github.com/sponsors/gaon12".toUri()),
                            )
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DeleteHistoryConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_delete_dialog_title)) },
        text = { Text(stringResource(R.string.settings_delete_dialog_body)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text(stringResource(R.string.common_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun AppResetConfirmDialog1(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reset_dialog_1_title)) },
        text = { Text(stringResource(R.string.settings_reset_dialog_1_body)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text(stringResource(R.string.common_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun AppResetConfirmDialog2(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_reset_dialog_2_title)) },
        text = { Text(stringResource(R.string.settings_reset_dialog_2_body)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text(stringResource(R.string.settings_reset_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun themeLabel(mode: ThemeMode): String =
    when (mode) {
        ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
    }

@Composable
private fun languageLabel(mode: xyz.gaon.typoon.core.data.datastore.AppLanguage): String =
    when (mode) {
        xyz.gaon.typoon.core.data.datastore.AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
        xyz.gaon.typoon.core.data.datastore.AppLanguage.KOREAN -> stringResource(R.string.settings_language_korean)
        xyz.gaon.typoon.core.data.datastore.AppLanguage.ENGLISH -> stringResource(R.string.settings_language_english)
    }
