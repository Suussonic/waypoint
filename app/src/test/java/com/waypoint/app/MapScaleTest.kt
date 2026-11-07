package com.waypoint.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapScaleTest {
    @Test fun metricTicksUseNiceDistancesAndSwitchToKilometers() {
        val near = requireNotNull(chooseMapScale(950.0, imperial = false))
        assertEquals("500 m", near.label)
        assertEquals(500.0, near.meters, 0.001)
        assertTrue(near.widthFraction in .5f..1f)

        val far = requireNotNull(chooseMapScale(1200.0, imperial = false))
        assertEquals("1 km", far.label)
        assertEquals(1000.0, far.meters, 0.001)
    }

    @Test fun imperialTicksUseFeetAndMiles() {
        assertEquals("200 ft", chooseMapScale(300 * .3048, imperial = true)?.label)
        assertEquals("2 mi", chooseMapScale(3 * 1609.344, imperial = true)?.label)
    }

    @Test fun invalidProjectedDistancesHideScale() {
        assertNull(chooseMapScale(0.0, imperial = false))
        assertNull(chooseMapScale(Double.NaN, imperial = true))
        assertNull(chooseMapScale(Double.POSITIVE_INFINITY, imperial = false))
    }
}
