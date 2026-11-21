package com.jonathan.chatsimpapp.ui.sector

import android.graphics.Bitmap
import android.view.Gravity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jonathan.chatsimpapp.data.model.Alert
import com.jonathan.chatsimpapp.data.model.AlertGroup
import com.jonathan.chatsimpapp.data.model.AlertType
import com.jonathan.chatsimpapp.util.LatLngPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.cos
import kotlin.math.sin

/**
 * Mapa gratis y sin tarjeta: MapLibre con los mapas de OpenFreeMap (datos de OpenStreetMap).
 * No pide clave. El crédito "© OpenStreetMap" se muestra siempre (botón ⓘ), como exige su licencia.
 * Estilo "positron": claro y con pocos detalles, para que solo resalten calles, parques y los pines.
 */
const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/positron"

/** No se puede alejar más de ~5 km de ancho: lo que se ve sigue siendo "el barrio". */
const val MIN_ZOOM = 13.5
const val DEFAULT_ZOOM = 15.5

private const val ALERTS_SOURCE = "alerts"
private const val ALERTS_LAYER = "alerts-layer"
private const val HOME_SOURCE = "home"
private const val ZONE_SOURCE = "zone"
private const val HOME_ICON = "pin-home"

/** Para mover la cámara desde fuera del mapa ("Volver a mi casa", zoom, "¿Fue aquí?"). */
class MapController {
    internal var map: MapLibreMap? = null
    private var pending: Pair<LatLngPoint, Double>? = null

    internal fun attach(map: MapLibreMap) {
        this.map = map
        pending?.let { (p, z) -> map.cameraPosition = position(p, z) }
        pending = null
    }

    fun centerOn(point: LatLngPoint, zoom: Double = DEFAULT_ZOOM) {
        val m = map
        if (m == null) pending = point to zoom else m.cameraPosition = position(point, zoom)
    }

    fun animateTo(point: LatLngPoint, zoom: Double = DEFAULT_ZOOM) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(point.lat, point.lng), zoom), 600)
            ?: centerOn(point, zoom)
    }

    fun zoomIn() {
        map?.animateCamera(CameraUpdateFactory.zoomIn(), 250)
    }

    fun zoomOut() {
        map?.animateCamera(CameraUpdateFactory.zoomOut(), 250)
    }

    /** Centro actual del mapa (el pin fijo de "¿Fue aquí?"). */
    fun target(): LatLngPoint? = map?.cameraPosition?.target?.let { LatLngPoint(it.latitude, it.longitude) }

    private fun position(p: LatLngPoint, zoom: Double) =
        CameraPosition.Builder().target(LatLng(p.lat, p.lng)).zoom(zoom).build()
}

/** MapView de MapLibre dentro de Compose, con su ciclo de vida. */
@Composable
private fun MapLibreView(
    controller: MapController,
    modifier: Modifier = Modifier,
    onStyle: (MapLibreMap, Style) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onStyleState = rememberUpdatedState(onStyle)
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setMinZoomPreference(MIN_ZOOM)
                map.uiSettings.apply {
                    isRotateGesturesEnabled = false
                    isTiltGesturesEnabled = false
                    isCompassEnabled = false
                    isLogoEnabled = true
                    isAttributionEnabled = true
                    attributionGravity = Gravity.BOTTOM or Gravity.START
                }
                controller.attach(map)
                map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { style -> onStyleState.value(map, style) }
            }
        }
    }
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
            controller.map = null
        }
    }
    AndroidView(factory = { mapView }, modifier = modifier)
}

/** Mapa del barrio: zona de 2 km, el edificio y las alertas como pines grandes. */
@Composable
fun SectorMapLibre(
    controller: MapController,
    home: LatLngPoint?,
    radiusMeters: Double,
    alerts: List<Alert>,
    onAlertClick: (Alert) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pins = rememberPinBitmaps()
    val alertsState = rememberUpdatedState(alerts)
    val clickState = rememberUpdatedState(onAlertClick)
    val styleRef = remember { arrayOfNulls<Style>(1) }

    Box(modifier) {
        MapLibreView(controller, Modifier.fillMaxSize()) { map, style ->
            pins.forEach { (name, bitmap) -> style.addImage(name, bitmap) }
            style.addSource(GeoJsonSource(ZONE_SOURCE))
            style.addLayer(
                FillLayer("zone-fill", ZONE_SOURCE).withProperties(PropertyFactory.fillColor(Color(0xFF4F46E5).copy(alpha = 0.08f).toArgb())),
            )
            style.addLayer(
                LineLayer("zone-line", ZONE_SOURCE).withProperties(
                    PropertyFactory.lineColor(Color(0xFF4F46E5).copy(alpha = 0.45f).toArgb()),
                    PropertyFactory.lineWidth(2f),
                ),
            )
            style.addSource(GeoJsonSource(HOME_SOURCE))
            style.addLayer(
                SymbolLayer("home-layer", HOME_SOURCE).withProperties(
                    PropertyFactory.iconImage(HOME_ICON),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                ),
            )
            style.addSource(GeoJsonSource(ALERTS_SOURCE))
            style.addLayer(
                SymbolLayer(ALERTS_LAYER, ALERTS_SOURCE).withProperties(
                    PropertyFactory.iconImage(Expression.get("icon")),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.symbolSortKey(Expression.get("order")),
                ),
            )
            map.addOnMapClickListener { latLng ->
                val point = map.projection.toScreenLocation(latLng)
                val id = map.queryRenderedFeatures(point, ALERTS_LAYER).firstOrNull()?.getStringProperty("id")
                val alert = alertsState.value.firstOrNull { it.id == id }
                if (alert != null) clickState.value(alert)
                alert != null
            }
            styleRef[0] = style
            render(style, home, radiusMeters, alertsState.value)
        }
        ZoomButtons(controller, Modifier.align(Alignment.BottomEnd).padding(10.dp))
    }

    LaunchedEffect(home, radiusMeters, alerts) {
        styleRef[0]?.let { render(it, home, radiusMeters, alerts) }
    }
}

