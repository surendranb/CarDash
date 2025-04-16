package com.example.cardash.services.obd

import android.content.Context
import com.example.cardash.data.db.OBDDataType
import com.example.cardash.utils.OBDLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extension functions to add logging capability to OBDService
 */
class OBDServiceLogger(
    private val context: Context,
    private val obdService: OBDService
) {
    private val obdLogger = OBDLogger(context)

    /**
     * Log a command and its response
     */
    suspend fun logCommand(
        sessionId: String,
        command: String,
        rawResponse: String,
        parsedValue: String,
        dataType: OBDDataType
    ) {
        obdLogger.logCommand(sessionId, command, rawResponse, parsedValue, dataType)
    }

    /**
     * Log an error
     */
    suspend fun logError(
        sessionId: String,
        command: String,
        errorMessage: String,
        dataType: OBDDataType
    ) {
        obdLogger.logError(sessionId, command, errorMessage, dataType)
    }
}

// Extension function for OBDService to get current session ID
suspend fun OBDService.getCurrentSessionId(): String {
    // Get or create a session ID
    return "session_" + System.currentTimeMillis()
}
