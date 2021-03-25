package com.jonathan.loginfuturo

/** Clase SingleLiveEvent como solución al no poder usar toas en el ViewModel **/

open class Event<out T>(private val content: T) {

    private var hasBeenHandled = false
        private set // Permitir lectura externa pero no escritura

    /** Devuelve el contenido y evita su uso nuevamente. **/

    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /** Devuelve el contenido, incluso si ya se ha manejado. **/

    fun peekContent(): T = content
}