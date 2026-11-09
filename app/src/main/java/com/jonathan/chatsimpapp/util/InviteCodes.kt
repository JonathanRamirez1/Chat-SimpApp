package com.jonathan.chatsimpapp.util

import java.security.SecureRandom

object InviteCodes {
    // Sin 0/O ni 1/I/L para que se pueda dictar sin confusiones.
    private const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    private val random = SecureRandom()

    fun generate(length: Int = 6): String = buildString { repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    fun normalize(code: String): String = code.trim().uppercase().replace(" ", "").replace("-", "")

    fun isValid(code: String): Boolean = normalize(code).let { c -> c.length == 6 && c.all { it in ALPHABET } }
}
