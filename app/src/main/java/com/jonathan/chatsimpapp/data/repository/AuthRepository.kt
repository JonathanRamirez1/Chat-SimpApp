package com.jonathan.chatsimpapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.jonathan.chatsimpapp.data.remote.WorkerApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sesión. Solo "Continuar con Google": es un único botón (más fácil para personas mayores) y
 * cumple la regla de RJ2 de que para comprar Pro hay que tener sesión con Google.
 * No hay login anónimo: la app necesita saber quién es cada vecino y su unidad.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val users: UserRepository,
    private val presence: PresenceRepository,
    private val api: WorkerApi,
) {
    val currentUserId: String? get() = auth.currentUser?.uid

    /** Emite el uid del usuario actual (o null) cada vez que cambia la sesión. */
    val authState: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user: FirebaseUser = auth.signInWithCredential(credential).await().user
            ?: error("Google sign in returned no user")
        users.ensureProfile(user)
        runCatching { users.registerCurrentFcmToken() }
        presence.ping()
    }

    /** Elimina la cuenta (exigido por Google Play). El servidor borra los datos y el usuario de Auth. */
    suspend fun deleteAccount() {
        api.deleteAccount()
        auth.signOut()
    }

    suspend fun signOut() {
        if (currentUserId != null) runCatching { users.removeCurrentFcmToken() }
        auth.signOut()
    }
}
