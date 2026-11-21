package com.jonathan.chatsimpapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.jonathan.chatsimpapp.data.model.User
import com.jonathan.chatsimpapp.data.remote.WorkerApi
import com.jonathan.chatsimpapp.util.snapshotFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val messaging: FirebaseMessaging,
    private val api: WorkerApi,
) {
    private val users get() = db.collection(USERS)

    fun observeUser(uid: String): Flow<User?> =
        users.document(uid).snapshotFlow().map { it.toObject(User::class.java) }

    /** Escucha varios usuarios a la vez (Firestore permite hasta 30 ids por consulta `in`). */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeUsers(ids: Set<String>): Flow<Map<String, User>> {
        if (ids.isEmpty()) return flowOf(emptyMap())
        val chunks = ids.sorted().chunked(30).map { chunk ->
            users.whereIn(FieldPath.documentId(), chunk).snapshotFlow()
                .map { snap -> snap.toObjects(User::class.java) }
        }
        return combine(chunks) { lists -> lists.flatMap { it }.associateBy { it.uid } }
    }

    /** Busca por prefijo de nombre, o por correo exacto si la consulta contiene "@". */
    suspend fun search(query: String, excludeUid: String?): List<User> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val snapshot = if ('@' in q) {
            users.whereEqualTo("email", q).limit(10).get().await()
        } else {
            users.orderBy("displayNameLower")
                .startAt(q).endAt(q + "")
                .limit(25).get().await()
        }
        return snapshot.toObjects(User::class.java).filter { it.uid != excludeUid }
    }

    /** Crea el perfil `users/{uid}` con el nombre y la foto de la cuenta de Google. */
    suspend fun ensureProfile(firebaseUser: FirebaseUser) {
        val ref = users.document(firebaseUser.uid)
        val photo = firebaseUser.photoUrl?.toString()
        if (ref.get().await().exists()) {
            // La foto de perfil siempre es la de Google (no se suben fotos: el plan gratis no tiene Storage).
            if (photo != null) ref.update("photoUrl", photo).await()
            return
        }
        val email = firebaseUser.email.orEmpty().lowercase()
        val name = firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')
        val data = mapOf(
            "displayName" to name,
            "displayNameLower" to name.lowercase(),
            "email" to email,
            "photoUrl" to photo,
            "about" to "",
            "createdAt" to FieldValue.serverTimestamp(),
        )
        ref.set(data).await()
    }

    suspend fun updateProfile(uid: String, name: String, about: String) {
        val clean = name.trim()
        users.document(uid).update(
            mapOf("displayName" to clean, "displayNameLower" to clean.lowercase(), "about" to about.trim())
        ).await()
        auth.currentUser?.updateProfile(userProfileChangeRequest { displayName = clean })?.await()
    }

    /** Guarda el token de notificaciones de este teléfono en el servidor (D1 de Cloudflare). */
    suspend fun registerCurrentFcmToken() {
        @Suppress("DEPRECATION") // Sigue siendo la API documentada para obtener el token FCM.
        val token = messaging.token.await()
        api.registerToken(token)
    }

    suspend fun registerToken(token: String) {
        api.registerToken(token)
    }

    suspend fun removeCurrentFcmToken() {
        @Suppress("DEPRECATION")
        val token = messaging.token.await()
        api.deleteToken(token)
    }

    companion object {
        const val USERS = "users"
    }
}
