package com.shoecycle.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import android.util.Log
import java.io.IOException

class UserSettingsRepository(private val context: Context) {
    
    private object PreferenceKeys {
        val DISTANCE_UNIT = intPreferencesKey("distance_unit")
        val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")
        val FAVORITE_1 = doublePreferencesKey("favorite_1")
        val FAVORITE_2 = doublePreferencesKey("favorite_2")
        val FAVORITE_3 = doublePreferencesKey("favorite_3")
        val FAVORITE_4 = doublePreferencesKey("favorite_4")
        val HEALTH_CONNECT_ENABLED = booleanPreferencesKey("health_connect_enabled")
        val STRAVA_ENABLED = booleanPreferencesKey("strava_enabled")
        val SELECTED_SHOE_ID = stringPreferencesKey("selected_shoe_id")
    }
    
    val userSettingsFlow: Flow<UserSettingsData> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("UserSettingsRepository", "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val distanceUnitOrdinal = preferences[PreferenceKeys.DISTANCE_UNIT] ?: DistanceUnit.MILES.ordinal
            val firstDayOrdinal = preferences[PreferenceKeys.FIRST_DAY_OF_WEEK] ?: FirstDayOfWeek.MONDAY.ordinal
            
            UserSettingsData(
                distanceUnit = DistanceUnit.fromOrdinal(distanceUnitOrdinal),
                firstDayOfWeek = FirstDayOfWeek.fromOrdinal(firstDayOrdinal),
                favorite1 = preferences[PreferenceKeys.FAVORITE_1] ?: 0.0,
                favorite2 = preferences[PreferenceKeys.FAVORITE_2] ?: 0.0,
                favorite3 = preferences[PreferenceKeys.FAVORITE_3] ?: 0.0,
                favorite4 = preferences[PreferenceKeys.FAVORITE_4] ?: 0.0,
                healthConnectEnabled = preferences[PreferenceKeys.HEALTH_CONNECT_ENABLED] ?: false,
                stravaEnabled = preferences[PreferenceKeys.STRAVA_ENABLED] ?: false,
                selectedShoeId = preferences[PreferenceKeys.SELECTED_SHOE_ID]
            )
        }
    
    suspend fun updateDistanceUnit(unit: DistanceUnit) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferenceKeys.DISTANCE_UNIT] = unit.ordinal
            }
        } catch (exception: IOException) {
            Log.e("UserSettingsRepository", "Error updating distance unit", exception)
        }
    }
    
    suspend fun updateFirstDayOfWeek(firstDay: FirstDayOfWeek) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferenceKeys.FIRST_DAY_OF_WEEK] = firstDay.ordinal
            }
        } catch (exception: IOException) {
            Log.e("UserSettingsRepository", "Error updating first day of week", exception)
        }
    }
    
    suspend fun updateFavorite1(distance: Double) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferenceKeys.FAVORITE_1] = distance
            }
        } catch (exception: IOException) {
            Log.e("UserSettingsRepository", "Error updating favorite 1", exception)
        }
    }
    
    suspend fun updateFavorite2(distance: Double) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferenceKeys.FAVORITE_2] = distance
            }
        } catch (exception: IOException) {
            Log.e("UserSettingsRepository", "Error updating favorite 2", exception)
        }
    }
    
    suspend fun updateFavorite3(distance: Double) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferenceKeys.FAVORITE_3] = distance
            }
        } catch (exception: IOException) {
            Log.e("UserSettingsRepository", "Error updating favorite 3", exception)
        }
    }
    
    suspend fun updateFavorite4(distance: Double) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferenceKeys.FAVORITE_4] = distance
            }
        } catch (exception: IOException) {
            Log.e("UserSettingsRepository", "Error updating favorite 4", exception)
        }
    }
    
    suspend fun updateHealthConnectEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferenceKeys.HEALTH_CONNECT_ENABLED] = enabled
            }
        } catch (exception: IOException) {
            Log.e("UserSettingsRepository", "Error updating health connect enabled", exception)
        }
    }
    
    suspend fun updateStravaEnabled(enabled: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferenceKeys.STRAVA_ENABLED] = enabled
            }
        } catch (exception: IOException) {
            Log.e("UserSettingsRepository", "Error updating strava enabled", exception)
        }
    }
    
    suspend fun updateSelectedShoeId(shoeId: String?) {
        try {
            context.dataStore.edit { preferences ->
                if (shoeId != null) {
                    preferences[PreferenceKeys.SELECTED_SHOE_ID] = shoeId
                } else {
                    preferences.remove(PreferenceKeys.SELECTED_SHOE_ID)
                }
            }
        } catch (exception: IOException) {
            Log.e("UserSettingsRepository", "Error updating selected shoe ID", exception)
        }
    }
}