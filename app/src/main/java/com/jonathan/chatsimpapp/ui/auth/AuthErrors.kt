package com.jonathan.chatsimpapp.ui.auth

import androidx.annotation.StringRes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.jonathan.chatsimpapp.R

/** Traduce los errores de Firebase a mensajes entendibles para el usuario. */
@StringRes
fun Throwable.toAuthMessage(): Int = when (this) {
    is FirebaseAuthWeakPasswordException -> R.string.error_weak_password
    is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> R.string.error_invalid_credentials
    is FirebaseAuthUserCollisionException -> R.string.error_email_in_use
    is FirebaseNetworkException -> R.string.error_network
    is FirebaseTooManyRequestsException -> R.string.error_too_many_requests
    else -> R.string.error_generic
}
