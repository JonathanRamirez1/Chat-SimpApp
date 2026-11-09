package com.jonathan.chatsimpapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

/** Documento `users/{uid}`. Los tokens FCM viven aparte, en `users/{uid}/private/fcm`. */
@IgnoreExtraProperties
data class User(
    @DocumentId var uid: String = "",
    var displayName: String = "",
    var displayNameLower: String = "",
    var email: String = "",
    var photoUrl: String? = null,
    var about: String = "",
    @ServerTimestamp var createdAt: Timestamp? = null,
)
