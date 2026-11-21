package com.jonathan.chatsimpapp.data.repository

import com.jonathan.chatsimpapp.data.model.Presence
import com.jonathan.chatsimpapp.data.remote.WorkerApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Últ. vez" sin Realtime Database (en el plan gratis de Firebase solo admite 100 conexiones a la vez).
 * La app avisa al Worker cuando se abre, y las pantallas consultan cuándo fue la última vez de cada
 * persona. Se considera "en línea" si abrió la app hace menos de [ONLINE_WINDOW_MS].
 * No hay "escribiendo…": hacerlo en vivo gastaría las peticiones que comparten todas las apps.
 */
@Singleton
class PresenceRepository @Inject constructor(
    private val api: WorkerApi,
) {
    suspend fun ping() {
        runCatching { api.ping() }
    }

    /** Consulta una vez y luego cada [REFRESH_MS] mientras la pantalla esté abierta. */
    fun observePresence(uids: Set<String>): Flow<Map<String, Presence>> = flow {
        if (uids.isEmpty()) {
            emit(emptyMap())
            return@flow
        }
        while (true) {
            val now = System.currentTimeMillis()
            val seen = runCatching { api.lastSeen(uids) }.getOrDefault(emptyMap())
            emit(seen.mapValues { (_, t) -> Presence(online = now - t < ONLINE_WINDOW_MS, lastChanged = t) })
            delay(REFRESH_MS)
        }
    }

    companion object {
        private const val ONLINE_WINDOW_MS = 3 * 60_000L
        private const val REFRESH_MS = 2 * 60_000L
    }
}
