package com.fuseforge.cardash.ui.diagnostics

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fuseforge.cardash.CarDashApp
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.data.db.OBDDataType
import com.fuseforge.cardash.data.db.OBDLogEntry
import com.fuseforge.cardash.data.db.OBDSession
import com.fuseforge.cardash.utils.MockDiagnosticGenerator
import com.fuseforge.cardash.utils.OBDLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DiagnosticViewModel(private val context: Context) : ViewModel() {
    private val dao = AppDatabase.getDatabase(context).obdLogDao()
    private val obdLogger = (context.applicationContext as CarDashApp).obdLogger
    
    // Selected session
    private val _selectedSession = MutableStateFlow<OBDSession?>(null)
    val selectedSession: StateFlow<OBDSession?> = _selectedSession
    
    // All sessions
    val sessions = dao.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Selected data types filter
    private val _selectedDataTypes = MutableStateFlow<Set<OBDDataType>>(OBDDataType.values().toSet())
    val selectedDataTypes: StateFlow<Set<OBDDataType>> = _selectedDataTypes
    
    // Show only errors filter
    private val _showOnlyErrors = MutableStateFlow(false)
    val showOnlyErrors: StateFlow<Boolean> = _showOnlyErrors
    
    // Get all logs based on session, data types, and error filter
    val logs = combine(
        dao.getAllSessions().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        ),
        _selectedSession,
        _selectedDataTypes,
        _showOnlyErrors
    ) { allSessions, selectedSession, dataTypes, onlyErrors ->
        // Start with all logs or session-specific logs
        val sessionId = selectedSession?.sessionId
        val baseFlow = if (sessionId != null) {
            // Specific session selected
            dao.getSessionLogs(sessionId).first()
        } else if (allSessions.isEmpty()) {
            // No sessions exist - get all logs regardless of session
            dao.getAllLogs().first()
        } else {
            // No specific session selected, but sessions exist
            allSessions.flatMap { session ->
                dao.getSessionLogs(session.sessionId).first()
            }
        }
        
        // Apply filters
        baseFlow.filter { log ->
            (dataTypes.contains(log.dataType) || log.dataType == OBDDataType.UNKNOWN) &&
            (!onlyErrors || log.isError)
        }.sortedByDescending { it.timestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Session selection
    fun selectSession(session: OBDSession?) {
        _selectedSession.value = session
    }
    
    // Toggle data type filter
    fun toggleDataTypeFilter(dataType: OBDDataType, selected: Boolean) {
        val current = _selectedDataTypes.value.toMutableSet()
        if (selected) {
            current.add(dataType)
        } else {
            current.remove(dataType)
        }
        _selectedDataTypes.value = current
    }
    
    // Toggle show only errors
    fun toggleShowOnlyErrors(showOnly: Boolean) {
        _showOnlyErrors.value = showOnly
    }
    
    // Export logs to file and share
    fun exportLogs() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Share the existing log file
                    val logFile = File(obdLogger.getLogFilePath())
                    
                    if (logFile.exists()) {
                        val fileUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            logFile
                        )
                        
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        
                        val chooserIntent = Intent.createChooser(shareIntent, "Share Log File")
                        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooserIntent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    // Clear logs
    fun clearLogs() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                obdLogger.clearLogFile()
            }
        }
    }
    
    // Get log file path
    fun getLogFilePath(): String {
        return obdLogger.getLogFilePath()
    }
    
    // Format timestamp
    fun formatTimestamp(date: Date): String {
        val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return formatter.format(date)
    }
    
    // Generate test data
    fun generateTestData() {
        viewModelScope.launch(Dispatchers.IO) {
            val mockGenerator = MockDiagnosticGenerator(context)
            mockGenerator.generateMockDiagnosticLogs(50)
        }
    }
}

/**
 * Factory for creating DiagnosticViewModel with application context
 */
class DiagnosticViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DiagnosticViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DiagnosticViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}