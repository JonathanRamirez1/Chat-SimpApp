package com.jonathan.chatsimpapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Alerta del barrio (`alerts/{id}`): algo que pasa en la calle y que ven los vecinos de varios
 * edificios cercanos. Se reportan hechos, nunca personas.
 */
@IgnoreExtraProperties
data class Alert(
    @DocumentId var id: String = "",
    var type: String = "other",
    var description: String = "",
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var geohash: String = "",
    var communityId: String = "",
    var communityName: String = "",
    var createdBy: String = "",
    var createdAt: Timestamp? = null,
    var expiresAt: Timestamp? = null,
    /** Vecinos que confirmaron "yo también lo vi". */
    var confirmedBy: List<String> = emptyList(),
    /** Vecinos que dicen "ya no está" o que es falso. */
    var goneBy: List<String> = emptyList(),
) {
    @get:Exclude
    val group: AlertGroup get() = AlertType.of(type).group

    /** Se oculta cuando varios vecinos dicen que ya no está (y más que los que lo confirmaron). */
    @get:Exclude
    val hidden: Boolean get() = goneBy.size >= GONE_TO_HIDE && goneBy.size > confirmedBy.size

    @Exclude
    fun isActive(now: Long = System.currentTimeMillis()): Boolean =
        !hidden && (expiresAt?.toDate()?.time ?: Long.MAX_VALUE) > now

    companion object {
        const val GONE_TO_HIDE = 2
    }
}

enum class AlertGroup { SECURITY, DAMAGE, OTHER }

/** Tipos de alerta y cuánto duran en el mapa. */
enum class AlertType(val key: String, val group: AlertGroup, val hoursVisible: Long) {
    THEFT("theft", AlertGroup.SECURITY, 12),
    STREETLIGHT("streetlight", AlertGroup.DAMAGE, 24 * 30),
    ROAD("road", AlertGroup.DAMAGE, 24 * 30),
    WATER("water", AlertGroup.DAMAGE, 24 * 7),
    TRASH("trash", AlertGroup.DAMAGE, 24 * 7),
    NOISE("noise", AlertGroup.OTHER, 6),
    ;

    companion object {
        fun of(key: String): AlertType = entries.firstOrNull { it.key == key } ?: ROAD
    }
}
