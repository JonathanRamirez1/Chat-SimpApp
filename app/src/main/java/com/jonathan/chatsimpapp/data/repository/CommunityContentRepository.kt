package com.jonathan.chatsimpapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jonathan.chatsimpapp.data.model.Announcement
import com.jonathan.chatsimpapp.data.model.Poll
import com.jonathan.chatsimpapp.data.model.Post
import com.jonathan.chatsimpapp.data.model.Report
import com.jonathan.chatsimpapp.data.model.ReportEvent
import com.jonathan.chatsimpapp.data.model.Vote
import com.jonathan.chatsimpapp.data.model.WeeklySummary
import com.jonathan.chatsimpapp.data.remote.WorkerApi
import com.jonathan.chatsimpapp.util.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Contenido de una comunidad: avisos, votaciones, reportes, tablón y resúmenes.
 * Todas las listas tienen límite para que el costo de lecturas en Firestore sea predecible.
 */
@Singleton
class CommunityContentRepository @Inject constructor(
    private val db: FirebaseFirestore,
    private val api: WorkerApi,
) {
    /** Las notificaciones las envía el Worker; si falla, lo publicado igual queda guardado. */
    private suspend fun notifyQuietly(kind: String, vararg ids: Pair<String, String>) {
        runCatching { api.notify(kind, *ids) }
    }

    private fun col(cid: String, name: String) =
        db.collection(CommunityRepository.COMMUNITIES).document(cid).collection(name)

    // ---------- Avisos ----------

    fun observeAnnouncements(cid: String, limit: Long = 30): Flow<List<Announcement>> =
        col(cid, ANNOUNCEMENTS).orderBy("createdAt", Query.Direction.DESCENDING).limit(limit)
            .snapshotFlow().map { it.toObjects(Announcement::class.java) }

    suspend fun postAnnouncement(cid: String, uid: String, unit: String, title: String, body: String, pinned: Boolean) {
        val ref = col(cid, ANNOUNCEMENTS).add(
            mapOf(
                "title" to title.trim(), "body" to body.trim(), "type" to Announcement.TYPE_NOTICE,
                "pinned" to pinned, "authorId" to uid, "authorUnit" to unit,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        notifyQuietly("announcement", "communityId" to cid, "announcementId" to ref.id)
    }

    suspend fun raiseEmergency(cid: String, uid: String, unit: String, kind: String, details: String) {
        val ref = col(cid, ANNOUNCEMENTS).add(
            mapOf(
                "title" to kind, "body" to details.trim(), "type" to Announcement.TYPE_EMERGENCY,
                "emergencyKind" to kind, "pinned" to false, "authorId" to uid, "authorUnit" to unit,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        notifyQuietly("announcement", "communityId" to cid, "announcementId" to ref.id)
    }

    suspend fun deleteAnnouncement(cid: String, id: String) {
        col(cid, ANNOUNCEMENTS).document(id).delete().await()
    }

    /** Marca el aviso como visto (una escritura por persona, solo la primera vez). */
    fun markAnnouncementSeen(cid: String, id: String, uid: String, unit: String) {
        col(cid, ANNOUNCEMENTS).document(id).collection(READS).document(uid)
            .set(mapOf("unit" to unit, "at" to FieldValue.serverTimestamp()))
    }

    /** Cuántas personas vieron el aviso (consulta de conteo: cuesta 1 lectura por cada 1000). */
    suspend fun seenCount(cid: String, id: String): Long =
        col(cid, ANNOUNCEMENTS).document(id).collection(READS).count().get(AggregateSource.SERVER).await().count

    // ---------- Votaciones ----------

    fun observePolls(cid: String, limit: Long = 30): Flow<List<Poll>> =
        col(cid, POLLS).orderBy("createdAt", Query.Direction.DESCENDING).limit(limit)
            .snapshotFlow().map { it.toObjects(Poll::class.java) }

    fun observePoll(cid: String, pid: String): Flow<Poll?> =
        col(cid, POLLS).document(pid).snapshotFlow().map { it.toObject(Poll::class.java) }

    fun observeVotes(cid: String, pid: String): Flow<List<Vote>> =
        col(cid, POLLS).document(pid).collection(VOTES).snapshotFlow().map { it.toObjects(Vote::class.java) }

    suspend fun createPoll(
        cid: String, uid: String, question: String, description: String,
        options: List<String>, quorumPercent: Int, closesAt: Date,
    ): String {
        val ref = col(cid, POLLS).document()
        ref.set(
            mapOf(
                "question" to question.trim(), "description" to description.trim(),
                "options" to options.map { it.trim() }.filter { it.isNotEmpty() },
                "quorumPercent" to quorumPercent, "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(), "closesAt" to Timestamp(closesAt), "closed" to false,
            ),
        ).await()
        notifyQuietly("poll", "communityId" to cid, "pollId" to ref.id)
        return ref.id
    }

    /** Un voto por unidad: el documento del voto se llama como la unidad y no se puede cambiar. */
    suspend fun vote(cid: String, pid: String, uid: String, unit: String, option: Int) {
        col(cid, POLLS).document(pid).collection(VOTES).document(unit)
            .set(mapOf("option" to option, "uid" to uid, "votedAt" to FieldValue.serverTimestamp())).await()
    }

    suspend fun closePoll(cid: String, pid: String) {
        col(cid, POLLS).document(pid).update("closed", true).await()
    }

    // ---------- Reportes ----------

    fun observeReports(cid: String, limit: Long = 60): Flow<List<Report>> =
        col(cid, REPORTS).orderBy("updatedAt", Query.Direction.DESCENDING).limit(limit)
            .snapshotFlow().map { it.toObjects(Report::class.java) }

    fun observeReport(cid: String, rid: String): Flow<Report?> =
        col(cid, REPORTS).document(rid).snapshotFlow().map { it.toObject(Report::class.java) }

    suspend fun createReport(
        cid: String, uid: String, unit: String, title: String, description: String, category: String,
    ): String {
        val ref = col(cid, REPORTS).document()
        val now = Timestamp.now()
        ref.set(
            mapOf(
                "title" to title.trim(), "description" to description.trim(), "category" to category,
                "status" to Report.STATUS_OPEN, "createdBy" to uid, "unit" to unit,
                "affectedUnits" to listOf(unit),
                "history" to listOf(ReportEvent(Report.STATUS_OPEN, "", unit, now)),
                "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        return ref.id
    }

    /** "A mí también me afecta": suma la unidad, lo que ayuda a priorizar. */
    suspend fun toggleAffected(cid: String, rid: String, unit: String, affected: Boolean) {
        col(cid, REPORTS).document(rid)
            .update("affectedUnits", if (affected) FieldValue.arrayUnion(unit) else FieldValue.arrayRemove(unit)).await()
    }

    /** Solo administración: cambia el estado y deja constancia en el historial. */
    suspend fun updateReportStatus(cid: String, rid: String, status: String, note: String, byUnit: String) {
        col(cid, REPORTS).document(rid).update(
            mapOf(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp(),
                "history" to FieldValue.arrayUnion(ReportEvent(status, note.trim(), byUnit, Timestamp.now())),
            ),
        ).await()
        // Avisa solo a las unidades afectadas.
        notifyQuietly("report", "communityId" to cid, "reportId" to rid)
    }

    // ---------- Tablón ----------

    fun observePosts(cid: String, limit: Long = 50): Flow<List<Post>> =
        col(cid, POSTS).orderBy("createdAt", Query.Direction.DESCENDING).limit(limit)
            .snapshotFlow().map { it.toObjects(Post::class.java) }

    suspend fun createPost(cid: String, uid: String, unit: String, type: String, text: String) {
        val ref = col(cid, POSTS).document()
        ref.set(
            mapOf(
                "type" to type, "text" to text.trim(), "authorId" to uid,
                "unit" to unit, "resolved" to false, "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    suspend fun setPostResolved(cid: String, id: String, resolved: Boolean) {
        col(cid, POSTS).document(id).update("resolved", resolved).await()
    }

    suspend fun deletePost(cid: String, id: String) {
        col(cid, POSTS).document(id).delete().await()
    }

    // ---------- Resumen semanal ----------

    fun observeSummary(cid: String, week: String): Flow<WeeklySummary?> =
        col(cid, SUMMARIES).document(week).snapshotFlow().map { it.toObject(WeeklySummary::class.java) }

    suspend fun saveSummary(cid: String, summary: WeeklySummary) {
        col(cid, SUMMARIES).document(summary.week).set(
            mapOf(
                "overview" to summary.overview, "decisions" to summary.decisions,
                "commitments" to summary.commitments, "notices" to summary.notices,
                "generatedAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    companion object {
        const val ANNOUNCEMENTS = "announcements"
        const val READS = "reads"
        const val POLLS = "polls"
        const val VOTES = "votes"
        const val REPORTS = "reports"
        const val POSTS = "posts"
        const val SUMMARIES = "summaries"
    }
}
