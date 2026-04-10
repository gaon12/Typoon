package xyz.gaon.typoon.ui.components

import android.content.Intent
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay
import xyz.gaon.typoon.BuildConfig
import xyz.gaon.typoon.R

private const val BANNER_LINK = "https://github.com/sponsors/gaon12"
private const val TAG = "AdBannerView"
private const val MAX_AD_LOAD_ATTEMPTS = 3
private val AD_RETRY_DELAYS_MS = listOf(1_500L, 4_000L)

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
fun AdBannerView(
    modifier: Modifier = Modifier,
    onProbableAdBlockDetected: () -> Unit = {},
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val networkState by rememberNetworkValidationState(context)
    val currentOnProbableAdBlockDetected by rememberUpdatedState(onProbableAdBlockDetected)
    val currentNetworkState by rememberUpdatedState(networkState)
    var isAdLoaded by
        remember(configuration.orientation, configuration.screenWidthDp) {
            mutableStateOf(false)
        }
    var hasReportedProbableAdBlock by
        remember(configuration.orientation, configuration.screenWidthDp) {
            mutableStateOf(false)
        }
    var consecutiveRelevantFailures by
        remember(configuration.orientation, configuration.screenWidthDp) {
            mutableIntStateOf(0)
        }
    var loadAttempt by
        remember(configuration.orientation, configuration.screenWidthDp) {
            mutableIntStateOf(0)
        }
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
    val adWidthDp = configuration.screenWidthDp.coerceAtLeast(1)
    val adView =
        remember(context, adWidthDp) {
            AdView(context).apply {
                adUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidthDp))
                adListener =
                    object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                            consecutiveRelevantFailures = 0
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            isAdLoaded = false
                            val failureDiagnostics =
                                buildFailureDiagnostics(
                                    code = loadAdError.code,
                                    domain = loadAdError.domain,
                                    message = loadAdError.message,
                                    causeMessages = generateSequence(loadAdError.cause) { it.cause }.mapNotNull { it.message },
                                )
                            Log.w(
                                TAG,
                                "Banner ad failed: attempt=${loadAttempt + 1}/$MAX_AD_LOAD_ATTEMPTS, code=${loadAdError.code}, domain=${loadAdError.domain}, message=${loadAdError.message}, responseId=${loadAdError.responseInfo?.responseId}, networkValidated=${currentNetworkState.isValidated}, vpn=${currentNetworkState.isVpn}, diagnostics=$failureDiagnostics",
                            )

                            val nextFailureCount =
                                if (shouldCountAsRelevantAdFailure(loadAdError.code)) {
                                    consecutiveRelevantFailures + 1
                                } else {
                                    0
                                }
                            consecutiveRelevantFailures = nextFailureCount

                            if (
                                !hasReportedProbableAdBlock &&
                                shouldShowProbableAdBlockNotice(
                                    failureCount = nextFailureCount,
                                    loadAdError = loadAdError,
                                    isNetworkValidated = currentNetworkState.isValidated,
                                )
                            ) {
                                hasReportedProbableAdBlock = true
                                currentOnProbableAdBlockDetected()
                            }

                            if (loadAttempt < MAX_AD_LOAD_ATTEMPTS - 1) {
                                loadAttempt += 1
                            }
                        }
                    }
            }
        }

    androidx.compose.runtime.LaunchedEffect(adView, loadAttempt) {
        if (isAdLoaded) return@LaunchedEffect

        val retryDelayMs = AD_RETRY_DELAYS_MS.getOrElse(loadAttempt - 1) { 0L }
        if (retryDelayMs > 0) {
            delay(retryDelayMs)
        }
        adView.loadAd(AdRequest.Builder().build())
    }

    DisposableEffect(adView) {
        onDispose {
            adView.destroy()
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (isAdLoaded) {
            AndroidView(
                factory = { adView },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            FallbackBannerImage(
                context = context,
                bannerBitmap = bannerBitmap,
            )
        }
    }
}

internal fun isProbableAdBlock(loadAdError: LoadAdError): Boolean {
    return isProbableAdBlock(
        code = loadAdError.code,
        domain = loadAdError.domain,
        message = loadAdError.message,
        causeMessages = generateSequence(loadAdError.cause) { it.cause }.mapNotNull { it.message },
    )
}

internal fun isProbableAdBlock(
    code: Int,
    domain: String?,
    message: String?,
    causeMessages: Sequence<String> = emptySequence(),
): Boolean {
    val diagnostics = buildFailureDiagnostics(code, domain, message, causeMessages)

    val hasAdHostMarker =
        AD_HOST_MARKERS.any { marker ->
            diagnostics.contains(marker)
        }
    val hasBlockingMarker =
        BLOCKING_MARKERS.any { marker ->
            diagnostics.contains(marker)
        }
    val hasDnsFailureMarker =
        DNS_FAILURE_MARKERS.any { marker ->
            diagnostics.contains(marker)
        }
    val hasConnectionFailureMarker =
        CONNECTION_FAILURE_MARKERS.any { marker ->
            diagnostics.contains(marker)
        }

    return hasAdHostMarker &&
        (
            hasBlockingMarker ||
                hasDnsFailureMarker ||
                (code == AdRequest.ERROR_CODE_NETWORK_ERROR && hasConnectionFailureMarker)
        )
}

internal fun shouldCountAsRelevantAdFailure(code: Int): Boolean =
    code != AdRequest.ERROR_CODE_NO_FILL && code != AdRequest.ERROR_CODE_MEDIATION_NO_FILL

internal fun shouldShowProbableAdBlockNotice(
    failureCount: Int,
    loadAdError: LoadAdError,
    isNetworkValidated: Boolean,
): Boolean =
    shouldShowProbableAdBlockNotice(
        failureCount = failureCount,
        isNetworkValidated = isNetworkValidated,
        code = loadAdError.code,
        domain = loadAdError.domain,
        message = loadAdError.message,
        causeMessages = generateSequence(loadAdError.cause) { it.cause }.mapNotNull { it.message },
    )

internal fun shouldShowProbableAdBlockNotice(
    failureCount: Int,
    isNetworkValidated: Boolean,
    code: Int,
    domain: String?,
    message: String?,
    causeMessages: Sequence<String> = emptySequence(),
): Boolean {
    if (!isNetworkValidated) return false
    if (!shouldCountAsRelevantAdFailure(code)) return false
    if (failureCount < 2) return false

    if (
        isProbableAdBlock(
            code = code,
            domain = domain,
            message = message,
            causeMessages = causeMessages,
        ) || code == AdRequest.ERROR_CODE_NETWORK_ERROR
    ) {
        return true
    }

    return failureCount >= MAX_AD_LOAD_ATTEMPTS
}

private fun buildFailureDiagnostics(
    code: Int,
    domain: String?,
    message: String?,
    causeMessages: Sequence<String>,
): String =
    buildList {
        add("code=$code")
        if (domain != null) add(domain)
        if (message != null) add(message)
        addAll(causeMessages)
    }.joinToString(separator = " | ").lowercase()

private val AD_HOST_MARKERS =
    listOf(
        "googleads",
        "doubleclick",
        "admob",
        "adsense",
        "pagead",
        "adservice",
    )

private val BLOCKING_MARKERS =
    listOf(
        "ad blocker",
        "adblock",
        "blocked",
        "dns filter",
        "private dns",
        "content filter",
    )

private val DNS_FAILURE_MARKERS =
    listOf(
        "unable to resolve host",
        "name_not_resolved",
        "host lookup",
        "no address associated with hostname",
        "nxdomain",
    )

private val CONNECTION_FAILURE_MARKERS =
    listOf(
        "connection refused",
        "connection reset",
        "timed out",
        "timeout",
        "network is unreachable",
        "net::err",
    )

private data class NetworkValidationState(
    val isValidated: Boolean = false,
    val isVpn: Boolean = false,
)

@Composable
private fun rememberNetworkValidationState(context: Context): androidx.compose.runtime.State<NetworkValidationState> {
    val appContext = context.applicationContext
    val connectivityManager =
        remember(appContext) {
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        }
    val state =
        remember {
            mutableStateOf(connectivityManager.currentNetworkValidationState())
        }

    DisposableEffect(connectivityManager) {
        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    state.value = connectivityManager.currentNetworkValidationState()
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    state.value =
                        NetworkValidationState(
                            isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
                            isVpn = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN),
                        )
                }

                override fun onLost(network: Network) {
                    state.value = connectivityManager.currentNetworkValidationState()
                }
            }

        connectivityManager.registerDefaultNetworkCallback(callback)
        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    return state
}

private fun ConnectivityManager.currentNetworkValidationState(): NetworkValidationState {
    val capabilities = getNetworkCapabilities(activeNetwork)
    return NetworkValidationState(
        isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
        isVpn = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true,
    )
}

@Composable
private fun FallbackBannerImage(
    context: android.content.Context,
    bannerBitmap: Bitmap,
) {
    Image(
        bitmap = bannerBitmap.asImageBitmap(),
        contentDescription = null,
        modifier =
            Modifier
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

    return if (width <= 0 || height <= 0) {
        source
    } else {
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

        val shouldKeepSource = (top == 0 && bottom == height - 1) || top > bottom
        if (shouldKeepSource) {
            source
        } else {
            Bitmap.createBitmap(source, 0, top, width, bottom - top + 1).also { cropped ->
                if (cropped != source) {
                    source.recycle()
                }
            }
        }
    }
}
