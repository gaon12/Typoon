package xyz.gaon.typoon.feature.settings.sub

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.gaon.typoon.BuildConfig
import xyz.gaon.typoon.R
import xyz.gaon.typoon.feature.settings.SettingsHeroCard
import xyz.gaon.typoon.feature.settings.SettingsPageScaffold
import xyz.gaon.typoon.feature.settings.SettingsSectionTitle
import xyz.gaon.typoon.feature.settings.SettingsValuePill

private data class LicenseItem(
    val name: String,
    val version: String,
    val licenseName: String,
    val note: String,
    val licenseText: String,
    val noticeText: String? = null,
)

private data class LicenseSpec(
    val name: String,
    val version: String,
    val licenseName: String,
    val noteResId: Int,
    val licenseAssetPath: String,
    val noticeAssetPath: String? = null,
)

@Composable
fun OpenSourceLicensesScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val items = remember(context) { buildLicenseItems(context) }
    val appLicense = items.first()
    val libraryLicenses = items.drop(1)
    var selectedLicense by remember { mutableStateOf<LicenseItem?>(null) }

    SettingsPageScaffold(
        title = stringResource(R.string.open_source_title),
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
                chip = stringResource(R.string.open_source_chip),
                title = stringResource(R.string.open_source_hero_title),
                description = stringResource(R.string.open_source_hero_description),
                primaryIcon = Icons.Default.Description,
                secondaryIcon = Icons.Default.Verified,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsSectionTitle(stringResource(R.string.open_source_app_license_section))
                LicenseItemCard(
                    item = appLicense,
                    onClick = { selectedLicense = appLicense },
                )
            }

            LicenseLibrarySection(
                items = libraryLicenses,
                onItemClick = { selectedLicense = it },
            )
        }
    }

    selectedLicense?.let { item ->
        LicenseTextDialog(
            item = item,
            onDismiss = { selectedLicense = null },
        )
    }
}

private fun buildLicenseItems(context: Context): List<LicenseItem> =
    listOf(appLicenseItem(context)) +
        openSourceLibrarySpecs().map { spec ->
            LicenseItem(
                name = spec.name,
                version = spec.version,
                licenseName = spec.licenseName,
                note = context.getString(spec.noteResId),
                licenseText = context.readAssetText(spec.licenseAssetPath),
                noticeText = spec.noticeAssetPath?.let(context::readAssetText),
            )
        }

private fun appLicenseItem(context: Context): LicenseItem =
    LicenseItem(
        name = "Typoon",
        version = BuildConfig.VERSION_NAME,
        licenseName = "MIT License",
        note = context.getString(R.string.open_source_note_app),
        licenseText = context.readAssetText("licenses/Typoon-LICENSE.txt"),
    )

