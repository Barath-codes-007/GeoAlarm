package com.geoalarm.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GeoMathTest {

    @Test
    fun `distance between identical points is zero`() {
        assertEquals(0.0, GeoMath.distanceMeters(13.08, 80.27, 13.08, 80.27), 0.001)
    }

    @Test
    fun `distance of one degree latitude is about 111 km`() {
        val d = GeoMath.distanceMeters(0.0, 0.0, 1.0, 0.0)
        assertTrue("expected ~111km, got $d", abs(d - 111_195.0) < 500)
    }

    @Test
    fun `known city pair distance is approximately correct`() {
        // Chennai Central approx -> Chennai Airport approx: ~18-19 km straight-line.
        val d = GeoMath.distanceMeters(13.0827, 80.2707, 12.9941, 80.1709)
        assertTrue("expected 15-20km, got $d", d in 14_000.0..21_000.0)
    }

    @Test
    fun `distance to segment is zero when point lies on the segment`() {
        val d = GeoMath.distanceToSegmentMeters(
            pointLat = 13.0, pointLon = 80.001,
            aLat = 13.0, aLon = 80.0,
            bLat = 13.0, bLon = 80.002
        )
        assertTrue("expected near zero, got $d", d < 1.0)
    }

    @Test
    fun `distance to segment matches distance to nearest endpoint when point is off the end`() {
        val d = GeoMath.distanceToSegmentMeters(
            pointLat = 14.0, pointLon = 80.0,
            aLat = 13.0, aLon = 80.0,
            bLat = 13.001, bLon = 80.0
        )
        val toA = GeoMath.distanceMeters(14.0, 80.0, 13.0, 80.0)
        assertTrue(abs(d - toA) < 50.0)
    }
}
