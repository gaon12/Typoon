package xyz.gaon.typoon

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.gaon.typoon.core.data.datastore.AppLocaleManager
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.di.ApplicationScope
import xyz.gaon.typoon.core.di.ConversionEngineExceptionSync
import xyz.gaon.typoon.core.di.FeedbackRateSync
import javax.inject.Inject

@HiltAndroidApp
class TypoonApp : Application() {
    @Inject
    lateinit var conversionEngineExceptionSync: ConversionEngineExceptionSync

    @Inject
    lateinit var feedbackRateSync: FeedbackRateSync

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
        applicationScope.launch {
            val appLanguage = appPreferences.settings.first().appLanguage
            withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
                AppLocaleManager.apply(appLanguage)
            }
        }
        conversionEngineExceptionSync.start()
    }
}
