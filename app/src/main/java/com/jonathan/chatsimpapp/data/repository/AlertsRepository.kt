package com.jonathan.chatsimpapp.data.repository

import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jonathan.chatsimpapp.data.model.Alert
import com.jonathan.chatsimpapp.data.model.AlertType
import com.jonathan.chatsimpapp.util.Distance
import com.jonathan.chatsimpapp.util.LatLngPoint
import com.jonathan.chatsimpapp.util.snapshotFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alertas del barrio con búsqueda por cercanía (geohash): el mundo se divide en cuadritos con
 * nombre y solo se piden los cuadritos que tocan la zona; luego se descarta lo que queda fuera
 * del radio exacto. No hace falta servidor y cuesta pocas lecturas.
 */
@Singleton
class AlertsRepository @Inject constructor(
    private val db: FirebaseFirestore,
) {
    private val alerts get() = db.collection(ALERTS)

    fun observeNearby(center: LatLngPoint, radiusMeters: Double): Flow<List<Alert>> {
        val bounds = GeoFireUtils.getGeoHashQueryBounds(GeoLocation(center.lat, center.lng), radiusMeters)
        val flows = bounds.map { b ->
            alerts.orderBy("geohash").startAt(b.startHash).endAt(b.endHash).limit(PER_CELL_LIMIT)
                .snapshotFlow()
                .map { it.toObjects(Alert::class.java) }
                .catch { emit(emptyList()) }
        }
        return combine(flows) { lists ->
            val now = System.currentTimeMillis()
            lists.flatMap { it }
                .distinctBy { it.id }
                .filter { it.isActive(now) && Distance.meters(center, LatLngPoint(it.lat, it.lng)) <= radiusMeters }
                .sortedByDescending { it.createdAt }
        }
    }

    suspend fun create(
        type: AlertType,
        description: String,
        point: LatLngPoint,
        communityId: String,
        communityName: String,
        uid: String,
    ) {
        // ~11 m de precisión: suficiente para ubicar el hecho sin señalar una casa exacta.
        val lat = Math.round(point.lat * 10_000) / 10_000.0
        val lng = Math.round(point.lng * 10_000) / 10_000.0
        val expires = Timestamp(Date(System.currentTimeMillis() + type.hoursVisible * 3_600_000))
        alerts.add(
            mapOf(
                "type" to type.key,
                "description" to description.trim().take(280),
                "lat" to lat,
                "lng" to lng,
                "geohash" to GeoFireUtils.getGeoHashForLocation(GeoLocation(lat, lng)),
                "communityId" to communityId,
                "communityName" to communityName,
                "createdBy" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to expires,
                "confirmedBy" to emptyList<String>(),
                "goneBy" to emptyList<String>(),
            ),
        ).await()
    }

    suspend fun confirm(alertId: String, uid: String, confirmed: Boolean) {
        alerts.document(alertId)
            .update("confirmedBy", if (confirmed) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)).await()
    }

    suspend fun markGone(alertId: String, uid: String) {
        alerts.document(alertId).update("goneBy", FieldValue.arrayUnion(uid)).await()
    }

    suspend fun delete(alertId: String) {
        alerts.document(alertId).delete().await()
    }

    /** Solo administración: marca dónde está el edificio. */
    suspend fun setCommunityLocation(communityId: String, point: LatLngPoint) {
        db.collection(CommunityRepository.COMMUNITIES).document(communityId).update(
            mapOf(
                "lat" to point.lat,
                "lng" to point.lng,
                "geohash" to GeoFireUtils.getGeoHashForLocation(GeoLocation(point.lat, point.lng)),
            ),
        ).await()
    }

    companion object {
        const val ALERTS = "alerts"
        /** Radio del "barrio": lo que se ve y lo que notifica. */
        const val SECTOR_RADIUS_M = 2_000.0
        private const val PER_CELL_LIMIT = 100L
    }
}
