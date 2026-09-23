package com.geoalarm.app.domain.engine

import com.geoalarm.app.domain.model.AlarmStatus
import com.geoalarm.app.domain.model.GeoAlarm
import kotlin.math.max

enum class GpsQuality { GOOD, FAIR, POOR }

/** Snapshot for the active-alarm screen's "Emergency Location Status" panel. */
data class DistanceStatus(
    val alarmId: Long,
    val distanceMeters: Double,
    val accuracyMeters: Float,
    val quality: GpsQuality,
    /** False when this reading was too inaccurate, or too implausible a jump, to count
     * toward a warning/arrival decision. The distance shown is still the best estimate. */
    val reliableForDecision: Boolean,
    val lastUpdateElapsedRealtimeMillis: Long
)

sealed interface ArrivalEvent {
    val alarmId: Long

    data class WarningTriggered(override val alarmId: Long, val distanceMeters: Double) : ArrivalEvent
    data class DestinationReached(override val alarmId: Long, val distanceMeters: Double) : ArrivalEvent
}

/**
 * Decides, from a stream of raw GPS readings, when an alarm's warning and arrival should
 * fire — without over-reacting to normal GPS noise (Section 9 / 12 / 15A).
 *
 * Rules of thumb encoded here:
 *  - A reading with a very large accuracy circle is shown, but never fires an alarm.
 *  - Arrival/warning requires [CONFIRM_COUNT] consecutive acceptable readings inside the
 *    zone, not just one — filters a single noisy point sitting inside the geofence.
 *  - A physically implausible jump between two consecutive readings (faster than a plane)
 *    is rejected outright and does not reset the confirmation counters, so one bad fix
 *    can't either falsely confirm or falsely interrupt an in-progress confirmation.
 *  - Between two accepted readings, the *segment* connecting them is checked against the
 *    geofence too, so a fast-moving user (e.g. a car) whose GPS updates are sparse doesn't
 *    "skip over" a small arrival radius entirely.
 *
 * This class is Android-free and unit-testable; callers translate platform `Location`
 * objects into [LocationReading] and persisted status changes into future calls.
 */
class ArrivalDetectionEngine {

    private data class TrackerState(
        var consecutiveInsideWarning: Int = 0,
        var consecutiveInsideArrival: Int = 0,
        var lastAccepted: LocationReading? = null
    )

    private val trackers = mutableMapOf<Long, TrackerState>()

    fun forget(alarmId: Long) {
        trackers.remove(alarmId)
    }

    fun reset() {
        trackers.clear()
    }

    /**
     * Feed one new reading for [alarm], currently in [currentStatus].
     * Returns the distance snapshot (always) plus at most one state-changing event.
     * The caller is responsible for persisting any [ArrivalEvent] as a status change;
     * this class does not call itself again for the same transition once [currentStatus]
     * reflects it, so it is safe to call once per location update with no external de-duplication.
     */
    fun onLocationUpdate(
        alarm: GeoAlarm,
        currentStatus: AlarmStatus,
        reading: LocationReading
    ): Pair<DistanceStatus, ArrivalEvent?> {
        val tracker = trackers.getOrPut(alarm.id) { TrackerState() }

        val distance = GeoMath.distanceMeters(reading.latitude, reading.longitude, alarm.latitude, alarm.longitude)
        val quality = classify(reading.accuracyMeters)
        val accurateEnough = reading.accuracyMeters in 0f..MAX_ACCEPTABLE_ACCURACY_METERS

        val previous = tracker.lastAccepted
        val isJump = previous != null && isImplausibleJump(previous, reading)
        val reliable = accurateEnough && !isJump

        val status = DistanceStatus(
            alarmId = alarm.id,
            distanceMeters = distance,
            accuracyMeters = reading.accuracyMeters,
            quality = quality,
            reliableForDecision = reliable,
            lastUpdateElapsedRealtimeMillis = reading.elapsedRealtimeMillis
        )

        if (!reliable) {
            // Do not touch the counters: one bad fix should neither confirm nor cancel a streak.
            return status to null
        }

        // A fast-moving user can jump over a small geofence between two sparse updates;
        // treat the closest approach along the segment as "inside" if it dips into the ring.
        val closestApproach = previous?.let {
            GeoMath.distanceToSegmentMeters(
                alarm.latitude, alarm.longitude,
                it.latitude, it.longitude,
                reading.latitude, reading.longitude
            )
        } ?: distance
        val effectiveDistance = minOf(distance, closestApproach)

        val insideArrival = effectiveDistance <= alarm.arrivalRadiusMeters
        val insideWarning = alarm.hasSeparateWarning &&
            effectiveDistance <= (alarm.warningDistanceMeters ?: Int.MAX_VALUE)

        tracker.consecutiveInsideArrival = if (insideArrival) tracker.consecutiveInsideArrival + 1 else 0
        tracker.consecutiveInsideWarning = if (insideWarning) tracker.consecutiveInsideWarning + 1 else 0
        tracker.lastAccepted = reading

        val event: ArrivalEvent? = when {
            currentStatus.isTerminal || currentStatus == AlarmStatus.PAUSED || currentStatus == AlarmStatus.CREATED -> null
            tracker.consecutiveInsideArrival >= CONFIRM_COUNT ->
                ArrivalEvent.DestinationReached(alarm.id, distance)
            currentStatus == AlarmStatus.ACTIVE &&
                alarm.hasSeparateWarning &&
                tracker.consecutiveInsideWarning >= CONFIRM_COUNT ->
                ArrivalEvent.WarningTriggered(alarm.id, distance)
            else -> null
        }

        return status to event
    }

    private fun classify(accuracyMeters: Float): GpsQuality = when {
        accuracyMeters <= GOOD_ACCURACY_METERS -> GpsQuality.GOOD
        accuracyMeters <= FAIR_ACCURACY_METERS -> GpsQuality.FAIR
        else -> GpsQuality.POOR
    }

    private fun isImplausibleJump(previous: LocationReading, next: LocationReading): Boolean {
        val dtSeconds = (next.elapsedRealtimeMillis - previous.elapsedRealtimeMillis) / 1000.0
        if (dtSeconds <= 0) return true // out-of-order or duplicate-timestamp reading
        val moved = GeoMath.distanceMeters(previous.latitude, previous.longitude, next.latitude, next.longitude)
        val impliedSpeed = moved / dtSeconds
        // Allow generous slack for combined accuracy error of both fixes.
        val slackMeters = max(previous.accuracyMeters, next.accuracyMeters) * 2.0
        val plausibleDistance = MAX_PLAUSIBLE_SPEED_MPS * dtSeconds + slackMeters
        return moved > plausibleDistance
    }

    companion object {
        /** Consecutive acceptable readings required inside a zone before it fires. */
        const val CONFIRM_COUNT = 2

        /** Readings less accurate than this never count toward a warning/arrival decision. */
        const val MAX_ACCEPTABLE_ACCURACY_METERS = 100f

        const val GOOD_ACCURACY_METERS = 20f
        const val FAIR_ACCURACY_METERS = 50f

        /** ~360 km/h: generous enough for any car/train, tight enough to reject GPS teleports. */
        const val MAX_PLAUSIBLE_SPEED_MPS = 100.0
    }
}