private fun openSourceLibrarySpecs(): List<LicenseSpec> =
    listOf(
        LicenseSpec(
            name = "AndroidX Core KTX",
            version = "1.18.0",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_core_ktx,
            licenseAssetPath = "licenses/androidx-core-ktx-LICENSE.txt",
        ),
        LicenseSpec(
            name = "AndroidX AppCompat",
            version = "1.7.0",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_appcompat,
            licenseAssetPath = "licenses/androidx-appcompat-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Activity Compose",
            version = "1.13.0",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_activity_compose,
            licenseAssetPath = "licenses/androidx-activity-compose-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Compose UI",
            version = "1.7.0",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_compose_ui,
            licenseAssetPath = "licenses/androidx-compose-ui-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Compose UI Graphics",
            version = "1.7.0",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_compose_ui_graphics,
            licenseAssetPath = "licenses/androidx-compose-ui-graphics-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Compose UI Tooling Preview",
            version = "1.7.0",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_compose_ui_tooling_preview,
            licenseAssetPath = "licenses/androidx-compose-ui-tooling-preview-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Material 3",
            version = "1.3.0",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_material3,
            licenseAssetPath = "licenses/androidx-compose-material3-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Material Icons Extended",
            version = "1.7.0",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_material_icons,
            licenseAssetPath = "licenses/androidx-compose-material-icons-extended-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Lifecycle Runtime KTX",
            version = "2.10.0",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_lifecycle_runtime,
            licenseAssetPath = "licenses/androidx-lifecycle-runtime-ktx-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Lifecycle ViewModel Compose",
            version = "2.10.0",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_lifecycle_viewmodel_compose,
            licenseAssetPath = "licenses/androidx-lifecycle-viewmodel-compose-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Navigation Compose",
            version = "2.9.0",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_navigation,
            licenseAssetPath = "licenses/androidx-navigation-compose-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Room Runtime",
            version = "2.7.1",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_room_runtime,
            licenseAssetPath = "licenses/androidx-room-runtime-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Room KTX",
            version = "2.7.1",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_room_ktx,
            licenseAssetPath = "licenses/androidx-room-ktx-LICENSE.txt",
        ),
        LicenseSpec(
            name = "DataStore Preferences",
            version = "1.1.4",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_datastore,
            licenseAssetPath = "licenses/androidx-datastore-preferences-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Hilt Android",
            version = "2.59.2",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_hilt_android,
            licenseAssetPath = "licenses/google-dagger-hilt-android-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Hilt Navigation Compose",
            version = "1.2.0",
            licenseName = "Apache License 2.0",
            noteResId = R.string.open_source_note_hilt_navigation,
            licenseAssetPath = "licenses/androidx-hilt-navigation-compose-LICENSE.txt",
        ),
        LicenseSpec(
            name = "Google Play Review",
            version = "2.0.2",
            licenseName = "Play Core SDK Terms",
            noteResId = R.string.open_source_note_play_review,
            licenseAssetPath = "licenses/google-play-review-LICENSE.txt",
            noticeAssetPath = "licenses/google-play-review-NOTICE.txt",
        ),
        LicenseSpec(
            name = "Google Play Review KTX",
            version = "2.0.2",
            licenseName = "Play Core SDK Terms",
            noteResId = R.string.open_source_note_play_review_ktx,
            licenseAssetPath = "licenses/google-play-review-ktx-LICENSE.txt",
            noticeAssetPath = "licenses/google-play-review-ktx-NOTICE.txt",
        ),
        LicenseSpec(
            name = "Google Mobile Ads",
            version = "24.2.0",
            licenseName = "Android SDK License",
            noteResId = R.string.open_source_note_ads,
            licenseAssetPath = "licenses/google-play-services-ads-LICENSE.txt",
            noticeAssetPath = "licenses/google-play-services-ads-NOTICE.txt",
        ),
    )

@Composable
private fun LicenseLibrarySection(
    items: List<LicenseItem>,
    onItemClick: (LicenseItem) -> Unit,
) {
    Column(
        modifier = Modifier.padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsSectionTitle(stringResource(R.string.open_source_section))
        items.forEach { item ->
            LicenseItemCard(
                item = item,
                onClick = { onItemClick(item) },
            )
        }
    }
}

@Composable
private fun LicenseItemCard(
    item: LicenseItem,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    )
                }
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsValuePill(text = item.version)
                SettingsValuePill(text = item.licenseName)
                if (item.noticeText != null) {
                    SettingsValuePill(text = stringResource(R.string.open_source_notice_pill))
                }
            }
        }
    }
}

@Composable
private fun LicenseTextDialog(
    item: LicenseItem,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsValuePill(text = item.licenseName)
                    if (item.noticeText != null) {
                        SettingsValuePill(text = stringResource(R.string.open_source_notice_pill))
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.licenseText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                item.noticeText?.let { notice ->
                    Text(
                        text = stringResource(R.string.open_source_notice_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = notice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
}

private fun Context.readAssetText(path: String): String = assets.open(path).bufferedReader().use { it.readText() }
