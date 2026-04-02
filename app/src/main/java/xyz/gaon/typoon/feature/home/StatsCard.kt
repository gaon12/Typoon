package xyz.gaon.typoon.feature.home

import android.icu.text.CompactDecimalFormat
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.gaon.typoon.R
import xyz.gaon.typoon.ui.theme.MonoFontFamily
import java.util.Locale

@Composable
fun StatsCard(
    totalConversions: Int,
    totalChars: Int,
) {
    if (totalConversions == 0) return

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatItem(
            value = totalConversions.toCompactString(),
            label = stringResource(R.string.home_stats_total_conversions),
            valueColor = MaterialTheme.colorScheme.primary,
        )

        Box(
            modifier =
                Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        )

        StatItem(
            value = totalChars.toCompactString(),
            label = stringResource(R.string.home_stats_total_chars),
            valueColor = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    valueColor: androidx.compose.ui.graphics.Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MonoFontFamily,
            color = valueColor,
            letterSpacing = (-0.5).sp,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            letterSpacing = 0.5.sp,
        )
    }
}

internal fun Int.toCompactString(locale: Locale = Locale.getDefault()): String {
    val formatter =
        CompactDecimalFormat.getInstance(
            ULocale.forLocale(locale),
            CompactDecimalFormat.CompactStyle.SHORT,
        )
    formatter.maximumFractionDigits = 0
    return formatter.format(this.toLong())
}
