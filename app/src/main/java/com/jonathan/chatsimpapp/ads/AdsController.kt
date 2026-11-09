package com.jonathan.chatsimpapp.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.core.content.edit
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.jonathan.chatsimpapp.BuildConfig
import com.jonathan.chatsimpapp.config.AppConfig
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.lang.ref.WeakReference
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anuncios a pantalla completa para comunidades del plan gratuito:
 *
 * - **Apertura (App Open)**: al abrir la app o volver a ella, como máximo cada [APP_OPEN_GAP_MS].
 * - **Intersticial**: solo en pausas naturales (después de publicar algo o al salir de una
 *   votación/reporte), con un mínimo de [INTERSTITIAL_GAP_MS] entre uno y otro y [INTERSTITIAL_DAILY_MAX] al día.
 * - **Recompensa**: opcional; el vecino lo elige para desbloquear algo (p. ej. un acta en PDF).
 *
 * Nunca se muestran dentro de un chat, durante una emergencia ni en comunidades Pro.
 * Los límites existen porque un vecino molesto desinstala, y sin vecinos no hay negocio.
 */
@Singleton
class AdsController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    session: CommunitySession,
    config: AppConfig,
    @ApplicationScope scope: CoroutineScope,
) : Application.ActivityLifecycleCallbacks {

    /** true si la comunidad abierta está en el plan gratuito y los anuncios están listos. */
    val enabled: StateFlow<Boolean> = combine(session.current, Ads.ready) { ctx, ready ->
        ready && ctx != null && !ctx.isPro && config.adsEnabled
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private val prefs = context.getSharedPreferences("ads", Context.MODE_PRIVATE)
    private var currentActivity = WeakReference<Activity>(null)
    private var appOpen: AppOpenAd? = null
    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    private var showingFullScreen = false
    private var lastFullScreenAt = 0L

    init {
        enabled.onEach { if (it) preload() }.launchIn(scope)
    }

    fun register(app: Application) = app.registerActivityLifecycleCallbacks(this)

    // ---------- Apertura ----------

    private var backgroundSince = 0L

    /** Llamar cuando la app pasa a segundo plano. */
    fun onAppBackground() {
        if (!showingFullScreen) backgroundSince = System.currentTimeMillis()
    }

    /** Llamar cuando la app pasa a primer plano. */
    fun onAppForeground() {
        val activity = currentActivity.get() ?: return
        val ad = appOpen ?: return
        if (!enabled.value || showingFullScreen) return
        val now = System.currentTimeMillis()
        // Volver de elegir una foto, compartir o pagar no cuenta como "abrir la app".
        if (backgroundSince != 0L && now - backgroundSince < MIN_AWAY_MS) return
        if (now - prefs.getLong(KEY_LAST_APP_OPEN, 0) < APP_OPEN_GAP_MS) return
        // La primera vez que alguien usa la app no se le recibe con un anuncio.
        if (!prefs.getBoolean(KEY_SEEN_BEFORE, false)) {
            prefs.edit { putBoolean(KEY_SEEN_BEFORE, true) }
            return
        }
        appOpen = null
        prefs.edit { putLong(KEY_LAST_APP_OPEN, now) }
        show(ad::show, ad::setFullScreenContentCallback, activity) { loadAppOpen() }
    }

    // ---------- Intersticial ----------

    /** Pausa natural: muestra un intersticial si toca según los límites; si no, no hace nada. */
    fun onNaturalBreak(activity: Activity?) {
        val ad = interstitial ?: return
        if (activity == null || !enabled.value || showingFullScreen) return
        val now = System.currentTimeMillis()
        if (now - lastFullScreenAt < INTERSTITIAL_GAP_MS) return
        val today = LocalDate.now().toString()
        val count = if (prefs.getString(KEY_DAY, null) == today) prefs.getInt(KEY_DAY_COUNT, 0) else 0
        if (count >= INTERSTITIAL_DAILY_MAX) return
        prefs.edit { putString(KEY_DAY, today); putInt(KEY_DAY_COUNT, count + 1) }
        interstitial = null
        show(ad::show, ad::setFullScreenContentCallback, activity) { loadInterstitial() }
    }

    // ---------- Recompensa ----------

    val rewardedReady: Boolean get() = rewarded != null

    /** Muestra el anuncio con recompensa; [onReward] se llama solo si el vecino lo ve completo. */
    fun showRewarded(activity: Activity?, onReward: () -> Unit): Boolean {
        val ad = rewarded ?: return false
        if (activity == null || showingFullScreen) return false
        rewarded = null
        showingFullScreen = true
        ad.fullScreenContentCallback = callback { loadRewarded() }
        ad.show(activity) { onReward() }
        return true
    }

    // ---------- Carga ----------

    private fun preload() {
        if (appOpen == null) loadAppOpen()
        if (interstitial == null) loadInterstitial()
        if (rewarded == null) loadRewarded()
    }

    private fun loadAppOpen() {
        if (!enabled.value) return
        AppOpenAd.load(context, BuildConfig.ADMOB_APP_OPEN_ID, AdRequest.Builder().build(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) { appOpen = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { appOpen = null }
        })
    }

    private fun loadInterstitial() {
        if (!enabled.value) return
        InterstitialAd.load(context, BuildConfig.ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null }
        })
    }

    private fun loadRewarded() {
        if (!enabled.value) return
        RewardedAd.load(context, BuildConfig.ADMOB_REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
            override fun onAdFailedToLoad(error: LoadAdError) { rewarded = null }
        })
    }

    private fun show(
        show: (Activity) -> Unit,
        setCallback: (FullScreenContentCallback) -> Unit,
        activity: Activity,
        reload: () -> Unit,
    ) {
        showingFullScreen = true
        setCallback(callback(reload))
        show(activity)
    }

    private fun callback(reload: () -> Unit) = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() = done()
        override fun onAdFailedToShowFullScreenContent(error: AdError) = done()
        private fun done() {
            showingFullScreen = false
            lastFullScreenAt = System.currentTimeMillis()
            reload()
        }
    }

    // ---------- Actividad visible ----------

    override fun onActivityStarted(activity: Activity) {
        if (!showingFullScreen) currentActivity = WeakReference(activity)
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit

    private companion object {
        const val APP_OPEN_GAP_MS = 4 * 60 * 60 * 1000L
        const val MIN_AWAY_MS = 2 * 60 * 1000L
        const val INTERSTITIAL_GAP_MS = 3 * 60 * 1000L
        const val INTERSTITIAL_DAILY_MAX = 6
        const val KEY_LAST_APP_OPEN = "last_app_open"
        const val KEY_SEEN_BEFORE = "seen_before"
        const val KEY_DAY = "day"
        const val KEY_DAY_COUNT = "day_count"
    }
}
