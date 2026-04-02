@file:Suppress("MatchingDeclarationName")

package xyz.gaon.typoon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class TypoonWidthClass {
    Compact,
    Medium,
    Expanded,
}

@Composable
fun rememberTypoonWidthClass(): TypoonWidthClass {
    val density = LocalDensity.current
    val widthDp =
        with(density) {
            LocalWindowInfo.current.containerSize.width
                .toDp()
        }
    return remember(widthDp) {
        when {
            widthDp >= 960.dp -> TypoonWidthClass.Expanded
            widthDp >= 600.dp -> TypoonWidthClass.Medium
            else -> TypoonWidthClass.Compact
        }
    }
}

@Composable
fun AdaptiveContentContainer(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 1120.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = maxWidth),
        ) {
            content()
        }
    }
}

@Composable
fun AdaptiveTwoPane(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 24.dp,
    primary: @Composable () -> Unit,
    secondary: @Composable () -> Unit,
) {
    if (expanded) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                primary()
            }
            Column(modifier = Modifier.weight(1f)) {
                secondary()
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(horizontalSpacing),
        ) {
            primary()
            secondary()
        }
    }
}
