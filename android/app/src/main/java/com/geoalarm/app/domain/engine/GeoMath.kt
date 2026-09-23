package com.geoalarm.app.domain.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Pure geographic math, deliberately free of any Android dependency so it is trivially unit-testable. */
object GeoMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Great-circle distance between two points, in metres. Accurate enough for alarm-radius
     * decisions at the distances GeoAlarm cares about (straight-line, not routed). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dPhi = Math.toRadians(lat2 - lat1)
        val dLambda = Math.toRadians(lon2 - lon1)

        val a = sin(dPhi / 2) * sin(dPhi / 2) +
            cos(phi1) * cos(phi2) * sin(dLambda / 2) * sin(dLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Distance from a point to the nearest point on the segment [a, b], approximated by
     * treating the segment as locally flat (fine for segments of a few hundred metres).
     * Used to catch "the user's GPS jumped past the geofence between two readings" cases
     * for a fast-moving user with an infrequent update rate.
     */
    fun distanceToSegmentMeters(
        pointLat: Double, pointLon: Double,
        aLat: Double, aLon: Double,
        bLat: Double, bLon: Double
    ): Double {
        // Convert to a local equirectangular projection centred on `a`, in metres.
        val metersPerDegLat = 111_320.0
        val metersPerDegLon = 111_320.0 * cos(Math.toRadians(aLat))

        val ax = 0.0
        val ay = 0.0
        val bx = (bLon - aLon) * metersPerDegLon
        val by = (bLat - aLat) * metersPerDegLat
        val px = (pointLon - aLon) * metersPerDegLon
        val py = (pointLat - aLat) * metersPerDegLat

        val abx = bx - ax
        val aby = by - ay
        val abLenSq = abx * abx + aby * aby
        val t = if (abLenSq <= 1e-6) 0.0 else (((px - ax) * abx + (py - ay) * aby) / abLenSq).coerceIn(0.0, 1.0)
        val closestX = ax + t * abx
        val closestY = ay + t * aby
        val dx = px - closestX
        val dy = py - closestY
        return sqrt(dx * dx + dy * dy)
    }
}
