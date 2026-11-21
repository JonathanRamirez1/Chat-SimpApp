package com.jonathan.chatsimpapp.notifications

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import com.jonathan.chatsimpapp.data.repository.CommunitySession
import com.jonathan.chatsimpapp.data.repository.CommunityState
import com.jonathan.chatsimpapp.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Temas de notificaciones de la comunidad (mismos nombres que el Worker, ver server/src/fcm.ts):
 * `c_<comunidad>` para avisos, emergencias, votaciones y alertas del barrio, y
 * `c_<comunidad>_u_<unidad>` para los reportes que afectan a tu unidad.
 * Así el Worker hace un solo envío por comunidad en vez de uno por vecino.
 */
@Singleton
class TopicSubscriptions @Inject constructor(
    @ApplicationContext context: Context,
    private val session: CommunitySession,
    private val messaging: FirebaseMessaging,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val prefs = context.getSharedPreferences("topics", Context.MODE_PRIVATE)

    fun start() {
        session.state.onEach { state ->
            val current = prefs.getStringSet(KEY, emptySet()).orEmpty()
            val desired: Set<String> = when (state) {
                is CommunityState.Ready -> {
                    val ctx = state.context
                    val ids = ctx.all.map { it.id }.toSet()
                    // Se conservan los temas de unidad de otras comunidades a las que sigues perteneciendo.
                    val keptUnits = current.filter { t -> ids.any { t.startsWith(communityTopic(it) + "_u_") } }
                    ids.map(::communityTopic).toSet() + keptUnits + unitTopic(ctx.id, ctx.myUnit)
                }
                CommunityState.SignedOut, CommunityState.None -> emptySet()
                CommunityState.Loading -> return@onEach
            }
            (desired - current).forEach { runCatching { messaging.subscribeToTopic(it).await() } }
            (current - desired).forEach { runCatching { messaging.unsubscribeFromTopic(it).await() } }
            prefs.edit { putStringSet(KEY, desired) }
        }.launchIn(scope)
    }

    companion object {
        private const val KEY = "subscribed"

        fun communityTopic(cid: String) = "c_$cid"

        fun unitTopic(cid: String, unit: String) = "c_${cid}_u_${sanitize(unit)}"

        /** FCM acepta [a-zA-Z0-9-_.~%] en los temas (igual que sanitizeTopicPart del Worker). */
        fun sanitize(value: String): String =
            value.trim().uppercase().replace(Regex("[^A-Z0-9\\-_.~]"), "_").take(40)
    }
}
