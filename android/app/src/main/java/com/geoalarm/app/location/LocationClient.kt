package com.geoalarm.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.SystemClock
import com.geoalarm.app.domain.engine.LocationReading
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Section 10 / 15B: how often to ask for a location fix, based on distance to the nearest
 * relevant destination. Called far *less* often when nothing needs monitoring soon. */
enum class UpdateFrequency(val intervalMillis: Long, val priority: Int) {
    /** No alarm needs checking within the next tier of distance. */
    BATTERY_SAVER(60_000L, Priority.PRIORITY_BALANCED_POWER_ACCURACY),
    /** Within a few kilometres of some destination. */
    NORMAL(20_000L, Priority.PRIORITY_BALANCED_POWER_ACCURACY),
    /** Within warning range: accuracy matters more than battery now. */
    APPROACHING(7_000L, Priority.PRIORITY_HIGH_ACCURACY),
    /** Inside or just outside the arrival radius: confirm quickly. */
    ARRIVING(3_000L, Priority.PRIORITY_HIGH_ACCURACY);

    companion object {
        /** Picks a tier from the closest monitored alarm's distance, given its own thresholds. */
        fun forDistance(distanceMeters: Double, warningMeters: Int?, arrivalRadiusMeters: Int): UpdateFrequency = when {
            distanceMeters <= arrivalRadiusMeters * 3 -> ARRIVING
            warningMeters != null && distanceMeters <= warningMeters * 1.5 -> APPROACHING
            distanceMeters <= 5_000 -> NORMAL
            else -> BATTERY_SAVER
        }
    }
}

/** Thin wrapper so the rest of the app depends on [LocationReading], not a Play Services type. */
class LocationClient(private val context: Context) {
    private val fused: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // Caller (LocationMonitoringService) never starts this
    // flow without first confirming ACCESS_FINE_LOCATION is granted; see PermissionFlow.
    fun observe(frequency: UpdateFrequency): Flow<LocationReading> = callbackFlow {
        val request = LocationRequest.Builder(frequency.priority, frequency.intervalMillis)
            .setMinUpdateIntervalMillis(frequency.intervalMillis / 2)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(
                    LocationReading(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else Float.MAX_VALUE,
                        elapsedRealtimeMillis = if (loc.elapsedRealtimeNanos > 0) {
                            loc.elapsedRealtimeNanos / 1_000_000
                        } else SystemClock.elapsedRealtime(),
                        speedMetersPerSecond = if (loc.hasSpeed()) loc.speed else null
                    )
                )
            }
        }
        fused.requestLocationUpdates(request, callback, context.mainLooper)
        awaitClose { fused.removeLocationUpdates(callback) }
    }.distinctUntilChanged()
}
