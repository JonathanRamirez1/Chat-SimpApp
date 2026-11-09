package com.jonathan.chatsimpapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * `communities/{id}`: un edificio o conjunto residencial.
 * `plan` y `planExpiresAt` solo los escribe el servidor (Cloud Function de pagos).
 */
@IgnoreExtraProperties
data class Community(
    @DocumentId var id: String = "",
    var name: String = "",
    var address: String = "",
    var unitsCount: Long = 0,
    var inviteCode: String = "",
    var createdBy: String = "",
    var admins: List<String> = emptyList(),
    var plan: String = PLAN_FREE,
    var planExpiresAt: Timestamp? = null,
    var createdAt: Timestamp? = null,
) {
    @get:Exclude
    val isPro: Boolean
        get() = plan == PLAN_PRO && (planExpiresAt?.toDate()?.time ?: 0L) > System.currentTimeMillis()

    fun isAdmin(uid: String?): Boolean = uid != null && uid in admins

    companion object {
        const val PLAN_FREE = "free"
        const val PLAN_PRO = "pro"
    }
}

/** `communities/{id}/members/{uid}`. La unidad (apartamento/casa) identifica a cada vecino. */
@IgnoreExtraProperties
data class Member(
    @DocumentId var uid: String = "",
    var unit: String = "",
    var role: String = ROLE_RESIDENT,
    var joinedAt: Timestamp? = null,
) {
    companion object {
        const val ROLE_ADMIN = "admin"
        const val ROLE_RESIDENT = "resident"
    }
}

/** Aviso del canal oficial. Las emergencias son avisos que puede lanzar cualquier vecino. */
@IgnoreExtraProperties
data class Announcement(
    @DocumentId var id: String = "",
    var title: String = "",
    var body: String = "",
    var type: String = TYPE_NOTICE,
    var emergencyKind: String? = null,
    var pinned: Boolean = false,
    var authorId: String = "",
    var authorUnit: String = "",
    var createdAt: Timestamp? = null,
) {
    @get:Exclude
    val isEmergency: Boolean get() = type == TYPE_EMERGENCY

    companion object {
        const val TYPE_NOTICE = "notice"
        const val TYPE_EMERGENCY = "emergency"
    }
}

/** Votación oficial: un voto por unidad (el id del voto es la unidad). */
@IgnoreExtraProperties
data class Poll(
    @DocumentId var id: String = "",
    var question: String = "",
    var description: String = "",
    var options: List<String> = emptyList(),
    var quorumPercent: Long = 50,
    var createdBy: String = "",
    var createdAt: Timestamp? = null,
    var closesAt: Timestamp? = null,
    var closed: Boolean = false,
) {
    @Exclude
    fun isOpen(now: Long = System.currentTimeMillis()): Boolean =
        !closed && (closesAt?.toDate()?.time ?: Long.MAX_VALUE) > now
}

@IgnoreExtraProperties
data class Vote(
    @DocumentId var unit: String = "",
    var option: Long = 0,
    var uid: String = "",
    var votedAt: Timestamp? = null,
)

/** Reporte de un daño o problema del edificio, con estado y seguimiento. */
@IgnoreExtraProperties
data class Report(
    @DocumentId var id: String = "",
    var title: String = "",
    var description: String = "",
    var category: String = "other",
    var photoUrl: String? = null,
    var status: String = STATUS_OPEN,
    var createdBy: String = "",
    var unit: String = "",
    var affectedUnits: List<String> = emptyList(),
    var history: List<ReportEvent> = emptyList(),
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
) {
    companion object {
        const val STATUS_OPEN = "open"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_RESOLVED = "resolved"
        val CATEGORIES = listOf("plumbing", "electric", "elevator", "security", "cleaning", "common_areas", "noise", "other")
    }
}

@IgnoreExtraProperties
data class ReportEvent(
    var status: String = "",
    var note: String = "",
    var byUnit: String = "",
    var at: Timestamp? = null,
)

/** Publicación del tablón: préstamos, recomendaciones, objetos perdidos, ventas. */
@IgnoreExtraProperties
data class Post(
    @DocumentId var id: String = "",
    var type: String = "other",
    var text: String = "",
    var photoUrl: String? = null,
    var authorId: String = "",
    var unit: String = "",
    var resolved: Boolean = false,
    var createdAt: Timestamp? = null,
) {
    companion object {
        val TYPES = listOf("lend", "borrow", "recommend", "lost", "sell", "other")
    }
}

/** Resumen semanal hecho con IA, guardado para que se genere una sola vez por comunidad. */
@IgnoreExtraProperties
data class WeeklySummary(
    @DocumentId var week: String = "",
    var overview: String = "",
    var decisions: List<String> = emptyList(),
    var commitments: List<String> = emptyList(),
    var notices: List<String> = emptyList(),
    var generatedAt: Timestamp? = null,
)
