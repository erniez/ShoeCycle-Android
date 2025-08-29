package com.shoecycle.data.strava.storage

/**
 * In-memory implementation of TokenStorage for testing.
 * Stores values in memory without any encryption or persistence.
 */
class InMemoryTokenStorage : TokenStorage {
    
    private val storage = mutableMapOf<String, Any?>()
    
    override fun putString(key: String, value: String?) {
        storage[key] = value
    }
    
    override fun getString(key: String, defaultValue: String?): String? {
        return storage[key] as? String ?: defaultValue
    }
    
    override fun putLong(key: String, value: Long) {
        storage[key] = value
    }
    
    override fun getLong(key: String, defaultValue: Long): Long {
        return storage[key] as? Long ?: defaultValue
    }
    
    override fun contains(key: String): Boolean {
        return storage.containsKey(key)
    }
    
    override fun clear() {
        storage.clear()
    }
    
    override fun apply() {
        // No-op for in-memory storage
    }
}