package com.shoecycle.data.strava.storage

/**
 * Interface for token storage abstraction.
 * Allows for different implementations (secure, in-memory) for production and testing.
 */
interface TokenStorage {
    fun putString(key: String, value: String?)
    fun getString(key: String, defaultValue: String?): String?
    fun putLong(key: String, value: Long)
    fun getLong(key: String, defaultValue: Long): Long
    fun contains(key: String): Boolean
    fun clear()
    fun apply()
}