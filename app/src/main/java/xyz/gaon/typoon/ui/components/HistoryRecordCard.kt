@file:Suppress("LongMethod")

package xyz.gaon.typoon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.gaon.typoon.R
import xyz.gaon.typoon.core.data.db.ConversionEntity
import xyz.gaon.typoon.ui.theme.MonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryRecordCard(
    entity: ConversionEntity,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onToggleStar: (Long, Boolean) -> Unit,
    onClick: () -> Unit,
    showDeleteAction: Boolean = true,
) {
    val accentColor =
        when (entity.direction) {
            "KOR_TO_ENG" -> MaterialTheme.colorScheme.primary
            "ENG_TO_KOR" -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.outline
        }
    val starDescription = stringResource(R.string.history_record_star)
    val unstarDescription = stringResource(R.string.history_record_unstar)
    val copyDescription = stringResource(R.string.history_record_copy)
    val deleteDescription = stringResource(R.string.history_record_delete)
    val timestampText = formatHistoryTimestamp(entity.createdAt)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable(onClick = onClick)
                .height(IntrinsicSize.Min),
    ) {
        Box(
            modifier =
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(accentColor),
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = entity.sourceText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
                fontFamily = MonoFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entity.resultText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(
            modifier = Modifier.padding(end = 4.dp, top = 4.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = timestampText,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(end = 8.dp, top = 2.dp),
            )
            Row {
                IconButton(
                    onClick = { onToggleStar(entity.id, !entity.isStarred) },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = if (entity.isStarred) Icons.Default.Star else Icons.Outlined.StarOutline,
                        contentDescription = if (entity.isStarred) unstarDescription else starDescription,
                        modifier = Modifier.size(16.dp),
                        tint =
                            if (entity.isStarred) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            },
                    )
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = copyDescription,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    )
                }
                if (showDeleteAction) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = deleteDescription,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun formatHistoryTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> stringResource(R.string.history_record_now)
        diff < 3_600_000L -> stringResource(R.string.history_record_minutes_ago, diff / 60_000)
        diff < 86_400_000L -> stringResource(R.string.history_record_hours_ago, diff / 3_600_000)
        else -> {
            val recordYear =
                java.util.Calendar
                    .getInstance()
                    .apply { timeInMillis = timestamp }
                    .get(java.util.Calendar.YEAR)
            val thisYear =
                java.util.Calendar
                    .getInstance()
                    .get(java.util.Calendar.YEAR)
            val fmt = if (recordYear == thisYear) "MM.dd" else "yy.MM.dd"
            SimpleDateFormat(fmt, Locale.getDefault()).format(Date(timestamp))
        }
    }
}
