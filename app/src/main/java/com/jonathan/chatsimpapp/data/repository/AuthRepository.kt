package com.jonathan.chatsimpapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val users: UserRepository,
    private val presence: PresenceRepository,
) {
    val currentUserId: String? get() = auth.currentUser?.uid

    /** Emite el uid del usuario actual (o null) cada vez que cambia la sesión. */
    val authState: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    suspend fun signIn(email: String, password: String) {
        val user = auth.signInWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Sign in returned no user")
        users.ensureProfile(user)
        afterSignIn()
    }

    suspend fun register(name: String, email: String, password: String) {
        val user = auth.createUserWithEmailAndPassword(email.trim(), password).await().user
            ?: error("Register returned no user")
        user.updateProfile(userProfileChangeRequest { displayName = name.trim() }).await()
        users.ensureProfile(user, nameOverride = name.trim())
        runCatching { user.sendEmailVerification().await() }
        afterSignIn()
    }

    suspend fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user: FirebaseUser = auth.signInWithCredential(credential).await().user
            ?: error("Google sign in returned no user")
        users.ensureProfile(user)
        afterSignIn()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    suspend fun signOut() {
        val uid = currentUserId
        if (uid != null) {
            runCatching { users.removeCurrentFcmToken(uid) }
            presence.goOffline(uid)
        }
        auth.signOut()
    }

    private suspend fun afterSignIn() {
        val uid = currentUserId ?: return
        runCatching { users.registerCurrentFcmToken(uid) }
        presence.goOnline(uid)
    }
}
