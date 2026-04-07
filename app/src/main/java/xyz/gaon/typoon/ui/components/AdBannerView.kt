package xyz.gaon.typoon.ui.components

import android.content.res.Configuration
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.core.net.toUri
import xyz.gaon.typoon.R

private const val BANNER_LINK = "https://github.com/sponsors/gaon12"

/**
 * 광고 배너 영역.
 *
 * 현재(애드몹 승인 전): 이미지 배너를 항상 표시합니다. 탭 시 후원 페이지로 이동합니다.
 * 애드몹 승인 후: AndroidView + AdView로 교체하고,
 *   광고 로드 실패(차단 포함) 시 이 이미지를 폴백으로 사용합니다.
 *
 * 리소스 구조:
 *   drawable-nodpi/ad_banner_fallback.png          — 기본(영어, 라이트)
 *   drawable-night-nodpi/ad_banner_fallback.png     — 영어, 다크
 *   drawable-ko-nodpi/ad_banner_fallback.png        — 한국어, 라이트
 *   drawable-ko-night-nodpi/ad_banner_fallback.png  — 한국어, 다크
 */
@Composable
fun AdBannerView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bannerBitmap =
        remember(isDarkTheme, configuration.locales.toLanguageTags()) {
            val overrideConfig =
                Configuration(context.resources.configuration).apply {
                    uiMode =
                        (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            if (isDarkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                }
            val themedContext = context.createConfigurationContext(overrideConfig)
            val rawBitmap =
                BitmapFactory.decodeResource(themedContext.resources, R.drawable.ad_banner_fallback)
                    ?: BitmapFactory.decodeResource(context.resources, R.drawable.ad_banner_fallback)
            trimTransparentEdges(rawBitmap)
        }

    Image(
        bitmap = bannerBitmap.asImageBitmap(),
        contentDescription = null,
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, BANNER_LINK.toUri()),
                    )
                },
        alignment = Alignment.TopCenter,
        contentScale = ContentScale.FillWidth,
        filterQuality = FilterQuality.High,
    )
}

private fun trimTransparentEdges(source: Bitmap): Bitmap {
    val width = source.width
    val height = source.height
    if (width <= 0 || height <= 0) return source

    val row = IntArray(width)
    var top = 0
    while (top < height) {
        source.getPixels(row, 0, width, 0, top, width, 1)
        if (row.any { (it ushr 24) != 0 }) break
        top++
    }

    var bottom = height - 1
    while (bottom >= top) {
        source.getPixels(row, 0, width, 0, bottom, width, 1)
        if (row.any { (it ushr 24) != 0 }) break
        bottom--
    }

    if (top == 0 && bottom == height - 1) {
        return source
    }
    if (top > bottom) {
        return source
    }

    val cropped = Bitmap.createBitmap(source, 0, top, width, bottom - top + 1)
    if (cropped != source) {
        source.recycle()
    }
    return cropped
}
