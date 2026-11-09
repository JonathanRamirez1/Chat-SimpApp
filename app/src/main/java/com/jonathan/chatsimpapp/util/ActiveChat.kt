package com.jonathan.chatsimpapp.util

/** Chat que el usuario tiene abierto en pantalla, para no notificarle mensajes que ya está viendo. */
object ActiveChat {
    @Volatile
    var chatId: String? = null
}
