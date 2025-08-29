package com.shoecycle.data.strava

interface SecretDeobfuscator {
    fun getClearString(): String
}

class DefaultDeobfuscator : SecretDeobfuscator {
    override fun getClearString(): String {
        return "ThisIsNotAValidSecretKey"
    }
}

/**
 * Generates a clear string for a given deobfuscator type.
 * This follows the same pattern as the iOS implementation using runtime class loading.
 */
enum class SecretKeyFactory {
    STRAVA;
    
    /**
     * Creates a clear string from the deobfuscator called out by case. If a deobfuscator is not found, then the clear
     * string from DefaultDeobfuscator is used.
     * @return Clear string
     */
    fun getClearString(): String {
        var deobfuscator: SecretDeobfuscator = DefaultDeobfuscator()
        
        when (this) {
            STRAVA -> {
                // Deobfuscators are loaded using Class.forName so that this repo will compile without the desired deobfuscator.
                // This way the concrete deobfuscator file can remain untracked, and someone can download this repo and use it without modification.
                try {
                    val clazz = Class.forName("com.shoecycle.data.strava.StravaSecretKeyDeobfuscator")
                    val constructor = clazz.getDeclaredConstructor()
                    deobfuscator = constructor.newInstance() as SecretDeobfuscator
                } catch (e: Exception) {
                    // Class not found or cannot be instantiated, use default
                    // This is expected when Secrets.kt is not present
                }
            }
        }
        
        return deobfuscator.getClearString()
    }
}