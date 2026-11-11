package com.jonathan.chatsimpapp.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLngPoint(val lat: Double, val lng: Double)

object Distance {
    /** Distancia en metros entre dos puntos (fórmula de haversine). */
    fun meters(a: LatLngPoint, b: LatLngPoint): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * r * atan2(sqrt(h), sqrt(1 - h))
    }

    /** Cuadras aproximadas (una cuadra ≈ 100 m), redondeadas y nunca menos de 1. */
    fun blocks(meters: Double): Int = (meters / 100.0).roundToInt().coerceAtLeast(1)
}
