package com.jonathan.chatsimpapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.jonathan.chatsimpapp.data.repository.AuthRepository
import com.jonathan.chatsimpapp.notifications.NotificationHelper
import com.jonathan.chatsimpapp.ui.navigation.AppNavHost
import com.jonathan.chatsimpapp.ui.theme.ChatSimpAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var auth: AuthRepository

    private val openChatRequests = Channel<String>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val signedIn = auth.authState.map { it != null }
            .stateIn(lifecycleScope, SharingStarted.Eagerly, auth.currentUserId != null)

        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            ChatSimpAppTheme {
                AppNavHost(signedIn = signedIn, openChatRequests = openChatRequests.receiveAsFlow())
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
