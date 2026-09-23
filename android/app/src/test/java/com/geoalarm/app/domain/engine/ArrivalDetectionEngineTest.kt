package com.geoalarm.app.domain.engine

import com.geoalarm.app.domain.model.AlarmSoundType
import com.geoalarm.app.domain.model.AlarmStatus
import com.geoalarm.app.domain.model.GeoAlarm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ArrivalDetectionEngineTest {

    private lateinit var engine: ArrivalDetectionEngine
    private val destLat = 13.0
    private val destLon = 80.0

    private fun alarm(warn: Int? = 500, radius: Int = 100) = GeoAlarm(
        id = 1, destinationName = "Test Destination",
        latitude = destLat, longitude = destLon,
        warningDistanceMeters = warn, arrivalRadiusMeters = radius,
        soundType = AlarmSoundType.DEFAULT_RINGTONE, createdAt = 0L
    )

    /** A reading `distanceM` metres due south of the destination. */
    private fun readingAt(distanceM: Double, accuracy: Float, atMillis: Long): LocationReading {
        val dLat = distanceM / 111_320.0
        return LocationReading(destLat + dLat, destLon, accuracy, atMillis)
    }

    @Before
    fun setUp() {
        engine = ArrivalDetectionEngine()
    }

    @Test
    fun `single reading inside arrival radius does not confirm arrival`() {
        val (_, event) = engine.onLocationUpdate(alarm(), AlarmStatus.ACTIVE, readingAt(50.0, 10f, 1000))
        assertNull("one reading alone must not confirm arrival", event)
    }

    @Test
    fun `two consecutive good readings inside arrival radius confirm arrival`() {
        engine.onLocationUpdate(alarm(), AlarmStatus.WARNING_TRIGGERED, readingAt(50.0, 10f, 1000))
        val (_, event) = engine.onLocationUpdate(alarm(), AlarmStatus.WARNING_TRIGGERED, readingAt(45.0, 10f, 6000))
        assertTrue(event is ArrivalEvent.DestinationReached)
    }

    @Test
    fun `warning fires once two consecutive readings are inside warning but outside arrival`() {
        val a = alarm(warn = 500, radius = 100)
        engine.onLocationUpdate(a, AlarmStatus.ACTIVE, readingAt(480.0, 10f, 1000))
        val (_, event) = engine.onLocationUpdate(a, AlarmStatus.ACTIVE, readingAt(470.0, 10f, 6000))
        assertTrue(event is ArrivalEvent.WarningTriggered)
    }

    @Test
    fun `warning does not fire twice for the same approach`() {
        val a = alarm()
        engine.onLocationUpdate(a, AlarmStatus.ACTIVE, readingAt(480.0, 10f, 1000))
        val (_, first) = engine.onLocationUpdate(a, AlarmStatus.ACTIVE, readingAt(470.0, 10f, 6000))
        assertTrue(first is ArrivalEvent.WarningTriggered)
        // Caller would now persist status = WARNING_TRIGGERED; further updates use that status.
        val (_, second) = engine.onLocationUpdate(a, AlarmStatus.WARNING_TRIGGERED, readingAt(460.0, 10f, 11000))
        assertNull("must not re-fire the warning", second)
    }

    @Test
    fun `readings with poor accuracy never confirm arrival even inside the radius`() {
        val a = alarm()
        repeat(5) { i ->
            val (status, event) = engine.onLocationUpdate(
                a, AlarmStatus.WARNING_TRIGGERED, readingAt(30.0, 300f, 1000L * (i + 1))
            )
            assertFalse(status.reliableForDecision)
            assertNull(event)
        }
    }

    @Test
    fun `an implausible GPS jump is rejected and does not confirm arrival`() {
        val a = alarm()
        engine.onLocationUpdate(a, AlarmStatus.ACTIVE, readingAt(5000.0, 10f, 1000))
        // "Teleporting" 4950m in one second is not physically plausible.
        val (status, event) = engine.onLocationUpdate(a, AlarmStatus.ACTIVE, readingAt(50.0, 10f, 2000))
        assertFalse(status.reliableForDecision)
        assertNull(event)
    }

    @Test
    fun `a fast-moving user who skips between two updates still triggers via the segment check`() {
        // Arrival radius of 100m, but two consecutive good readings straddle the destination
        // (one just before, one just after) without either landing inside the radius alone.
        val a = alarm(radius = 100)
        val before = LocationReading(destLat + (150.0 / 111_320.0), destLon, 10f, 1000)
        val after = LocationReading(destLat - (150.0 / 111_320.0), destLon, 10f, 6000)
        engine.onLocationUpdate(a, AlarmStatus.WARNING_TRIGGERED, before)
        val (_, event) = engine.onLocationUpdate(a, AlarmStatus.WARNING_TRIGGERED, after)
        assertTrue("segment through the geofence should count as inside", event is ArrivalEvent.DestinationReached)
    }

    @Test
    fun `at-destination-only alarm has no separate warning phase`() {
        val a = alarm(warn = null)
        assertFalse(a.hasSeparateWarning)
        engine.onLocationUpdate(a, AlarmStatus.ACTIVE, readingAt(400.0, 10f, 1000))
        val (_, event) = engine.onLocationUpdate(a, AlarmStatus.ACTIVE, readingAt(390.0, 10f, 6000))
        assertNull("no warning distance means no WarningTriggered event", event)
    }

    @Test
    fun `paused alarm never fires events`() {
        val a = alarm()
        engine.onLocationUpdate(a, AlarmStatus.PAUSED, readingAt(10.0, 10f, 1000))
        val (_, event) = engine.onLocationUpdate(a, AlarmStatus.PAUSED, readingAt(5.0, 10f, 6000))
        assertNull(event)
    }

    @Test
    fun `cancelled alarm never fires events`() {
        val a = alarm()
        engine.onLocationUpdate(a, AlarmStatus.CANCELLED, readingAt(10.0, 10f, 1000))
        val (_, event) = engine.onLocationUpdate(a, AlarmStatus.CANCELLED, readingAt(5.0, 10f, 6000))
        assertNull(event)
    }

    @Test
    fun `forget clears tracking state for an alarm`() {
        val a = alarm()
        engine.onLocationUpdate(a, AlarmStatus.WARNING_TRIGGERED, readingAt(50.0, 10f, 1000))
        engine.forget(a.id)
        // After forgetting, a single reading must not immediately confirm (counter reset to 0->1).
        val (_, event) = engine.onLocationUpdate(a, AlarmStatus.WARNING_TRIGGERED, readingAt(45.0, 10f, 6000))
        assertNull(event)
    }
}
