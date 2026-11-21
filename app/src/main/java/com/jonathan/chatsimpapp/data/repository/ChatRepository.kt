package com.jonathan.chatsimpapp.data.repository

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jonathan.chatsimpapp.data.model.Chat
import com.jonathan.chatsimpapp.data.model.Message
import com.jonathan.chatsimpapp.data.remote.WorkerApi
import com.jonathan.chatsimpapp.util.ChatIds
import com.jonathan.chatsimpapp.util.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val api: WorkerApi,
) {
    private val chats get() = db.collection(CHATS)
    private fun messages(chatId: String) = chats.document(chatId).collection(MESSAGES)

    /** Conversaciones del usuario, la más reciente primero. */
    fun observeChats(uid: String): Flow<List<Chat>> =
        chats.whereArrayContains("members", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshotFlow()
            .map { snap -> snap.documents.mapNotNull { it.toChat() } }

    fun observeChat(chatId: String): Flow<Chat?> =
        chats.document(chatId).snapshotFlow().map { it.toChat() }

    /**
     * Los últimos [limit] mensajes, del más nuevo al más viejo (para una lista invertida).
     * Incluye metadatos para saber qué mensajes siguen pendientes de subir.
     */
    fun observeMessages(chatId: String, limit: Long): Flow<List<Message>> =
        messages(chatId)
            .orderBy("sentAt", Query.Direction.DESCENDING)
            .limit(limit)
            .snapshotFlow(includeMetadata = true)
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    doc.toObject(Message::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?.apply { pending = doc.metadata.hasPendingWrites() }
                }
            }

    /** Devuelve el id del chat 1 a 1 entre dos usuarios, creándolo si todavía no existe. */
    suspend fun getOrCreateDirectChat(myUid: String, otherUid: String): String {
        val id = ChatIds.direct(myUid, otherUid)
        val ref = chats.document(id)
        db.runTransaction { tx ->
            if (!tx.get(ref).exists()) {
                tx.set(
                    ref,
                    mapOf(
                        "type" to Chat.TYPE_DIRECT,
                        "members" to listOf(myUid, otherUid).sorted(),
                        "createdBy" to myUid,
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "unread" to mapOf(myUid to 0, otherUid to 0),
                    ),
                )
            }
        }.await()
        return id
    }

    suspend fun createGroup(myUid: String, name: String, memberIds: List<String>): String {
        val ref = chats.document()
        val members = (memberIds + myUid).distinct()
        ref.set(
            mapOf(
                "type" to Chat.TYPE_GROUP,
                "name" to name.trim(),
                "members" to members,
                "createdBy" to myUid,
                "updatedAt" to FieldValue.serverTimestamp(),
                "unread" to members.associateWith { 0 },
            ),
        ).await()
        return ref.id
    }

    /**
     * Guarda el mensaje y actualiza el resumen del chat en una sola escritura atómica:
     * último mensaje, fecha y +1 en el contador de no leídos de los demás miembros.
     * Sin conexión, el mensaje aparece al instante como "pendiente" y se envía al volver la red.
     * Solo texto: el plan gratis de Firebase no tiene Storage para fotos.
     */
    suspend fun sendMessage(chat: Chat, senderId: String, text: String) {
        val messageRef = messages(chat.id).document()
        val cleanText = text.trim()

        val chatUpdate = mutableMapOf<String, Any>(
            "lastMessage" to mapOf(
                "text" to cleanText,
                "senderId" to senderId,
                "hasImage" to false,
                "sentAt" to FieldValue.serverTimestamp(),
            ),
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastRead.$senderId" to FieldValue.serverTimestamp(),
        )
        chat.members.filter { it != senderId }.forEach { chatUpdate["unread.$it"] = FieldValue.increment(1) }

        db.batch()
            .set(
                messageRef,
                mapOf(
                    "senderId" to senderId,
                    "text" to cleanText,
                    "sentAt" to FieldValue.serverTimestamp(),
                ),
            )
            .update(chats.document(chat.id), chatUpdate)
            .commit()
            .await()
        // El Worker envía la notificación (el chat general del edificio no notifica).
        if (chat.type != Chat.TYPE_COMMUNITY) {
            runCatching { api.notify("message", "chatId" to chat.id, "messageId" to messageRef.id) }
        }
    }

    /** Pone en cero los no leídos del usuario y guarda cuándo leyó (para los checks azules). */
    fun markRead(chatId: String, uid: String) {
        chats.document(chatId).update(
            mapOf("unread.$uid" to 0, "lastRead.$uid" to FieldValue.serverTimestamp()),
        )
    }

    private fun DocumentSnapshot.toChat(): Chat? =
        toObject(Chat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

    companion object {
        const val CHATS = "chats"
        const val MESSAGES = "messages"
    }
}
