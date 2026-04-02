package xyz.gaon.typoon.feature.processtext

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import xyz.gaon.typoon.R
import xyz.gaon.typoon.feature.result.ResultReviewPromptEffect
import xyz.gaon.typoon.feature.result.ResultViewModel
import xyz.gaon.typoon.ui.components.ConversionBottomSheetContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessTextSheet(
    onDismiss: () -> Unit,
    onReplace: ((String) -> Unit)?,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val sourceText by viewModel.sourceText.collectAsState()
    val result by viewModel.result.collectAsState()
    val copyDone by viewModel.copyDone.collectAsState()
    val copyAndClose by viewModel.copyAndClose.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val shareChooserTitle = stringResource(R.string.share_chooser_title)

    ResultReviewPromptEffect(viewModel = viewModel)

    fun runHaptic() {
        if (settings.hapticEnabled) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    LaunchedEffect(copyAndClose) {
        if (copyAndClose) onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        ConversionBottomSheetContent(
            sourceText = sourceText,
            result = result,
            copyDone = copyDone,
            onDismiss = {
                runHaptic()
                onDismiss()
            },
            onCopy = {
                runHaptic()
                viewModel.onCopyResult(closeAfterCopy = onReplace == null)
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
            primaryActionLabel = if (onReplace != null) stringResource(R.string.process_text_replace) else null,
            onPrimaryAction =
                if (onReplace != null) {
                    {
                        runHaptic()
                        result?.resultText?.let(onReplace)
                    }
                } else {
                    null
                },
            confidenceThreshold = settings.confidenceWarningThreshold,
        )
    }
}