private fun render(style: Style, home: LatLngPoint?, radius: Double, alerts: List<Alert>) {
    if (!style.isFullyLoaded) return
    (style.getSource(ZONE_SOURCE) as? GeoJsonSource)?.setGeoJson(
        home?.let { FeatureCollection.fromFeature(Feature.fromGeometry(circle(it, radius))) } ?: FeatureCollection.fromFeatures(emptyList()),
    )
    (style.getSource(HOME_SOURCE) as? GeoJsonSource)?.setGeoJson(
        home?.let { FeatureCollection.fromFeature(Feature.fromGeometry(Point.fromLngLat(it.lng, it.lat))) } ?: FeatureCollection.fromFeatures(emptyList()),
    )
    (style.getSource(ALERTS_SOURCE) as? GeoJsonSource)?.setGeoJson(
        FeatureCollection.fromFeatures(
            alerts.map { a ->
                val type = AlertType.of(a.type)
                Feature.fromGeometry(Point.fromLngLat(a.lng, a.lat)).apply {
                    addStringProperty("id", a.id)
                    addStringProperty("icon", "pin-${type.key}")
                    // Las de seguridad se dibujan encima.
                    addNumberProperty("order", if (type.group == AlertGroup.SECURITY) 2 else 1)
                }
            },
        ),
    )
}

/** Polígono de 64 lados que aproxima el círculo de la zona. */
private fun circle(center: LatLngPoint, radiusMeters: Double): Polygon {
    val dLat = radiusMeters / 111_320.0
    val dLng = radiusMeters / (111_320.0 * cos(Math.toRadians(center.lat)))
    val ring = (0..64).map { i ->
        val t = 2 * Math.PI * i / 64
        Point.fromLngLat(center.lng + dLng * cos(t), center.lat + dLat * sin(t))
    }
    return Polygon.fromLngLats(listOf(ring))
}

/** Botones grandes de acercar y alejar: más fáciles que pellizcar para personas mayores. */
@Composable
private fun ZoomButtons(controller: MapController, modifier: Modifier = Modifier) {
    Column(modifier) {
        listOf(Icons.Rounded.Add to controller::zoomIn, Icons.Rounded.Remove to controller::zoomOut).forEach { (icon, action) ->
            Surface(
                onClick = action,
                shape = CircleShape,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(top = 8.dp).size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(28.dp)) }
            }
        }
    }
}

/** Para "¿Fue aquí?": el mapa se mueve y el pin queda fijo en el centro. */
@Composable
fun PickLocationMap(controller: MapController, modifier: Modifier = Modifier) {
    Box(modifier) {
        MapLibreView(controller, Modifier.fillMaxSize()) { _, _ -> }
        ZoomButtons(controller, Modifier.align(Alignment.BottomEnd).padding(10.dp))
    }
}

// ---------- Pines como imágenes para el mapa ----------

@Composable
private fun rememberPinBitmaps(): Map<String, Bitmap> {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val painters = AlertType.entries.associateWith { rememberVectorPainter(iconOf(it)) }
    val homePainter = rememberVectorPainter(Icons.Rounded.Home)
    return remember(density) {
        val sizePx = with(density) { 48.dp.roundToPx() }
        AlertType.entries.associate { t ->
            "pin-${t.key}" to pinBitmap(painters.getValue(t), styleOf(t.group).strong, sizePx, density, direction)
        } + (HOME_ICON to pinBitmap(homePainter, Color(0xFF4F46E5), with(density) { 52.dp.roundToPx() }, density, direction))
    }
}

private fun pinBitmap(painter: VectorPainter, color: Color, sizePx: Int, density: Density, direction: LayoutDirection): Bitmap {
    val image = ImageBitmap(sizePx, sizePx)
    CanvasDrawScope().draw(density, direction, Canvas(image), Size(sizePx.toFloat(), sizePx.toFloat())) {
        val r = size.minDimension / 2
        drawCircle(Color.Black.copy(alpha = 0.18f), radius = r * 0.96f, center = center + Offset(0f, r * 0.06f))
        drawCircle(Color.White, radius = r * 0.94f)
        drawCircle(color, radius = r * 0.78f)
        val icon = size.minDimension * 0.5f
        translate((size.width - icon) / 2, (size.height - icon) / 2) {
            with(painter) { draw(Size(icon, icon), colorFilter = ColorFilter.tint(Color.White)) }
        }
    }
    return image.asAndroidBitmap()
}
