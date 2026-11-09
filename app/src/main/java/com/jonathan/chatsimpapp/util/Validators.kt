package com.jonathan.chatsimpapp.util

object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = emailRegex.matches(email.trim())

    fun isValidPassword(password: String): Boolean = password.length >= 6

    fun isValidName(name: String): Boolean = name.trim().isNotEmpty()
}
