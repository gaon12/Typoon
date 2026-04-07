@file:Suppress("LongMethod", "LongParameterList")

package xyz.gaon.typoon.feature.result

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import xyz.gaon.typoon.R
import xyz.gaon.typoon.core.engine.ConversionDirection
import xyz.gaon.typoon.ui.components.AdaptiveContentContainer
import xyz.gaon.typoon.ui.components.AdaptiveTwoPane
import xyz.gaon.typoon.ui.components.TypoonWidthClass
import xyz.gaon.typoon.ui.components.rememberTypoonWidthClass
import xyz.gaon.typoon.ui.theme.MonoFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onNavigateBack: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val sourceText by viewModel.sourceText.collectAsState()
    val result by viewModel.result.collectAsState()
    val copyDone by viewModel.copyDone.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val editedResultText by viewModel.editedResultText.collectAsState()
    val isStarred by viewModel.isStarred.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val directionEngToKor = stringResource(R.string.result_direction_eng_to_kor)
    val directionKorToEng = stringResource(R.string.result_direction_kor_to_eng)
    val directionUnknown = stringResource(R.string.result_direction_unknown)
    val shareChooserTitle = stringResource(R.string.share_chooser_title)
    val widthClass = rememberTypoonWidthClass()
    val isExpanded = widthClass == TypoonWidthClass.Expanded

    ResultReviewPromptEffect(viewModel = viewModel)

    fun runHaptic() {
        if (settings.hapticEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            runHaptic()
                            onNavigateBack()
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        runHaptic()
                        viewModel.onToggleStar()
                    }) {
                        Icon(
                            if (isStarred) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription =
                                if (isStarred) {
                                    stringResource(R.string.result_remove_favorite)
                                } else {
                                    stringResource(R.string.result_add_favorite)
                                },
                            tint =
                                if (isStarred) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                },
                expandedHeight = 56.dp,
                windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            )
        },
    ) { innerPadding ->
        AdaptiveContentContainer(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            maxWidth = 1080.dp,
        ) {
            AdaptiveTwoPane(
                expanded = isExpanded,
                primary = {
                    ConversionCard(
                        label = stringResource(R.string.result_source),
                        text = sourceText,
                        isSource = true,
                        onCopy = {
                            runHaptic()
                            viewModel.onCopySource()
                        },
                    )
                },
                secondary = {
                    if (result != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text =
                                    when (result?.direction) {
                                        ConversionDirection.ENG_TO_KOR -> "  $directionEngToKor"
                                        ConversionDirection.KOR_TO_ENG -> "  $directionKorToEng"
                                        else -> "  $directionUnknown"
                                    },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    ResultCard(
                        text = editedResultText ?: result?.resultText ?: "",
                        isEdited = editedResultText != null,
                        onTextChange = viewModel::onResultEditChange,
                        onReset = {
                            runHaptic()
                            viewModel.onResultEditReset()
                        },
                        copyDone = copyDone,
                        onCopy = {
                            runHaptic()
                            viewModel.onCopyResult()
                        },
                        onShare = {
                            runHaptic()
                            viewModel.onShare { text ->
                                val intent =
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, text)
                                    }
                                context.startActivity(
                                    Intent.createChooser(intent, shareChooserTitle),
                                )
                            }
                        },
                        onReverse = {
                            runHaptic()
                            viewModel.onReverse()
                        },
                    )
                },
            )

            val confidence = result?.confidence ?: 1f
            if (confidence < settings.confidenceWarningThreshold) {
                val confidencePct = (confidence * 100).toInt()
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "  ${stringResource(R.string.result_warning)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            Text(
                                text = stringResource(R.string.result_confidence_pct, confidencePct),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.75f),
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.result_warning_action),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    text: String,
    isEdited: Boolean,
    onTextChange: (String) -> Unit,
    onReset: () -> Unit,
    copyDone: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onReverse: () -> Unit,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    val canExpand = remember(text) { needsExpandedTextUi(text) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.result_output),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                Row {
                    if (isEdited) {
                        IconButton(
                            onClick = onReset,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.result_restore_original),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.result_editable),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = if (expanded) 360.dp else 196.dp)
                        .animateContentSize(),
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = MonoFontFamily,
                    ),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                minLines = 2,
            )
            if (canExpand) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        if (expanded) {
                            stringResource(R.string.result_collapse)
                        } else {
                            stringResource(R.string.result_expand)
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = if (copyDone) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text =
                            if (copyDone) {
                                stringResource(R.string.result_done)
                            } else {
                                stringResource(R.string.common_copy)
                            },
                        fontSize = 13.sp,
                    )
                }
                FilledTonalButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.common_share), fontSize = 13.sp)
                }
                FilledTonalButton(
                    onClick = onReverse,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.common_reverse), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ConversionCard(
    label: String,
    text: String,
    isSource: Boolean,
    onCopy: (() -> Unit)? = null,
) {
    var expanded by rememberSaveable(label, text) { mutableStateOf(false) }
    val canExpand = remember(text) { needsExpandedTextUi(text) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSource) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        if (isSource) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = text.ifBlank { stringResource(R.string.result_none) },
                onValueChange = {},
                readOnly = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = if (expanded) 360.dp else 196.dp)
                        .animateContentSize(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = MonoFontFamily),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                minLines = 2,
            )
            if (canExpand || onCopy != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (canExpand) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(
                                if (expanded) {
                                    stringResource(R.string.result_collapse)
                                } else {
                                    stringResource(R.string.result_expand)
                                },
                            )
                        }
                    }
                    if (onCopy != null) {
                        TextButton(onClick = onCopy) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.result_copy_source), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun needsExpandedTextUi(text: String): Boolean = text.length > 140 || text.count { it == '\n' } >= 3
