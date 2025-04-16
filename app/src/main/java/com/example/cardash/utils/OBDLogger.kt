package com.example.cardash.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.cardash.data.db.AppDatabase
import com.example.cardash.data.db.OBDDataType
import com.example.cardash.data.db.OBDLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for logging OBD communications to both database and file
 */
class OBDLogger(private val context: Context) {
    private val TAG = "OBDLogger"
    private val dao = AppDatabase.getDatabase(context).obdLogDao()
    private val ioScope = CoroutineScope(Dispatchers.IO)
    
    // File for storing logs
    private val logFile: File by lazy {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val today = dateFormat.format(Date())
        val logDir = File(context.getExternalFilesDir(null), "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        File(logDir, "obd_log_$today.txt")
    }
    
    /**
     * Log a command and its response
     */
    fun logCommand(
        sessionId: String,
        command: String,
        rawResponse: String,
        parsedValue: String,
        dataType: OBDDataType
    ) {
        ioScope.launch {
            // Create log entry for database
            val logEntry = OBDLogEntry(
                timestamp = Date(),
                command = command,
                rawResponse = rawResponse,
                parsedValue = parsedValue,
                dataType = dataType,
                sessionId = sessionId,
                isError = false,
                errorMessage = null
            )
            
            // Insert into database
            dao.insertLogEntry(logEntry)
            
            // Also log to file
            writeToFile(formatLogEntry(logEntry))
            
            // Log to Android log for development
            Log.d(TAG, formatLogEntry(logEntry))
        }
    }
    
    /**
     * Log an error
     */
    fun logError(
        sessionId: String,
        command: String,
        errorMessage: String,
        dataType: OBDDataType
    ) {
        ioScope.launch {
            // Create log entry for database
            val logEntry = OBDLogEntry(
                timestamp = Date(),
                command = command,
                rawResponse = "",
                parsedValue = "",
                dataType = dataType,
                sessionId = sessionId,
                isError = true,
                errorMessage = errorMessage
            )
            
            // Insert into database
            dao.insertLogEntry(logEntry)
            
            // Also log to file
            writeToFile(formatLogEntry(logEntry))
            
            // Log to Android log for development
            Log.e(TAG, formatLogEntry(logEntry))
        }
    }
    
    /**
     * Format a log entry for file output
     */
    private fun formatLogEntry(entry: OBDLogEntry): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            .format(entry.timestamp)
        
        return if (entry.isError) {
            "[$timestamp] ERROR | ${entry.dataType} | ${entry.command} | ${entry.errorMessage}"
        } else {
            "[$timestamp] ${entry.dataType} | ${entry.command} | ${entry.rawResponse} | ${entry.parsedValue}"
        }
    }
    
    /**
     * Write log entry to file
     */
    private fun writeToFile(text: String) {
        try {
            FileWriter(logFile, true).use { writer ->
                writer.append(text).append("\n")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write to log file: ${e.message}")
        }
    }
    
    /**
     * Get the path to the current log file
     */
    fun getLogFilePath(): String {
        return logFile.absolutePath
    }
    
    /**
     * Clear log file
     */
    fun clearLogFile() {
        try {
            FileWriter(logFile, false).use { writer ->
                writer.write("")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to clear log file: ${e.message}")
        }
    }
}