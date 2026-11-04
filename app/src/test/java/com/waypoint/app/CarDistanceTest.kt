package com.waypoint.app

import androidx.car.app.model.Distance
import org.junit.Assert.*
import org.junit.Test

class CarDistanceTest {
    @Test fun imperialGuidanceUsesFeetAndMiles() {
        assertEquals(Distance.UNIT_FEET,carDistance(30.48,true).displayUnit)
        assertEquals(100.0,carDistance(30.48,true).displayDistance,0.01)
        assertEquals(Distance.UNIT_MILES_P1,carDistance(1609.344,true).displayUnit)
        assertEquals(1.0,carDistance(1609.344,true).displayDistance,0.001)
        assertEquals(Distance.UNIT_MILES,carDistance(16093.44,true).displayUnit)
    }
    @Test fun metricGuidanceUsesMetersAndKilometers() {
        assertEquals(Distance.UNIT_METERS,carDistance(100.0).displayUnit)
        assertEquals(Distance.UNIT_KILOMETERS_P1,carDistance(1500.0).displayUnit)
        assertEquals(1.5,carDistance(1500.0).displayDistance,0.001)
        assertEquals(Distance.UNIT_KILOMETERS,carDistance(15000.0).displayUnit)
    }
    @Test fun invalidProgressNeverProducesInvalidHostDistances() {
        for(imperial in listOf(false,true)) for(value in listOf(-100.0,Double.NaN,Double.POSITIVE_INFINITY)) {
            assertEquals(0.0,carDistance(value,imperial).displayDistance,0.0)
        }
    }
}
