package com.jonathan.chatsimpapp.ui.sector

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.jonathan.chatsimpapp.data.model.AlertGroup
import androidx.compose.runtime.key
import com.jonathan.chatsimpapp.data.model.Alert
import com.jonathan.chatsimpapp.data.model.AlertType
import com.jonathan.chatsimpapp.util.LatLngPoint

/**
 * Estilo simple: sin negocios, restaurantes ni transporte, para que solo se vean calles,
 * parques y nuestros pines. Menos cosas en pantalla = menos confusión.
 */
private const val SIMPLE_STYLE = """
[
  {"featureType":"poi","stylers":[{"visibility":"off"}]},
  {"featureType":"poi.park","stylers":[{"visibility":"on"}]},
  {"featureType":"poi.park","elementType":"labels","stylers":[{"visibility":"off"}]},
  {"featureType":"transit","stylers":[{"visibility":"off"}]},
  {"featureType":"road","elementType":"labels.icon","stylers":[{"visibility":"off"}]}
]
"""

/** No se puede alejar más de ~5 km de ancho: lo que se ve sigue siendo "el barrio". */
const val MIN_ZOOM = 13.5f
const val DEFAULT_ZOOM = 15.5f

fun CameraPositionState.centerOn(point: LatLngPoint, zoom: Float = DEFAULT_ZOOM) {
    position = CameraPosition.fromLatLngZoom(LatLng(point.lat, point.lng), zoom)
}

suspend fun CameraPositionState.animateTo(point: LatLngPoint, zoom: Float = DEFAULT_ZOOM) {
    animate(CameraUpdateFactory.newLatLngZoom(LatLng(point.lat, point.lng), zoom), 600)
}

/** Mapa real de Google con la zona, el edificio y las alertas. */
@Composable
fun SectorGoogleMap(
    cameraState: CameraPositionState,
    home: LatLngPoint?,
    radiusMeters: Double,
    alerts: List<Alert>,
    onAlertClick: (Alert) -> Unit,
    modifier: Modifier = Modifier,
    showMyLocation: Boolean = false,
) {
    val style = remember { MapStyleOptions(SIMPLE_STYLE) }
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraState,
        properties = MapProperties(mapStyleOptions = style, minZoomPreference = MIN_ZOOM, isMyLocationEnabled = showMyLocation),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = false,
            tiltGesturesEnabled = false,
            compassEnabled = false,
            indoorLevelPickerEnabled = false,
        ),
    ) {
        home?.let {
            val homeLatLng = LatLng(it.lat, it.lng)
            Circle(
                center = homeLatLng,
                radius = radiusMeters,
                fillColor = androidx.compose.ui.graphics.Color(0x144F46E5),
                strokeColor = androidx.compose.ui.graphics.Color(0x664F46E5),
                strokeWidth = 3f,
            )
            MarkerComposable(state = rememberUpdatedMarkerState(position = homeLatLng), zIndex = 0f) { HomePin() }
        }
        alerts.forEach { alert ->
            key(alert.id) {
                val type = AlertType.of(alert.type)
                MarkerComposable(
                    alert.id, alert.type,
                    state = rememberUpdatedMarkerState(position = LatLng(alert.lat, alert.lng)),
                    zIndex = if (type.group == AlertGroup.SECURITY) 2f else 1f,
                    onClick = { onAlertClick(alert); true },
                ) { AlertPin(type) }
            }
        }
    }
}

/** Para la pantalla "¿Fue aquí?": el mapa se mueve y el pin queda fijo en el centro. */
@Composable
fun PickLocationMap(cameraState: CameraPositionState, modifier: Modifier = Modifier) {
    val style = remember { MapStyleOptions(SIMPLE_STYLE) }
    Box(modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(mapStyleOptions = style, minZoomPreference = MIN_ZOOM),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                rotationGesturesEnabled = false,
                tiltGesturesEnabled = false,
            ),
        )
    }
}

