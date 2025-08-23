package com.shoecycle.domain

import com.shoecycle.data.DistanceUnit
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddDistanceUtilityTest {

    @Test
    fun `displayString formats miles correctly`() {
        // When: Converting distance for display with MILES unit
        val result = DistanceUtility.displayString(5.0, DistanceUnit.MILES)

        // Then: Distance is displayed in miles
        assertEquals("5", result)
    }

    @Test
    fun `displayString converts miles to kilometers correctly`() {
        // When: Converting 5 miles for display with KM unit
        val result = DistanceUtility.displayString(5.0, DistanceUnit.KM)

        // Then: Distance is displayed in kilometers (5 * 1.609344 = 8.05)
        assertEquals("8.05", result)
    }

    @Test
    fun `favoriteDistanceDisplayString returns empty for zero`() {
        // When: Converting zero distance with MILES unit
        val result = DistanceUtility.favoriteDistanceDisplayString(0.0, DistanceUnit.MILES)

        // Then: Empty string is returned
        assertEquals("", result)
    }

    @Test
    fun `favoriteDistanceDisplayString returns formatted string for positive value`() {
        // When: Converting positive distance with KM unit
        val result = DistanceUtility.favoriteDistanceDisplayString(10.0, DistanceUnit.KM)

        // Then: Formatted string is returned (10 miles = 16.09 km)
        assertEquals("16.09", result)
    }

    @Test
    fun `distance fromString parses miles correctly`() {
        // When: Parsing string to distance with MILES unit
        val result = DistanceUtility.distance("5.5", DistanceUnit.MILES)

        // Then: Distance is parsed as miles
        assertEquals(5.5, result, 0.001)
    }

    @Test
    fun `distance fromString parses and converts kilometers correctly`() {
        // When: Parsing "10" with KM unit (user enters 10km, we store as miles)
        val result = DistanceUtility.distance("10", DistanceUnit.KM)

        // Then: Distance is converted to miles for storage (10 km = 6.21 miles)
        assertEquals(6.21371, result, 0.001)
    }

    @Test
    fun `distance fromString returns zero for empty string`() {
        // When: Parsing empty string with MILES unit
        val result = DistanceUtility.distance("", DistanceUnit.MILES)

        // Then: Zero is returned
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `distance fromDouble converts for display correctly`() {
        // When: Converting stored miles value for KM display
        val result = DistanceUtility.distance(5.0, DistanceUnit.KM)

        // Then: Distance is converted to km (5 miles = 8.05 km)
        assertEquals(8.04672, result, 0.001)
    }

    @Test
    fun `milesToMeters converts miles to meters correctly`() {
        // When: Converting 1 mile to meters
        val result = DistanceUtility.milesToMeters(1.0)

        // Then: Distance is converted to meters (1609.34)
        assertEquals(1609.34, result, 0.01)
    }

    @Test
    fun `getUnitLabel returns correct label for miles`() {
        // When: Getting unit label for MILES
        val result = DistanceUtility.getUnitLabel(DistanceUnit.MILES)

        // Then: Miles label is returned
        assertEquals("mi", result)
    }

    @Test
    fun `getUnitLabel returns correct label for kilometers`() {
        // When: Getting unit label for KM
        val result = DistanceUtility.getUnitLabel(DistanceUnit.KM)

        // Then: Kilometers label is returned
        assertEquals("km", result)
    }

    @Test
    fun `convertToMiles from kilometers works correctly`() {
        // When: Converting 10 km to miles
        val result = DistanceUtility.convertToMiles(10.0, DistanceUnit.KM)

        // Then: Distance is converted to miles (10 * 0.621371 = 6.21)
        assertEquals(6.21371, result, 0.001)
    }

    @Test
    fun `convertToMiles from miles returns same value`() {
        // When: Converting 10 miles to miles (no conversion)
        val result = DistanceUtility.convertToMiles(10.0, DistanceUnit.MILES)

        // Then: Same value is returned
        assertEquals(10.0, result, 0.001)
    }

    @Test
    fun `convertFromMiles to kilometers works correctly`() {
        // When: Converting 5 miles to kilometers
        val result = DistanceUtility.convertFromMiles(5.0, DistanceUnit.KM)

        // Then: Distance is converted to kilometers (5 * 1.609344 = 8.0467)
        assertEquals(8.04672, result, 0.001)
    }

    @Test
    fun `convertFromMiles to miles returns same value`() {
        // When: Converting 5 miles to miles (no conversion)
        val result = DistanceUtility.convertFromMiles(5.0, DistanceUnit.MILES)

        // Then: Same value is returned
        assertEquals(5.0, result, 0.001)
    }

    @Test
    fun `format returns properly formatted string`() {
        // When: Formatting various distances
        val result1 = DistanceUtility.format(5.0)
        val result2 = DistanceUtility.format(5.5)
        val result3 = DistanceUtility.format(5.567)
        val result4 = DistanceUtility.format(5.001)

        // Then: Proper formatting is applied
        assertEquals("5", result1)
        assertEquals("5.5", result2)
        assertEquals("5.57", result3)
        assertEquals("5", result4)
    }
}