package com.example.cardash.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property for Context to access the DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    
    // Define preference keys
    companion object {
        val LOGGING_ENABLED = booleanPreferencesKey("logging_enabled")
        val LOG_RETENTION_DAYS = intPreferencesKey("log_retention_days")
        val DIAGNOSTIC_MODE = booleanPreferencesKey("diagnostic_mode")
    }
    
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
}