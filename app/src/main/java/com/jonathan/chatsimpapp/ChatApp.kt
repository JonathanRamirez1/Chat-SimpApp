package com.jonathan.chatsimpapp

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.jonathan.chatsimpapp.ads.AdsController
import com.jonathan.chatsimpapp.data.repository.AuthRepository
import com.jonathan.chatsimpapp.data.repository.PresenceRepository
import com.jonathan.chatsimpapp.data.repository.UserRepository
import com.jonathan.chatsimpapp.di.ApplicationScope
import com.jonathan.chatsimpapp.notifications.NotificationHelper
import com.jonathan.chatsimpapp.notifications.TopicSubscriptions
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ChatApp : Application() {

    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var users: UserRepository
    @Inject lateinit var presence: PresenceRepository
    @Inject @ApplicationScope lateinit var scope: CoroutineScope
    @Inject lateinit var ads: AdsController
    @Inject lateinit var topics: TopicSubscriptions

    override fun onCreate() {
        super.onCreate()
        // App Check impide que alguien use tu Firebase desde fuera de la app (y te genere costos).
        AppCheckInstaller.install()
        NotificationHelper.createChannels(this)
        ads.register(this)
        topics.start()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                ads.onAppForeground()
                if (auth.currentUserId == null) return
                // Una sola vez al abrir: "últ. vez" y token de notificaciones (en el Worker de Cloudflare).
                scope.launch {
                    presence.ping()
                    runCatching { users.registerCurrentFcmToken() }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                ads.onAppBackground()
            }
        })
    }
}
