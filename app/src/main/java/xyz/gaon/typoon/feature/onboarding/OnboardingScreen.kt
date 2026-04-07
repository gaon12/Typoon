@file:Suppress("LongMethod")

package xyz.gaon.typoon.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import xyz.gaon.typoon.R
import xyz.gaon.typoon.ui.components.TypoonActionButton
import xyz.gaon.typoon.ui.components.TypoonSurface

private data class OnboardingSlide(
    val title: String,
    val body: String,
    val icon: ImageVector,
    val accentLabel: String,
    val visual: OnboardingVisual,
)

private enum class OnboardingVisual {
    Conversion,
    Clipboard,
    Share,
}

@Composable
fun OnboardingRoute(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    OnboardingScreen(
        modifier = modifier,
        onSkip = {
            viewModel.onComplete()
            onFinish()
        },
        onGetStarted = {
            viewModel.onComplete()
            onFinish()
        },
    )
}

@Composable
fun OnboardingScreen(
    onSkip: () -> Unit,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slides =
        listOf(
            OnboardingSlide(
                title = stringResource(R.string.onboarding_slide_1_title),
                body = stringResource(R.string.onboarding_slide_1_body),
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                accentLabel = stringResource(R.string.onboarding_slide_1_accent),
                visual = OnboardingVisual.Conversion,
            ),
            OnboardingSlide(
                title = stringResource(R.string.onboarding_slide_2_title),
                body = stringResource(R.string.onboarding_slide_2_body),
                icon = Icons.Default.ContentPaste,
                accentLabel = stringResource(R.string.onboarding_slide_2_accent),
                visual = OnboardingVisual.Clipboard,
            ),
            OnboardingSlide(
                title = stringResource(R.string.onboarding_slide_3_title),
                body = stringResource(R.string.onboarding_slide_3_body),
                icon = Icons.Default.Share,
                accentLabel = stringResource(R.string.onboarding_slide_3_accent),
                visual = OnboardingVisual.Share,
            ),
        )
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == slides.lastIndex

    TypoonSurface(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.onboarding_skip))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) { page ->
                val slide = slides[page]
                val pageScrollState = rememberScrollState()
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(pageScrollState)
                            .padding(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                        ) {
                            Text(
                                text = slide.accentLabel,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }

                        Text(
                            text = slide.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = slide.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    OnboardingIllustration(
                        slide = slide,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.extraLarge,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text =
                                    when (page) {
                                        0 -> stringResource(R.string.onboarding_slide_1_note)
                                        1 -> stringResource(R.string.onboarding_slide_2_note)
                                        else -> stringResource(R.string.onboarding_slide_3_note)
                                    },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(slides.size) { index ->
                    val selected = index == pagerState.currentPage
                    Box(
                        modifier =
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = if (selected) 26.dp else 10.dp, height = 10.dp)
                                .background(
                                    color =
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                        },
                                    shape = CircleShape,
                                ),
                    )
                }
            }

            Text(
                text = stringResource(R.string.onboarding_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            TypoonActionButton(
                text =
                    if (isLastPage) {
                        stringResource(R.string.onboarding_start)
                    } else {
                        stringResource(R.string.onboarding_next)
                    },
                onClick = {
                    if (isLastPage) {
                        onGetStarted()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun OnboardingIllustration(
    slide: OnboardingSlide,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        when (slide.visual) {
            OnboardingVisual.Conversion -> OnboardingConversionIllustration()
            OnboardingVisual.Clipboard -> OnboardingClipboardIllustration()
            OnboardingVisual.Share -> OnboardingShareIllustration()
        }
    }
}

@Composable
private fun OnboardingConversionIllustration() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        IllustrationHeader(
            icon = Icons.AutoMirrored.Filled.CompareArrows,
            title = stringResource(R.string.onboarding_slide_1_accent),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IllustrationTextBlock(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.onboarding_sample_conversion_source),
                subtitle = stringResource(R.string.onboarding_illustration_conversion_source),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            IllustrationTextBlock(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.onboarding_sample_conversion_result),
                subtitle = stringResource(R.string.onboarding_illustration_conversion_result),
            )
        }
    }
}

@Composable
private fun OnboardingClipboardIllustration() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        IllustrationHeader(
            icon = Icons.Default.ContentPaste,
            title = stringResource(R.string.onboarding_illustration_clipboard_title),
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_illustration_clipboard_recent),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.onboarding_sample_clipboard_text),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.onboarding_illustration_clipboard_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OnboardingShareIllustration() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        IllustrationHeader(
            icon = Icons.Default.Share,
            title = stringResource(R.string.onboarding_illustration_share_title),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IllustrationMiniCard(
                title = stringResource(R.string.onboarding_sample_share_source),
                subtitle = stringResource(R.string.onboarding_illustration_share_source),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
            )
            IllustrationMiniCard(
                title = stringResource(R.string.onboarding_sample_share_result),
                subtitle = stringResource(R.string.onboarding_illustration_share_result),
            )
        }
    }
}

@Composable
private fun IllustrationHeader(
    icon: ImageVector,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(12.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun IllustrationTextBlock(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun IllustrationMiniCard(
    title: String,
    subtitle: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .width(120.dp)
                    .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
