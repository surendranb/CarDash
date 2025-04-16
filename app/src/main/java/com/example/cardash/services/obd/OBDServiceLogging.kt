package com.example.cardash.services.obd

import android.content.Context
import com.example.cardash.data.db.OBDDataType as DbOBDDataType
import com.example.cardash.utils.OBDLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Class that wraps OBDService and adds logging functionality
 */
class OBDServiceLogging(
    private val obdService: OBDService,
    context: Context
) {
    private val obdLogger = OBDLogger(context)
    
    // Reference to the services input/output streams for logging
    private val inputStream: InputStream? get() = obdService.inputStream
    private val outputStream: OutputStream? get() = obdService.outputStream
    
    // Current session ID for logging
    private var sessionId = "session_" + UUID.randomUUID().toString()
    
    /**
     * Send a command and log the results
     */
    suspend fun sendCommand(command: String, dataType: OBDDataType = OBDDataType.UNKNOWN): String {
        return withContext(Dispatchers.IO) {
            try {
                // Send command using the original OBD service (which now uses command queue)
                val response = obdService.sendCommand(command)
                
                // Log successful command
                obdLogger.logCommand(
                    sessionId = sessionId,
                    command = command,
                    rawResponse = response,
                    parsedValue = "",
                    dataType = convertDataType(dataType)
                )
                
                response
            } catch (e: Exception) {
                // Log error
                obdLogger.logError(
                    sessionId = sessionId,
                    command = command,
                    errorMessage = "Command failed: ${e.message}",
                    dataType = convertDataType(dataType)
                )
                throw e
            }
        }
    }
    
    /**
     * Get vehicle speed with logging
     */
    suspend fun getSpeed(): Int {
        try {
            // Use the standard command from OBDService but we'll log the command and response
            val command = OBDService.SPEED_COMMAND
            val response = sendCommand(command, OBDDataType.SPEED)
            
            // Use the parsing function from OBDService
            val speed = obdService.parseSpeedResponse(response)
            
            // Update log with the parsed value
            obdLogger.logCommand(
                sessionId = sessionId,
                command = command,
                rawResponse = response,
                parsedValue = speed.toString(),
                dataType = DbOBDDataType.SPEED
            )
            
            return speed
        } catch (e: Exception) {
            obdLogger.logError(
                sessionId = sessionId,
                command = OBDService.SPEED_COMMAND,
                errorMessage = "Failed to get speed: ${e.message}",
                dataType = DbOBDDataType.SPEED
            )
            throw e
        }
    }
    
    // Helper function to convert between OBD data types
    private fun convertDataType(type: OBDDataType): DbOBDDataType {
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
            OBDDataType.CONNECTION -> DbOBDDataType.CONNECTION
            OBDDataType.INITIALIZATION -> DbOBDDataType.INITIALIZATION
            OBDDataType.UNKNOWN -> DbOBDDataType.UNKNOWN
        }
    }
    
    // Additional methods for other OBD commands would follow the same pattern
}