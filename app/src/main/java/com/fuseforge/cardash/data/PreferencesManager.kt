package com.fuseforge.cardash.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.content.SharedPreferences

// Extension property for Context to access the DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private const val PREFS_NAME = "CarDashPrefs"
// Add keys for your preferences here
private const val KEY_LAST_CONNECTED_DEVICE_ADDRESS = "last_connected_device_address"
private const val KEY_VEHICLE_PROFILE = "vehicle_profile"
private const val KEY_GEMINI_API_KEY = "gemini_api_key"
private const val KEY_GEMINI_MODEL_NAME = "gemini_model_name"
private const val KEY_AI_INSIGHTS_ENABLED = "ai_insights_enabled"
private const val KEY_ODOMETER_OFFSET = "odometer_offset"

class PreferencesManager(private val context: Context) {
    
    // Define preference keys
    companion object {
        val LOGGING_ENABLED = booleanPreferencesKey("logging_enabled")
        val LOG_RETENTION_DAYS = intPreferencesKey("log_retention_days")
        val DIAGNOSTIC_MODE = booleanPreferencesKey("diagnostic_mode")
        val DATA_COLLECTION_FREQUENCY = intPreferencesKey("data_collection_frequency")
        val STORAGE_FREQUENCY = intPreferencesKey("storage_frequency")
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Get logging enabled preference
    val loggingEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[LOGGING_ENABLED] ?: true // Default to true
        }
        
    // Get log retention days preference
    val logRetentionDays: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[LOG_RETENTION_DAYS] ?: 7 // Default to 7 days
        }
        
    // Get diagnostic mode preference
    val diagnosticMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DIAGNOSTIC_MODE] ?: false // Default to false
        }
    
    // Update logging enabled preference
    suspend fun updateLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOGGING_ENABLED] = enabled
        }
    }
    
    // Update log retention days preference
    suspend fun updateLogRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[LOG_RETENTION_DAYS] = days
        }
    }
    
    // Update diagnostic mode preference
    suspend fun updateDiagnosticMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DIAGNOSTIC_MODE] = enabled
        }
    }

    fun getLastConnectedDeviceAddress(): String? {
        return preferences.getString(KEY_LAST_CONNECTED_DEVICE_ADDRESS, null)
    }

    fun saveLastConnectedDeviceAddress(address: String?) {
        val editor = preferences.edit()
        if (address == null) {
            editor.remove(KEY_LAST_CONNECTED_DEVICE_ADDRESS)
        } else {
            editor.putString(KEY_LAST_CONNECTED_DEVICE_ADDRESS, address)
        }
        editor.apply()
    }

    fun updateDataCollectionFrequency(frequencyMs: Int) {
        preferences.edit().putInt("data_collection_frequency", frequencyMs).apply()
    }

    fun updateStorageFrequency(frequencyMs: Int) {
        preferences.edit().putInt("storage_frequency", frequencyMs).apply()
    }

    fun setVerboseLoggingEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("logging_enabled", enabled).apply()
    }

    fun isVerboseLoggingEnabled(): Boolean {
        return preferences.getBoolean("logging_enabled", true)
    }
    
    fun getDataCollectionFrequency(): Long {
        return preferences.getInt("data_collection_frequency", 500).toLong()
    }

    fun getStorageFrequency(): Long {
        return preferences.getInt("storage_frequency", 5000).toLong()
    }

    fun getVehicleProfile(): String {
        return preferences.getString(KEY_VEHICLE_PROFILE, "") ?: ""
    }

    fun setVehicleProfile(profile: String) {
        preferences.edit().putString(KEY_VEHICLE_PROFILE, profile).apply()
    }

    fun isAiInsightsEnabled(): Boolean {
        // Defaults to true, user can turn it off
        return preferences.getBoolean(KEY_AI_INSIGHTS_ENABLED, true)
    }

    fun setAiInsightsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AI_INSIGHTS_ENABLED, enabled).apply()
    }

    fun getGeminiApiKey(): String {
        return preferences.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    fun setGeminiApiKey(key: String) {
        preferences.edit().putString(KEY_GEMINI_API_KEY, key).apply()
    }

    fun getGeminiModelName(): String {
        return preferences.getString(KEY_GEMINI_MODEL_NAME, "gemini-1.5-pro-latest") ?: "gemini-1.5-pro-latest"
    }

    fun setGeminiModelName(model: String) {
        preferences.edit().putString(KEY_GEMINI_MODEL_NAME, model).apply()
    }

    fun getOdometerOffset(): Int {
        return preferences.getInt(KEY_ODOMETER_OFFSET, 0)
    }

    fun setOdometerOffset(km: Int) {
        preferences.edit().putInt(KEY_ODOMETER_OFFSET, km).apply()
    }

    // Add other preference methods below as needed
    fun clearAllPreferences() {
        val editor = preferences.edit()
        editor.clear()
        editor.apply()
        println("PreferencesManager: All preferences cleared.")
    }
}