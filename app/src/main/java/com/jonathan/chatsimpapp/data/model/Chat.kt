package com.jonathan.chatsimpapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Documento `chats/{chatId}`.
 *
 * - Chats directos: el id es [ChatIds.direct] (los dos uid ordenados), así nunca hay duplicados.
 * - `unread[uid]` es el contador de no leídos de cada miembro.
 * - `lastRead[uid]` es la última vez que cada miembro abrió el chat (para los checks de "leído").
 */
@IgnoreExtraProperties
data class Chat(
    @DocumentId var id: String = "",
    var type: String = TYPE_DIRECT,
    var members: List<String> = emptyList(),
    var name: String? = null,
    var photoUrl: String? = null,
    var createdBy: String = "",
    var lastMessage: LastMessage? = null,
    var updatedAt: Timestamp? = null,
    var unread: Map<String, Long> = emptyMap(),
    var lastRead: Map<String, Timestamp> = emptyMap(),
) {
    @get:Exclude
    val isGroup: Boolean get() = type == TYPE_GROUP || type == TYPE_COMMUNITY

    fun otherMember(myUid: String): String? = members.firstOrNull { it != myUid }

    companion object {
        const val TYPE_DIRECT = "direct"
        const val TYPE_COMMUNITY = "community"
        const val TYPE_GROUP = "group"
    }
}

@IgnoreExtraProperties
data class LastMessage(
    var text: String = "",
    var senderId: String = "",
    var hasImage: Boolean = false,
    var sentAt: Timestamp? = null,
)
