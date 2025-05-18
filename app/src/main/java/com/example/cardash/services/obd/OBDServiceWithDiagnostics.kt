package com.example.cardash.services.obd

import android.content.Context
import com.example.cardash.data.db.OBDDataType as DbOBDDataType
import com.example.cardash.utils.OBDLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * This class wraps the OBDService and adds diagnostic logging capabilities
 */
class OBDServiceWithDiagnostics(
    private val obdService: OBDService,
    private val context: Context
) {
    
    // Provide context for database access
    fun getContext(): Context {
        return context
    }
    private val obdLogger = OBDLogger(context)
    private val ioScope = CoroutineScope(Dispatchers.IO)
    
    // Current session ID
    private var sessionId = "session_" + UUID.randomUUID().toString()
    
    // Start logging a new session
    fun startLoggingSession() {
        sessionId = "session_" + UUID.randomUUID().toString()
        ioScope.launch {
            obdLogger.logCommand(
                sessionId = sessionId,
                command = "SESSION_START",
                rawResponse = "New diagnostic session started",
                parsedValue = "",
                dataType = DbOBDDataType.CONNECTION
            )
        }
    }
    
    // Log a command and its response
    fun logCommand(command: String, response: String, parsedValue: String, dataType: DbOBDDataType) {
        ioScope.launch {
            obdLogger.logCommand(
                sessionId = sessionId,
                command = command,
                rawResponse = response,
                parsedValue = parsedValue,
                dataType = dataType
            )
        }
    }
    
    // Log an error
    fun logError(command: String, errorMessage: String, dataType: DbOBDDataType) {
        ioScope.launch {
            obdLogger.logError(
                sessionId = sessionId,
                command = command,
                errorMessage = errorMessage,
                dataType = dataType
            )
        }
    }
    
    // Get current session ID
    fun getSessionId(): String {
        return sessionId
    }
    
    // Get the wrapped OBDService
    fun getOBDService(): OBDService {
        return obdService
    }
    
    // Helper function to convert between OBD data types
    fun convertDataType(type: OBDDataType): DbOBDDataType {
        return when (type) {
            OBDDataType.RPM -> DbOBDDataType.RPM
            OBDDataType.SPEED -> DbOBDDataType.SPEED
            OBDDataType.ENGINE_LOAD -> DbOBDDataType.ENGINE_LOAD
            OBDDataType.COOLANT_TEMP -> DbOBDDataType.COOLANT_TEMP
            OBDDataType.FUEL_LEVEL -> DbOBDDataType.FUEL_LEVEL
            OBDDataType.INTAKE_AIR_TEMP -> DbOBDDataType.INTAKE_AIR_TEMP
            OBDDataType.THROTTLE_POSITION -> DbOBDDataType.THROTTLE_POSITION
            OBDDataType.FUEL_PRESSURE -> DbOBDDataType.FUEL_PRESSURE
            OBDDataType.BARO_PRESSURE -> DbOBDDataType.BARO_PRESSURE
            OBDDataType.BATTERY_VOLTAGE -> DbOBDDataType.BATTERY_VOLTAGE
            OBDDataType.MAF -> DbOBDDataType.MAF
            OBDDataType.AMBIENT_AIR_TEMP -> DbOBDDataType.AMBIENT_AIR_TEMP
            OBDDataType.CONNECTION -> DbOBDDataType.CONNECTION
            OBDDataType.INITIALIZATION -> DbOBDDataType.INITIALIZATION
            OBDDataType.UNKNOWN -> DbOBDDataType.UNKNOWN
        }
    }
}