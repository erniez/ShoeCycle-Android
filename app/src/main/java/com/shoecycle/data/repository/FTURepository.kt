package com.shoecycle.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shoecycle.domain.FTUHintManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val FTU_PREFERENCES_NAME = "ftu_preferences"
private val Context.ftuDataStore by preferencesDataStore(name = FTU_PREFERENCES_NAME)

/**
 * Repository for managing First Time User hint preferences.
 * Stores completed hints and provides reactive streams for hint state.
 */
class FTURepository(private val context: Context) {
    
    companion object {
        private const val TAG = "FTURepository"
        private val COMPLETED_HINTS = stringSetPreferencesKey(FTUHintManager.COMPLETED_HINTS_KEY)
    }
    
    private val hintManager = FTUHintManager()
    
    /**
     * Flow of completed hint keys
     */
    val completedHintsFlow: Flow<Set<String>> = context.ftuDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading completed hints", exception)
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[COMPLETED_HINTS] ?: emptySet()
        }
    
    /**
     * Flow of the next available hint, or null if all hints are completed
     */
    val nextHintFlow: Flow<FTUHintManager.HintKey?> = completedHintsFlow
        .map { completedHints ->
            hintManager.getNextHint(completedHints)
        }
    
    /**
     * Marks a hint as completed
     */
    suspend fun completeHint(hintKey: FTUHintManager.HintKey) {
        try {
            context.ftuDataStore.edit { preferences ->
                val currentHints = preferences[COMPLETED_HINTS] ?: emptySet()
                preferences[COMPLETED_HINTS] = currentHints + hintKey.key
                Log.d(TAG, "Completed hint: ${hintKey.key}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save completed hint: ${hintKey.key}", e)
        }
    }
    
    /**
     * Marks a hint as completed by its string key
     */
    suspend fun completeHint(hintKey: String) {
        FTUHintManager.HintKey.fromKey(hintKey)?.let { hint ->
            completeHint(hint)
        }
    }
    
    /**
     * Resets all completed hints (useful for testing)
     */
    suspend fun resetAllHints() {
        try {
            context.ftuDataStore.edit { preferences ->
                preferences.remove(COMPLETED_HINTS)
                Log.d(TAG, "Reset all FTU hints")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to reset FTU hints", e)
        }
    }
    
    /**
     * Checks if a specific hint has been completed
     */
    suspend fun isHintCompleted(hintKey: FTUHintManager.HintKey): Boolean {
        return try {
            val completedHints = completedHintsFlow.first()
            hintManager.isHintCompleted(hintKey, completedHints)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking hint completion", e)
            false
        }
    }
}