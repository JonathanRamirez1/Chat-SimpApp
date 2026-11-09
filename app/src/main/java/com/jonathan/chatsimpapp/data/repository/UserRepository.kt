package com.jonathan.chatsimpapp.data.repository

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import com.jonathan.chatsimpapp.data.model.User
import com.jonathan.chatsimpapp.util.ImageCompressor
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
    private val storage: FirebaseStorage,
    private val messaging: FirebaseMessaging,
    private val context: Context,
) {
    private val users get() = db.collection(USERS)

    /** Los tokens de notificación van en un documento privado que solo el dueño puede leer. */
    private fun tokensDoc(uid: String) = users.document(uid).collection("private").document("fcm")

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

    /**
     * Crea el perfil `users/{uid}` si no existe. Si el usuario venía de la versión anterior
     * de la app (colección `UsersRegister`), reutiliza su nombre y foto.
     */
    suspend fun ensureProfile(firebaseUser: FirebaseUser, nameOverride: String? = null) {
        val ref = users.document(firebaseUser.uid)
        if (ref.get().await().exists()) return

        val legacy = runCatching { db.collection(LEGACY_USERS).document(firebaseUser.uid).get().await() }.getOrNull()
        val legacyName = legacy?.getString("username")?.takeIf { it.isNotBlank() }
        val legacyPhoto = legacy?.getString("photo")?.takeIf { it.isNotBlank() }

        val email = firebaseUser.email.orEmpty().lowercase()
        val name = nameOverride
            ?: firebaseUser.displayName?.takeIf { it.isNotBlank() }
            ?: legacyName
            ?: email.substringBefore('@')
        val data = mapOf(
            "displayName" to name,
            "displayNameLower" to name.lowercase(),
            "email" to email,
            "photoUrl" to (firebaseUser.photoUrl?.toString() ?: legacyPhoto),
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

    suspend fun updatePhoto(uid: String, uri: Uri): String {
        val bytes = ImageCompressor.compress(context, uri, maxSize = 640)
        val ref = storage.reference.child("users/$uid/avatar.jpg")
        ref.putBytes(bytes, storageMetadata { contentType = "image/jpeg" }).await()
        val url = ref.downloadUrl.await().toString()
        users.document(uid).update("photoUrl", url).await()
        auth.currentUser?.updateProfile(userProfileChangeRequest { photoUri = Uri.parse(url) })?.await()
        return url
    }

    suspend fun registerCurrentFcmToken(uid: String) {
        @Suppress("DEPRECATION") // Sigue siendo la API documentada para obtener el token FCM.
        val token = messaging.token.await()
        addFcmToken(uid, token)
    }

    suspend fun addFcmToken(uid: String, token: String) {
        tokensDoc(uid).set(mapOf("tokens" to FieldValue.arrayUnion(token)), SetOptions.merge()).await()
    }

    suspend fun removeCurrentFcmToken(uid: String) {
        @Suppress("DEPRECATION") // Sigue siendo la API documentada para obtener el token FCM.
        val token = messaging.token.await()
        tokensDoc(uid).update("tokens", FieldValue.arrayRemove(token)).await()
    }

    companion object {
        const val USERS = "users"
        private const val LEGACY_USERS = "UsersRegister"
    }
}
