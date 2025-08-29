package com.shoecycle.data.strava.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Production implementation of TokenStorage using EncryptedSharedPreferences.
 * Provides secure storage using Android Keystore encryption.
 */
class SecureTokenStorage(context: Context) : TokenStorage {
    
    companion object {
        private const val PREFS_NAME = "strava_secure_prefs"
    }
    
    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private var editor: SharedPreferences.Editor? = null
    
    override fun putString(key: String, value: String?) {
        getEditor().putString(key, value)
    }
    
    override fun getString(key: String, defaultValue: String?): String? {
        return encryptedPrefs.getString(key, defaultValue)
    }
    
    override fun putLong(key: String, value: Long) {
        getEditor().putLong(key, value)
    }
    
    override fun getLong(key: String, defaultValue: Long): Long {
        return encryptedPrefs.getLong(key, defaultValue)
    }
    
    override fun contains(key: String): Boolean {
        return encryptedPrefs.contains(key)
    }
    
    override fun clear() {
        getEditor().clear()
    }
    
    override fun apply() {
        editor?.apply()
        editor = null
    }
    
    private fun getEditor(): SharedPreferences.Editor {
        if (editor == null) {
            editor = encryptedPrefs.edit()
        }
        return editor!!
    }
}