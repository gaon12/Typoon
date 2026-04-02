package xyz.gaon.typoon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun TypoonSurface(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxSize(),
        color = scheme.background,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    scheme.background,
                                    scheme.surfaceContainerLow,
                                    scheme.surface,
                                ),
                        ),
                    ),
        ) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        scheme.primary.copy(alpha = 0.16f),
                                        Color.Transparent,
                                    ),
                            ),
                        ),
            )
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        Color.Transparent,
                                        scheme.tertiary.copy(alpha = 0.08f),
                                        Color.Transparent,
                                    ),
                            ),
                        ),
            )
            content()
        }
    }
}
