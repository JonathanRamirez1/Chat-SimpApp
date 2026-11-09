package com.jonathan.chatsimpapp.ads

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.jonathan.chatsimpapp.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Anuncios para comunidades del plan gratuito.
 * Antes de pedir anuncios se solicita el consentimiento (UMP), obligatorio en algunos países.
 */
object Ads {
    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    fun init(activity: Activity) {
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (consent.canRequestAds()) start(activity)
                }
            },
            { if (consent.canRequestAds()) start(activity) },
        )
        if (consent.canRequestAds()) start(activity)
    }

    private fun start(activity: Activity) {
        if (!started.compareAndSet(false, true)) return
        MobileAds.initialize(activity.applicationContext) { _ready.value = true }
    }
}

/** Banner adaptativo. No se muestra en vistas previas ni si los anuncios no están listos. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) return
    val width = LocalConfiguration.current.screenWidthDp
    AndroidView(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, width))
                adUnitId = BuildConfig.ADMOB_BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() },
    )
}
