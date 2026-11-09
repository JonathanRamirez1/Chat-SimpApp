package com.jonathan.chatsimpapp.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.jonathan.chatsimpapp.data.model.Presence
import com.jonathan.chatsimpapp.util.valueFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Presencia ("en línea" / "últ. vez") y estado "escribiendo" con Realtime Database.
 * Se usa RTDB porque `onDisconnect()` marca al usuario como desconectado aunque la app muera
 * o se pierda la red, algo que Firestore no puede hacer.
 */
@Singleton
class PresenceRepository @Inject constructor(
    private val database: FirebaseDatabase,
) {
    private val connectedRef by lazy { database.getReference(".info/connected") }
    private var connectionListener: ValueEventListener? = null

    private fun statusRef(uid: String) = database.getReference("status").child(uid)
    private fun typingRef(chatId: String) = database.getReference("typing").child(chatId)

    private val onlineValue get() = mapOf("state" to "online", "lastChanged" to ServerValue.TIMESTAMP)
    private val offlineValue get() = mapOf("state" to "offline", "lastChanged" to ServerValue.TIMESTAMP)

    /** Marca al usuario en línea mientras haya conexión; se re-registra si la conexión vuelve. */
    fun goOnline(uid: String) {
        if (connectionListener != null) return
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.getValue(Boolean::class.java) != true) return
                statusRef(uid).onDisconnect().setValue(offlineValue).addOnSuccessListener {
                    statusRef(uid).setValue(onlineValue)
                }
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        connectionListener = listener
        connectedRef.addValueEventListener(listener)
    }

    fun goOffline(uid: String) {
        connectionListener?.let { connectedRef.removeEventListener(it) }
        connectionListener = null
        statusRef(uid).setValue(offlineValue)
    }

    fun observePresence(uid: String): Flow<Presence> =
        statusRef(uid).valueFlow()
            .map { snap ->
                Presence(
                    online = snap.child("state").getValue(String::class.java) == "online",
                    lastChanged = snap.child("lastChanged").getValue(Long::class.java),
                )
            }
            .catch { emit(Presence()) }

    fun setTyping(chatId: String, uid: String, typing: Boolean) {
        val ref = typingRef(chatId).child(uid)
        if (typing) {
            ref.onDisconnect().removeValue()
            ref.setValue(true)
        } else {
            ref.removeValue()
        }
    }

    /** Uids que están escribiendo en el chat (incluye el propio; filtrar en la UI). */
    fun observeTyping(chatId: String): Flow<Set<String>> =
        typingRef(chatId).valueFlow()
            .map { snap -> snap.children.mapNotNull { it.key }.toSet() }
            .catch { emit(emptySet()) }
}
