package com.jonathan.chatsimpapp.data.model

/** Estado de conexión guardado en Realtime Database: `status/{uid}`. */
data class Presence(
    val online: Boolean = false,
    /** Milisegundos desde epoch de la última conexión/desconexión. */
    val lastChanged: Long? = null,
)
