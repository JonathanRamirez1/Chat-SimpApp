package com.jonathan.chatsimpapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jonathan.chatsimpapp.ads.Ads
import com.jonathan.chatsimpapp.ads.AdsController
import com.jonathan.chatsimpapp.billing.BillingManager
import com.jonathan.chatsimpapp.config.AppConfig
import com.jonathan.chatsimpapp.update.ForcedUpdateManager
import com.jonathan.chatsimpapp.update.UpdateRequiredScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.data.repository.CommunityState
import com.jonathan.chatsimpapp.notifications.NotificationHelper
import com.jonathan.chatsimpapp.ui.navigation.AppNavHost
import com.jonathan.chatsimpapp.ui.theme.ChatSimpAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var session: CommunitySession
    @Inject lateinit var billing: BillingManager
    @Inject lateinit var ads: AdsController
    @Inject lateinit var config: AppConfig

    private lateinit var forcedUpdate: ForcedUpdateManager

    private val openChatRequests = Channel<String>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        // El splash se queda hasta saber si hay sesión y comunidad, para no "parpadear" pantallas.
        installSplashScreen().setKeepOnScreenCondition { session.state.value is CommunityState.Loading }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Actualización obligatoria (regla de RJ2): debe registrarse antes de que la Activity arranque.
        forcedUpdate = ForcedUpdateManager(this, config)
        forcedUpdate.onCreate()

        Ads.init(this)
        billing.restore()
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            ChatSimpAppTheme {
                val updateBlocked by forcedUpdate.blocked.collectAsStateWithLifecycle()
                if (updateBlocked) {
                    UpdateRequiredScreen(onUpdate = forcedUpdate::retry)
                    return@ChatSimpAppTheme
                }
                AppNavHost(
                    session = session.state,
                    openChatRequests = openChatRequests.receiveAsFlow(),
                    onNaturalBreak = { ads.onNaturalBreak(this) },
                    showRewarded = { onReward -> ads.showRewarded(this, onReward) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        forcedUpdate.onResume()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)?.let { openChatRequests.trySend(it) }
    }
}
