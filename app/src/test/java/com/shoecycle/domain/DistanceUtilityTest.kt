package com.shoecycle.domain

import com.shoecycle.data.DistanceUnit
import com.shoecycle.data.UserSettingsData
import com.shoecycle.data.UserSettingsRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DistanceUtilityTest {

    private lateinit var userSettingsRepository: UserSettingsRepository
    private lateinit var distanceUtility: DistanceUtility

    @Before
    fun setUp() {
        userSettingsRepository = mock()
        distanceUtility = DistanceUtility(userSettingsRepository)
    }

    @Test
    fun `displayString formats miles correctly`() = runTest {
        // Given: Miles unit preference
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(userSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // When: Converting distance for display
        val result = distanceUtility.displayString(5.0)

        // Then: Distance is displayed in miles
        assertEquals("5", result)
    }

    @Test
    fun `displayString converts miles to kilometers correctly`() = runTest {
        // Given: Kilometers unit preference
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.KM)
        whenever(userSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // When: Converting 5 miles for display
        val result = distanceUtility.displayString(5.0)

        // Then: Distance is displayed in kilometers (5 * 1.609344 = 8.0467)
        assertEquals("8.05", result)
    }

    @Test
    fun `favoriteDistanceDisplayString returns empty for zero distance`() = runTest {
        // Given: Any unit preference
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(userSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // When: Converting zero distance
        val result = distanceUtility.favoriteDistanceDisplayString(0.0)

        // Then: Empty string is returned
        assertEquals("", result)
    }

    @Test
    fun `distance fromString parses miles correctly`() = runTest {
        // Given: Miles unit preference
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(userSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // When: Parsing string to distance
        val result = distanceUtility.distance(fromString = "5.5")

        // Then: Distance is parsed as miles
        assertEquals(5.5, result, 0.001)
    }

    @Test
    fun `distance fromString returns zero for empty string`() = runTest {
        // Given: Any unit preference
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(userSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // When: Parsing empty string
        val result = distanceUtility.distance(fromString = "")

        // Then: Zero is returned
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun `stravaDistance converts miles to meters correctly`() = runTest {
        // When: Converting 1 mile for Strava
        val result = distanceUtility.stravaDistance(1.0)

        // Then: Distance is converted to meters (1609.34)
        assertEquals(1609.34, result, 0.01)
    }

    @Test
    fun `getUnitLabel returns correct label for miles`() = runTest {
        // Given: Miles unit preference
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.MILES)
        whenever(userSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // When: Getting unit label
        val result = distanceUtility.getUnitLabel()

        // Then: Miles label is returned
        assertEquals("mi", result)
    }

    @Test
    fun `getUnitLabel returns correct label for kilometers`() = runTest {
        // Given: Kilometers unit preference
        val userSettings = UserSettingsData(distanceUnit = DistanceUnit.KM)
        whenever(userSettingsRepository.userSettingsFlow).thenReturn(flowOf(userSettings))

        // When: Getting unit label
        val result = distanceUtility.getUnitLabel()

        // Then: Kilometers label is returned
        assertEquals("km", result)
    }

    @Test
    fun `convertToMiles from kilometers works correctly`() = runTest {
        // When: Converting 10 km to miles
        val result = distanceUtility.convertToMiles(10.0, DistanceUnit.KM)

        // Then: Distance is converted to miles (10 * 0.621371 = 6.21)
        assertEquals(6.21371, result, 0.001)
    }

    @Test
    fun `convertFromMiles to kilometers works correctly`() = runTest {
        // When: Converting 5 miles to kilometers
        val result = distanceUtility.convertFromMiles(5.0, DistanceUnit.KM)

        // Then: Distance is converted to kilometers (5 * 1.609344 = 8.0467)
        assertEquals(8.0467, result, 0.01)
    }
}