package xyz.gaon.typoon

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import xyz.gaon.typoon.core.data.datastore.AppLocaleManager
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.di.ConversionEngineExceptionSync
import xyz.gaon.typoon.core.di.FeedbackRateSync
import javax.inject.Inject

@HiltAndroidApp
class TypoonApp : Application() {
    @Inject
    lateinit var conversionEngineExceptionSync: ConversionEngineExceptionSync

    @Inject
    lateinit var feedbackRateSync: FeedbackRateSync

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        runBlocking {
            val appLanguage = AppPreferences(this@TypoonApp).settings.first().appLanguage
            AppLocaleManager.apply(appLanguage)
        }
        conversionEngineExceptionSync.start()
    }
}
