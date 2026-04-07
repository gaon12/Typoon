@file:Suppress("LongMethod", "CyclomaticComplexMethod", "MaxLineLength")

package xyz.gaon.typoon.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import xyz.gaon.typoon.R
import xyz.gaon.typoon.core.data.db.ConversionEntity
import xyz.gaon.typoon.ui.components.AdaptiveContentContainer
import xyz.gaon.typoon.ui.components.AdaptiveTwoPane
import xyz.gaon.typoon.ui.components.DismissibleSnackbarHost
import xyz.gaon.typoon.ui.components.HistoryRecordCard
import xyz.gaon.typoon.ui.components.TypoonWidthClass
import xyz.gaon.typoon.ui.components.rememberTypoonWidthClass
import xyz.gaon.typoon.ui.theme.MonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    shortcutClipboardToken: Int = 0,
    scrollToTopTrigger: Int = 0,
    onNavigateToResult: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val inputText by viewModel.inputText.collectAsState()
    val recentHistory by viewModel.recentHistory.collectAsState()
    val starredHistory by viewModel.starredHistory.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val clipboardSuggestion by viewModel.clipboardSuggestion.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val deletedMessage = stringResource(R.string.home_deleted_message)
    val undoLabel = stringResource(R.string.common_undo)
    val listState =
        androidx.compose.foundation.lazy
            .rememberLazyListState()
    val widthClass = rememberTypoonWidthClass()
    val isExpanded = widthClass == TypoonWidthClass.Expanded

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) listState.animateScrollToItem(0)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HomeUiEvent.NavigateToResult -> onNavigateToResult()
                is HomeUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is HomeUiEvent.ItemDeleted -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = deletedMessage,
                            actionLabel = undoLabel,
                            duration = SnackbarDuration.Short,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete()
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel, shortcutClipboardToken) {
        viewModel.onScreenEntered(forceReadClipboard = shortcutClipboardToken > 0)
    }

    fun runHaptic() {
        if (settings.hapticEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { DismissibleSnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Typoon",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        letterSpacing = (-0.8).sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            runHaptic()
                            onNavigateToSettings()
                        },
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.home_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                expandedHeight = 56.dp,
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        ) {
            item {
                AdaptiveContentContainer {
                    AdaptiveTwoPane(
                        expanded = isExpanded,
                        primary = {
                            if (settings.showHelpBanner) {
                                HomeHelpBanner(onDismiss = {
                                    runHaptic()
                                    viewModel.dismissHelpBanner()
                                })
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            clipboardSuggestion?.let { suggestion ->
                                ClipboardSuggestionCard(
                                    state = suggestion,
                                    onApply = {
                                        runHaptic()
                                        viewModel.applyClipboardSuggestion()
                                    },
                                    onDismiss = {
                                        runHaptic()
                                        viewModel.dismissClipboardSuggestion()
                                    },
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            InputSection(
                                inputText = inputText,
                                onInputChange = viewModel::onInputChange,
                                onClear = {
                                    runHaptic()
                                    viewModel.onClearInput()
                                },
                                onConvert = {
                                    runHaptic()
                                    viewModel.onConvert()
                                },
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ActionButtons(
                                inputText = inputText,
                                onReadClipboard = {
                                    runHaptic()
                                    viewModel.onReadClipboard()
                                },
                                onConvert = {
                                    runHaptic()
                                    viewModel.onConvert()
                                },
                            )
                        },
                        secondary = {
                            StatsCard(
                                totalConversions = stats.totalConversions,
                                totalChars = stats.totalCharsProcessed,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            HomeHistorySection(
                                recentHistory = recentHistory,
                                starredHistory = starredHistory,
                                onNavigateToHistory = {
                                    runHaptic()
                                    onNavigateToHistory()
                                },
                                onCopy = { entity ->
                                    runHaptic()
                                    viewModel.copyHistoryResult(entity)
                                },
                                onDelete = { entity ->
                                    runHaptic()
                                    viewModel.deleteHistoryItem(entity)
                                },
                                onToggleStar = { id, starred ->
                                    runHaptic()
                                    viewModel.onToggleStar(id, starred)
                                },
                                onOpen = { entity ->
                                    runHaptic()
                                    viewModel.loadHistoryToResult(entity)
                                    onNavigateToResult()
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHelpBanner(onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_help_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.home_help_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.home_hide_help),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ClipboardSuggestionCard(
    state: ClipboardSuggestionState,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = stringResource(R.string.home_suggestion_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                text = stringResource(R.string.home_suggestion_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
            )
            Text(
                text = stringResource(R.string.home_suggestion_source, state.originalText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.home_suggestion_result, state.suggestedText),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.home_suggestion_dismiss))
                }
                TextButton(onClick = onApply) {
                    Text(stringResource(R.string.home_suggestion_apply))
                }
            }
        }
    }
}

@Composable
private fun InputSection(
    inputText: String,
    onInputChange: (String) -> Unit,
    onClear: () -> Unit,
    onConvert: () -> Unit,
) {
    val counterColor =
        when {
            inputText.length >= HomeViewModel.MAX_INPUT_LENGTH -> MaterialTheme.colorScheme.error
            inputText.length >= 800 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        }
    Box {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth().height(140.dp),
            placeholder = {
                Text(
                    stringResource(R.string.home_input_placeholder),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontFamily = MonoFontFamily,
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFontFamily),
            shape = RoundedCornerShape(16.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            maxLines = 6,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions =
                KeyboardActions(onGo = {
                    if (inputText.isNotBlank()) onConvert()
                }),
        )
        Row(
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${inputText.length}/${HomeViewModel.MAX_INPUT_LENGTH}",
                fontSize = 11.sp,
                color = counterColor,
                fontFamily = MonoFontFamily,
            )
            if (inputText.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.home_clear_input),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    inputText: String,
    onReadClipboard: () -> Unit,
    onConvert: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onConvert,
            enabled = inputText.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(
                stringResource(R.string.home_convert),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        FilledTonalButton(
            onClick = onReadClipboard,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
            Text("  ${stringResource(R.string.home_read_clipboard)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "—",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun HomeHistorySection(
    recentHistory: List<ConversionEntity>,
    starredHistory: List<ConversionEntity>,
    onNavigateToHistory: () -> Unit,
    onCopy: (ConversionEntity) -> Unit,
    onDelete: (ConversionEntity) -> Unit,
    onToggleStar: (Long, Boolean) -> Unit,
    onOpen: (ConversionEntity) -> Unit,
) {
    if (starredHistory.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.home_favorites),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp,
                )
            }
            TextButton(onClick = onNavigateToHistory) {
                Text(stringResource(R.string.home_all))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            starredHistory.take(3).forEach { entity ->
                HistoryRecordCard(
                    entity = entity,
                    onCopy = { onCopy(entity) },
                    onDelete = {},
                    onToggleStar = onToggleStar,
                    onClick = { onOpen(entity) },
                    showDeleteAction = false,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_recent_history),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
        )
        TextButton(onClick = onNavigateToHistory) {
            Text(stringResource(R.string.home_view_all))
        }
    }

    if (recentHistory.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            recentHistory.forEach { entity ->
                HistoryRecordCard(
                    entity = entity,
                    onCopy = { onCopy(entity) },
                    onDelete = { onDelete(entity) },
                    onToggleStar = onToggleStar,
                    onClick = { onOpen(entity) },
                )
            }
        }
    } else {
        EmptyHistoryState()
    }
}
