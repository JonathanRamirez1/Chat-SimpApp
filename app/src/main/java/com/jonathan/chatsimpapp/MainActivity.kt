package com.jonathan.chatsimpapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.jonathan.chatsimpapp.ads.Ads
import com.jonathan.chatsimpapp.billing.BillingManager
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

    private val openChatRequests = Channel<String>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        // El splash se queda hasta saber si hay sesión y comunidad, para no "parpadear" pantallas.
        installSplashScreen().setKeepOnScreenCondition { session.state.value is CommunityState.Loading }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Ads.init(this)
        billing.restore()
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            ChatSimpAppTheme {
                AppNavHost(session = session.state, openChatRequests = openChatRequests.receiveAsFlow())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(NotificationHelper.EXTRA_CHAT_ID)?.let { openChatRequests.trySend(it) }
    }
}
