package com.fuseforge.cardash.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Handles app-wide preferences storage and retrieval
 */
class AppPreferences(context: Context) {

    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME, Context.MODE_PRIVATE
    )

    // Verbose logging setting
    var verboseLoggingEnabled: Boolean
        get() = preferences.getBoolean(KEY_VERBOSE_LOGGING, false)
        set(value) = preferences.edit { putBoolean(KEY_VERBOSE_LOGGING, value) }

    // Data collection frequency in milliseconds
    var dataCollectionFrequencyMs: Int
        get() = preferences.getInt(KEY_DATA_COLLECTION_FREQUENCY, DEFAULT_DATA_COLLECTION_FREQUENCY)
        set(value) = preferences.edit { putInt(KEY_DATA_COLLECTION_FREQUENCY, value) }

    // Combined reading storage frequency in milliseconds
    var storageFrequencyMs: Int
        get() = preferences.getInt(KEY_STORAGE_FREQUENCY, DEFAULT_STORAGE_FREQUENCY)
        set(value) = preferences.edit { putInt(KEY_STORAGE_FREQUENCY, value) }

    companion object {
        private const val PREFERENCES_NAME = "cardash_preferences"
        
        private const val KEY_VERBOSE_LOGGING = "verbose_logging_enabled"
        private const val KEY_DATA_COLLECTION_FREQUENCY = "data_collection_frequency_ms"
        private const val KEY_STORAGE_FREQUENCY = "storage_frequency_ms"

        // Default to 3 seconds per complete cycle
        const val DEFAULT_DATA_COLLECTION_FREQUENCY = 3000
        
        // Default to every 10 seconds for database writes
        const val DEFAULT_STORAGE_FREQUENCY = 10000
    }
}