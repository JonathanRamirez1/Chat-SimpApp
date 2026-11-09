package com.jonathan.chatsimpapp.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jonathan.chatsimpapp.data.model.Chat
import com.jonathan.chatsimpapp.data.model.Community
import com.jonathan.chatsimpapp.data.model.Member
import com.jonathan.chatsimpapp.util.InviteCodes
import com.jonathan.chatsimpapp.util.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

class InvalidInviteCodeException : Exception("Invalid invite code")

/**
 * Comunidades (edificios), sus miembros y códigos de invitación.
 *
 * Cada comunidad tiene un chat general con el mismo id (`chats/{communityId}`), y cada usuario
 * guarda la lista de sus comunidades en `users/{uid}.communities`.
 */
@Singleton
class CommunityRepository @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private val communities get() = db.collection(COMMUNITIES)
    private fun members(cid: String) = communities.document(cid).collection(MEMBERS)
    private fun invite(code: String) = db.collection(INVITES).document(code)

    fun observeMyCommunityIds(uid: String): Flow<List<String>> =
        db.collection(UserRepository.USERS).document(uid).snapshotFlow()
            .map { snap -> (snap.get("communities") as? List<*>)?.filterIsInstance<String>().orEmpty() }

    /** Una escucha por comunidad: las reglas de seguridad verifican membresía documento a documento. */
    fun observeCommunities(ids: List<String>): Flow<List<Community>> {
        if (ids.isEmpty()) return flowOf(emptyList())
        val flows = ids.distinct().map { id ->
            communities.document(id).snapshotFlow()
                .map { it.toObject(Community::class.java) }
                .catch { emit(null) } // si te sacaron de una comunidad, simplemente desaparece
        }
        return combine(flows) { list -> list.filterNotNull().sortedBy { it.name.lowercase() } }
    }

    fun observeCommunity(cid: String): Flow<Community?> =
        communities.document(cid).snapshotFlow().map { it.toObject(Community::class.java) }

    fun observeMember(cid: String, uid: String): Flow<Member?> =
        members(cid).document(uid).snapshotFlow().map { it.toObject(Member::class.java) }

    fun observeMembers(cid: String): Flow<List<Member>> =
        members(cid).snapshotFlow().map { snap -> snap.toObjects(Member::class.java).sortedWith(UnitOrder) }

    /** Crea la comunidad, su código, su chat general y deja al creador como administrador. */
    suspend fun create(uid: String, name: String, address: String, unitsCount: Int, myUnit: String): String {
        val ref = communities.document()
        val code = InviteCodes.generate()
        db.batch()
            .set(
                ref,
                mapOf(
                    "name" to name.trim(),
                    "address" to address.trim(),
                    "unitsCount" to unitsCount,
                    "inviteCode" to code,
                    "createdBy" to uid,
                    "admins" to listOf(uid),
                    "plan" to Community.PLAN_FREE,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
            .set(invite(code), mapOf("communityId" to ref.id))
            .set(members(ref.id).document(uid), memberData(uid, myUnit, Member.ROLE_ADMIN, code))
            .set(
                db.collection(ChatRepository.CHATS).document(ref.id),
                mapOf(
                    "type" to Chat.TYPE_COMMUNITY,
                    "name" to name.trim(),
                    "members" to listOf(uid),
                    "createdBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
            .set(userDoc(uid), mapOf("communities" to FieldValue.arrayUnion(ref.id)), SetOptions.merge())
            .commit().await()
        return ref.id
    }

    /** Se une a una comunidad con el código que comparte la administración. */
    suspend fun join(uid: String, code: String, myUnit: String): String {
        val clean = InviteCodes.normalize(code)
        val cid = invite(clean).get().await().getString("communityId") ?: throw InvalidInviteCodeException()
        db.batch()
            .set(members(cid).document(uid), memberData(uid, myUnit, Member.ROLE_RESIDENT, clean))
            .update(db.collection(ChatRepository.CHATS).document(cid), "members", FieldValue.arrayUnion(uid))
            .set(userDoc(uid), mapOf("communities" to FieldValue.arrayUnion(cid)), SetOptions.merge())
            .commit().await()
        return cid
    }

    suspend fun leave(cid: String, uid: String, wasAdmin: Boolean) {
        val batch = db.batch()
            .delete(members(cid).document(uid))
            .update(db.collection(ChatRepository.CHATS).document(cid), "members", FieldValue.arrayRemove(uid))
            .update(userDoc(uid), "communities", FieldValue.arrayRemove(cid))
        if (wasAdmin) batch.update(communities.document(cid), "admins", FieldValue.arrayRemove(uid))
        batch.commit().await()
    }

    /** Solo administración: saca a alguien (por ejemplo, alguien que ya no vive ahí). */
    suspend fun removeMember(cid: String, uid: String) {
        db.batch()
            .delete(members(cid).document(uid))
            .update(db.collection(ChatRepository.CHATS).document(cid), "members", FieldValue.arrayRemove(uid))
            .commit().await()
    }

    suspend fun setAdmin(cid: String, uid: String, admin: Boolean) {
        db.batch()
            .update(communities.document(cid), "admins", if (admin) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid))
            .update(members(cid).document(uid), "role", if (admin) Member.ROLE_ADMIN else Member.ROLE_RESIDENT)
            .commit().await()
    }

    suspend fun update(cid: String, name: String, address: String, unitsCount: Int) {
        communities.document(cid).update(
            mapOf("name" to name.trim(), "address" to address.trim(), "unitsCount" to unitsCount),
        ).await()
        db.collection(ChatRepository.CHATS).document(cid).update("name", name.trim()).await()
    }

    /** Cambia el código de invitación (por ejemplo, si se filtró). El anterior deja de servir. */
    suspend fun regenerateInviteCode(cid: String, oldCode: String): String {
        val code = InviteCodes.generate()
        db.batch()
            .set(invite(code), mapOf("communityId" to cid))
            .update(communities.document(cid), "inviteCode", code)
            .delete(invite(oldCode))
            .commit().await()
        return code
    }

    private fun userDoc(uid: String) = db.collection(UserRepository.USERS).document(uid)

    private fun memberData(uid: String, unit: String, role: String, code: String) = mapOf(
        "uid" to uid,
        "unit" to unit.trim().uppercase(),
        "role" to role,
        "code" to code,
        "joinedAt" to FieldValue.serverTimestamp(),
    )

    companion object {
        const val COMMUNITIES = "communities"
        const val MEMBERS = "members"
        const val INVITES = "invites"

        /** Ordena unidades de forma natural: 2 < 10 < 101 < A-1. */
        val UnitOrder = compareBy<Member>({ it.unit.toIntOrNull() ?: Int.MAX_VALUE }, { it.unit })
    }
}
