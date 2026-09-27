package com.geoalarm.app.domain.engine

/** Android-independent snapshot of a location update, so the engine can be unit-tested
 * without a Robolectric/instrumented environment. Built from `android.location.Location`. */
data class LocationReading(
    val latitude: Double,
    val longitude: Double,
    /** Reported horizontal accuracy radius, in metres. Larger = less trustworthy. */
    val accuracyMeters: Float,
    val elapsedRealtimeMillis: Long,
    val speedMetersPerSecond: Float? = null
)
