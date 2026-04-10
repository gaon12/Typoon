package xyz.gaon.typoon.ui.components

import com.google.android.gms.ads.AdRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdBannerViewTest {
    @Test
    fun `detects dns filtered ad host as probable ad block`() {
        val detected =
            isProbableAdBlock(
                code = AdRequest.ERROR_CODE_INTERNAL_ERROR,
                domain = "com.google.android.gms.ads",
                message = "Unable to resolve host \"googleads.g.doubleclick.net\": No address associated with hostname",
            )

        assertTrue(detected)
    }

    @Test
    fun `detects blocked ad request from explicit blocker message`() {
        val detected =
            isProbableAdBlock(
                code = AdRequest.ERROR_CODE_NETWORK_ERROR,
                domain = "com.google.android.gms.ads",
                message = "Request blocked by ad blocker for googleads.g.doubleclick.net",
            )

        assertTrue(detected)
    }

    @Test
    fun `does not treat generic no fill as ad block`() {
        val detected =
            isProbableAdBlock(
                code = AdRequest.ERROR_CODE_NO_FILL,
                domain = "com.google.android.gms.ads",
                message = "No fill.",
            )

        assertFalse(detected)
    }

    @Test
    fun `does not treat unrelated network failure as ad block`() {
        val detected =
            isProbableAdBlock(
                code = AdRequest.ERROR_CODE_NETWORK_ERROR,
                domain = "com.google.android.gms.ads",
                message = "Timeout while connecting to analytics endpoint",
            )

        assertFalse(detected)
    }

    @Test
    fun `does not count no fill as relevant ad failure`() {
        assertFalse(shouldCountAsRelevantAdFailure(AdRequest.ERROR_CODE_NO_FILL))
        assertFalse(shouldCountAsRelevantAdFailure(AdRequest.ERROR_CODE_MEDIATION_NO_FILL))
    }

    @Test
    fun `shows notice only after repeated validated failures`() {
        val shouldShow =
            shouldShowProbableAdBlockNotice(
                failureCount = 3,
                isNetworkValidated = true,
                code = AdRequest.ERROR_CODE_NETWORK_ERROR,
                domain = "com.google.android.gms.ads",
                message = "Unable to resolve host \"googleads.g.doubleclick.net\": No address associated with hostname",
            )

        assertTrue(shouldShow)
    }

    @Test
    fun `does not show notice on first validated failure without strong blocker signal`() {
        val shouldShow =
            shouldShowProbableAdBlockNotice(
                failureCount = 1,
                isNetworkValidated = true,
                code = AdRequest.ERROR_CODE_INTERNAL_ERROR,
                domain = "com.google.android.gms.ads",
                message = "Internal error.",
            )

        assertFalse(shouldShow)
    }

    @Test
    fun `does not show notice when network is not validated`() {
        val shouldShow =
            shouldShowProbableAdBlockNotice(
                failureCount = 3,
                isNetworkValidated = false,
                code = AdRequest.ERROR_CODE_NETWORK_ERROR,
                domain = "com.google.android.gms.ads",
                message = "Unable to resolve host \"googleads.g.doubleclick.net\": No address associated with hostname",
            )

        assertFalse(shouldShow)
    }

    @Test
    fun `does not show notice for no fill even after repeated failures`() {
        val shouldShow =
            shouldShowProbableAdBlockNotice(
                failureCount = 5,
                isNetworkValidated = true,
                code = AdRequest.ERROR_CODE_NO_FILL,
                domain = "com.google.android.gms.ads",
                message = "No fill.",
            )

        assertFalse(shouldShow)
    }

    @Test
    fun `shows notice for repeated validated internal errors even without explicit blocker text`() {
        val shouldShow =
            shouldShowProbableAdBlockNotice(
                failureCount = 3,
                isNetworkValidated = true,
                code = AdRequest.ERROR_CODE_INTERNAL_ERROR,
                domain = "com.google.android.gms.ads",
                message = "Internal error.",
            )

        assertTrue(shouldShow)
    }

    @Test
    fun `shows notice early when blocker diagnostics are explicit`() {
        val shouldShow =
            shouldShowProbableAdBlockNotice(
                failureCount = 2,
                isNetworkValidated = true,
                code = AdRequest.ERROR_CODE_INTERNAL_ERROR,
                domain = "com.google.android.gms.ads",
                message = "Request blocked by ad blocker for googleads.g.doubleclick.net",
            )

        assertTrue(shouldShow)
    }
}
