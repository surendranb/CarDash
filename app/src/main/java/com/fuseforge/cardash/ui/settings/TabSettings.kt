package com.fuseforge.cardash.ui.settings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// DataStore for storing user preferences
val Context.dataStore by preferencesDataStore(name = "settings")

class TabSettings : ViewModel() {
    // Tab visibility settings with default values
    var showMetricsTab by mutableStateOf(true) // Always on by default
    var showHistoryTab by mutableStateOf(true)
    var showDiagnosticsTab by mutableStateOf(true)
    var showGraphsTab by mutableStateOf(true)
    
    // Preference keys
    companion object {
        val METRICS_TAB = booleanPreferencesKey("show_metrics_tab")
        val HISTORY_TAB = booleanPreferencesKey("show_history_tab")
        val DIAGNOSTICS_TAB = booleanPreferencesKey("show_diagnostics_tab")
        val GRAPHS_TAB = booleanPreferencesKey("show_graphs_tab")
    }
    
    // Load settings from DataStore
    fun loadSettings(context: Context) {
        viewModelScope.launch {
            context.dataStore.data.map { preferences ->
                // Always keep metrics tab visible
                showMetricsTab = true
                
                // Load other tab preferences with defaults if not set
                showHistoryTab = preferences[HISTORY_TAB] ?: true
                showDiagnosticsTab = preferences[DIAGNOSTICS_TAB] ?: true
                showGraphsTab = preferences[GRAPHS_TAB] ?: true
            }.collect { /* Just collect to trigger the map */ }
        }
    }
    
    // Save settings to DataStore
    fun saveSettings(context: Context) {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                // Always keep metrics tab visible
                preferences[METRICS_TAB] = true
                
                // Save other tab preferences
                preferences[HISTORY_TAB] = showHistoryTab
                preferences[DIAGNOSTICS_TAB] = showDiagnosticsTab
                preferences[GRAPHS_TAB] = showGraphsTab
            }
        }
    }
    
    // Toggle a specific tab's visibility
    fun toggleTab(tabType: TabType, context: Context) {
        when (tabType) {
            TabType.METRICS -> {
                // Metrics tab cannot be turned off
                showMetricsTab = true
            }
            TabType.HISTORY -> {
                showHistoryTab = !showHistoryTab
            }
            TabType.DIAGNOSTICS -> {
                showDiagnosticsTab = !showDiagnosticsTab
            }
            TabType.GRAPHS -> {
                showGraphsTab = !showGraphsTab
            }
        }
        
        // Save the changes
        saveSettings(context)
    }
}

// Tab types for easy reference
enum class TabType {
    METRICS, HISTORY, DIAGNOSTICS, GRAPHS
}