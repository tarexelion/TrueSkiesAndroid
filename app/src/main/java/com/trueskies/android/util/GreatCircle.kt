package com.trueskies.android.util

import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

/**
 * Great-circle path utilities — ported from iOS GreatCircle.swift.
 * Uses spherical linear interpolation (SLERP) for smooth arc sampling.
 */
object GreatCircle {

    /**
     * Sample points along the great-circle arc between two coordinates.
     * Produces a smooth curved path on the map.
     *
     * @param from Origin coordinate
     * @param to Destination coordinate
     * @param numPoints Number of intermediate points (auto-calculated if null)
     * @return List of LatLng points along the arc
     */
    fun sampleArc(
        from: LatLng,
        to: LatLng,
        numPoints: Int? = null
    ): List<LatLng> {
        val distKm = haversineDistance(from, to)
        val points = numPoints ?: when {
            distKm < 800 -> 30   // Short haul
            distKm < 2500 -> 60  // Medium haul
            else -> 100          // Long haul
        }

        if (points < 2) return listOf(from, to)

        val lat1 = Math.toRadians(from.latitude)
        val lon1 = Math.toRadians(from.longitude)
        val lat2 = Math.toRadians(to.latitude)
        val lon2 = Math.toRadians(to.longitude)

        val d = 2 * asin(
            sqrt(
                sin((lat2 - lat1) / 2).pow(2) +
                        cos(lat1) * cos(lat2) * sin((lon2 - lon1) / 2).pow(2)
            )
        )

        if (d < 1e-10) return listOf(from, to)

        return (0..points).map { i ->
            val f = i.toDouble() / points
            val a = sin((1 - f) * d) / sin(d)
            val b = sin(f * d) / sin(d)
            val x = a * cos(lat1) * cos(lon1) + b * cos(lat2) * cos(lon2)
            val y = a * cos(lat1) * sin(lon1) + b * cos(lat2) * sin(lon2)
            val z = a * sin(lat1) + b * sin(lat2)
            val lat = atan2(z, sqrt(x * x + y * y))
            val lon = atan2(y, x)
            LatLng(Math.toDegrees(lat), Math.toDegrees(lon))
        }
    }

    /**
     * Haversine distance between two points in kilometers.
     */
    fun haversineDistance(from: LatLng, to: LatLng): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return earthRadiusKm * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
