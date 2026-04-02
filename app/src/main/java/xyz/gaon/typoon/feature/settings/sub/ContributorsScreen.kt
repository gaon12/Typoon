package xyz.gaon.typoon.feature.settings.sub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.gaon.typoon.R
import xyz.gaon.typoon.feature.settings.SettingsHeroCard
import xyz.gaon.typoon.feature.settings.SettingsNavigationRow
import xyz.gaon.typoon.feature.settings.SettingsPageScaffold
import xyz.gaon.typoon.feature.settings.SettingsPanel
import xyz.gaon.typoon.feature.settings.SettingsRowDivider
import xyz.gaon.typoon.feature.settings.SettingsSectionTitle

private data class ContributorProfile(
    val name: String,
    val nickname: String,
    val githubUrl: String,
)

@Composable
fun ContributorsScreen(onNavigateBack: () -> Unit) {
    val contributors =
        remember {
            listOf(
                ContributorProfile(
                    name = "정가온",
                    nickname = "gaon12",
                    githubUrl = "https://github.com/gaon12",
                ),
            )
        }
    var selectedContributor by remember { mutableStateOf<ContributorProfile?>(null) }

    SettingsPageScaffold(
        title = stringResource(R.string.contributors_title),
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
                chip = stringResource(R.string.contributors_chip),
                title = stringResource(R.string.contributors_hero_title),
                description = stringResource(R.string.contributors_hero_description),
                primaryIcon = Icons.Default.People,
                secondaryIcon = Icons.Default.AutoAwesome,
            )

            ContributorPoster(
                contributors = contributors,
                onContributorClick = { selectedContributor = it },
            )

            Column(
                modifier = Modifier.padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingsSectionTitle(stringResource(R.string.contributors_section))
                SettingsPanel {
                    contributors.forEachIndexed { index, contributor ->
                        SettingsNavigationRow(
                            title = contributor.name,
                            description = stringResource(R.string.contributors_row_description),
                            value = "@${contributor.nickname}",
                            onClick = { selectedContributor = contributor },
                        )
                        if (index != contributors.lastIndex) {
                            SettingsRowDivider()
                        }
                    }
                }
            }
        }
    }

    selectedContributor?.let { contributor ->
        ContributorDetailDialog(
            contributor = contributor,
            onDismiss = { selectedContributor = null },
        )
    }
}

@Composable
private fun ContributorPoster(
    contributors: List<ContributorProfile>,
    onContributorClick: (ContributorProfile) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        val rowSize =
            when {
                contributors.size <= 1 -> 1
                contributors.size <= 4 -> 2
                else -> 3
            }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.surface,
                                ),
                        ),
                    ).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            contributors.chunked(rowSize).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowItems.forEach { contributor ->
                        ContributorPosterCell(
                            contributor = contributor,
                            modifier = Modifier.weight(1f),
                            featured = contributors.size == 1,
                            onClick = { onContributorClick(contributor) },
                        )
                    }
                    repeat(rowSize - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContributorPosterCell(
    contributor: ContributorProfile,
    modifier: Modifier = Modifier,
    featured: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            modifier
                .height(if (featured) 180.dp else 120.dp)
                .background(
                    color =
                        if (featured) {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
                        },
                    shape = MaterialTheme.shapes.extraLarge,
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (featured) {
                Box(
                    modifier =
                        Modifier
                            .size(14.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                                shape = CircleShape,
                            ),
                )
            }
            Text(
                text = contributor.name,
                style =
                    if (featured) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "@${contributor.nickname}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ContributorDetailDialog(
    contributor: ContributorProfile,
    onDismiss: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(contributor.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ContributorInfoLine(label = stringResource(R.string.contributors_label_name), value = contributor.name)
                ContributorInfoLine(
                    label = stringResource(R.string.contributors_label_nickname),
                    value = contributor.nickname,
                )
                ContributorInfoLine(
                    label = stringResource(R.string.contributors_label_github),
                    value = contributor.githubUrl,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
        dismissButton = {
            TextButton(onClick = { uriHandler.openUri(contributor.githubUrl) }) {
                Text(stringResource(R.string.contributors_open_github))
            }
        },
    )
}

@Composable
private fun ContributorInfoLine(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
