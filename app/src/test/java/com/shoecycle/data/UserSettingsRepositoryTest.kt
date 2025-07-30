package com.shoecycle.data

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class UserSettingsRepositoryTest {

    // Note: Repository tests with DataStore would typically use TestDataStore 
    // or dependency injection to avoid Android Context dependencies
    // For now, focusing on testing data classes and enums without Android dependencies


    @Test
    fun `enum companion object fromOrdinal should return correct values`() {
        // Test DistanceUnit.fromOrdinal
        assertEquals(DistanceUnit.MILES, DistanceUnit.fromOrdinal(0))
        assertEquals(DistanceUnit.KM, DistanceUnit.fromOrdinal(1))
        assertEquals(DistanceUnit.MILES, DistanceUnit.fromOrdinal(999)) // Invalid ordinal defaults to MILES
        
        // Test FirstDayOfWeek.fromOrdinal
        assertEquals(FirstDayOfWeek.SUNDAY, FirstDayOfWeek.fromOrdinal(0))
        assertEquals(FirstDayOfWeek.MONDAY, FirstDayOfWeek.fromOrdinal(1))
        assertEquals(FirstDayOfWeek.MONDAY, FirstDayOfWeek.fromOrdinal(999)) // Invalid ordinal defaults to MONDAY
    }

    @Test
    fun `UserSettingsData should have correct default values`() {
        val defaultData = UserSettingsData()
        
        assertEquals(DistanceUnit.MILES, defaultData.distanceUnit)
        assertEquals(FirstDayOfWeek.MONDAY, defaultData.firstDayOfWeek)
        assertEquals(0.0, defaultData.favorite1, 0.01)
        assertEquals(0.0, defaultData.favorite2, 0.01)
        assertEquals(0.0, defaultData.favorite3, 0.01)
        assertEquals(0.0, defaultData.favorite4, 0.01)
        assertEquals(false, defaultData.healthConnectEnabled)
        assertEquals(false, defaultData.stravaEnabled)
    }
}