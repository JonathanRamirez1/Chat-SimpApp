package com.jonathan.chatsimpapp.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.jonathan.chatsimpapp.R

/** Resultado de pedir la cuenta de Google con Credential Manager (el reemplazo de GoogleSignInClient). */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Failure(val error: Throwable) : GoogleSignInResult
}

suspend fun requestGoogleIdToken(activityContext: Context): GoogleSignInResult = try {
    val option = GetSignInWithGoogleOption.Builder(
        serverClientId = activityContext.getString(R.string.default_web_client_id),
    ).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        GoogleSignInResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
    } else {
        GoogleSignInResult.Failure(IllegalStateException("Unexpected credential type: ${credential.type}"))
    }
} catch (e: GetCredentialCancellationException) {
    GoogleSignInResult.Cancelled
} catch (e: Exception) {
    GoogleSignInResult.Failure(e)
}
