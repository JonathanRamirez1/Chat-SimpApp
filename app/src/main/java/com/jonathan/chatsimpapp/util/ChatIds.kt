package com.jonathan.chatsimpapp.util

object ChatIds {
    /** Id determinista para un chat 1 a 1: el mismo sin importar quién lo inicie. */
    fun direct(a: String, b: String): String = listOf(a, b).sorted().joinToString("_")
}
