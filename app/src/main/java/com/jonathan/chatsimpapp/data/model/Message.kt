package com.jonathan.chatsimpapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/** Documento `chats/{chatId}/messages/{messageId}`. */
@IgnoreExtraProperties
data class Message(
    @DocumentId var id: String = "",
    var senderId: String = "",
    var text: String = "",
    var imageUrl: String? = null,
    @ServerTimestamp var sentAt: Timestamp? = null,
    /** true mientras el mensaje solo existe en el teléfono (sin confirmar por el servidor). */
    @get:Exclude @set:Exclude var pending: Boolean = false,
)
