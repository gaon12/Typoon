package xyz.gaon.typoon.ui.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
    val painter = painterResource(R.drawable.ad_banner_fallback)
    val intrinsicSize = painter.intrinsicSize
    val aspectRatio =
        if (intrinsicSize.isSpecified && intrinsicSize.height != 0f) {
            intrinsicSize.width / intrinsicSize.height
        } else {
            6f
        }

    Image(
        painter = painter,
        contentDescription = null,
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .padding(vertical = 2.dp)
                .clickable(role = Role.Button) {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, BANNER_LINK.toUri()),
                    )
                },
        contentScale = ContentScale.Fit,
    )
}
