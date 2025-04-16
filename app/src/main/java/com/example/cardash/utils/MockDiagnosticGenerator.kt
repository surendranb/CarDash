package com.example.cardash.utils

import android.content.Context
import com.example.cardash.data.db.AppDatabase
import com.example.cardash.data.db.OBDDataType
import com.example.cardash.data.db.OBDLogEntry
import com.example.cardash.data.db.OBDSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.UUID
import kotlin.random.Random

/**
 * Generator for mock diagnostic data
 */
class MockDiagnosticGenerator(private val context: Context) {
    
    /**
     * Generate mock diagnostic logs for testing
     */
    suspend fun generateMockDiagnosticLogs(count: Int = 50) = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val dao = database.obdLogDao()
        
        // Create a session ID
        val sessionId = UUID.randomUUID().toString()
        
        // Create mock session
        val session = OBDSession(
            sessionId = sessionId,
            deviceAddress = "00:11:22:33:44:55",
            deviceName = "Mock OBD Adapter",
            startTime = Date(),
            isActive = true
        )
        
        // Insert session
        dao.insertSession(session)
        
        // Define commands
        val commands = mapOf(
            "01 0C" to OBDDataType.RPM,
            "01 0D" to OBDDataType.SPEED,
            "01 04" to OBDDataType.ENGINE_LOAD,
            "01 05" to OBDDataType.COOLANT_TEMP,
            "01 2F" to OBDDataType.FUEL_LEVEL,
            "01 0F" to OBDDataType.INTAKE_AIR_TEMP,
            "01 11" to OBDDataType.THROTTLE_POSITION,
            "01 0A" to OBDDataType.FUEL_PRESSURE,
            "01 33" to OBDDataType.BARO_PRESSURE,
            "01 42" to OBDDataType.BATTERY_VOLTAGE
        )
        
        // Create log entries with occasional errors
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -count)
        val rand = Random(System.currentTimeMillis())
        
        // First add initialization logs
        val initCommands = listOf(
            "ATZ" to "ELM327 v1.5",
            "ATE0" to "OK",
            "ATL0" to "OK",
            "ATSP0" to "OK"
        )
        
        for ((command, response) in initCommands) {
            dao.insertLogEntry(
                OBDLogEntry(
                    timestamp = calendar.time,
                    command = command,
                    rawResponse = response,
                    parsedValue = "Initialization command",
                    dataType = OBDDataType.INITIALIZATION,
                    sessionId = sessionId
                )
            )
            calendar.add(Calendar.SECOND, 1)
        }
        
        // Now add data logs
        for (i in 0 until count) {
            for ((command, dataType) in commands) {
                // Occasional error (1 in 20 chance)
                val isError = rand.nextInt(20) == 0
                
                if (isError) {
                    // Create error log
                    dao.insertLogEntry(
                        OBDLogEntry(
                            timestamp = calendar.time,
                            command = command,
                            rawResponse = "",
                            parsedValue = "",
                            dataType = dataType,
                            sessionId = sessionId,
                            isError = true,
                            errorMessage = getRandomError()
                        )
                    )
                } else {
                    // Create success log
                    val (response, parsedValue) = getMockResponse(command, dataType)
                    dao.insertLogEntry(
                        OBDLogEntry(
                            timestamp = calendar.time,
                            command = command,
                            rawResponse = response,
                            parsedValue = parsedValue,
                            dataType = dataType,
                            sessionId = sessionId
                        )
                    )
                }
                
                calendar.add(Calendar.SECOND, 1)
            }
            
            // Move forward a bit more between command sets
            calendar.add(Calendar.SECOND, 5)
        }
    }

    private fun getMockResponse(command: String, dataType: OBDDataType): Pair<String, String> {
        val rand = Random.Default
        
        return when (dataType) {
            OBDDataType.RPM -> {
                val rpm = 800 + rand.nextInt(3000)
                val hexA = (rpm * 4) / 256
                val hexB = (rpm * 4) % 256
                "41 0C ${hexA.toString(16).padStart(2, '0')} ${hexB.toString(16).padStart(2, '0')}" to rpm.toString()
            }
            OBDDataType.SPEED -> {
                val speed = rand.nextInt(100)
                "41 0D ${speed.toString(16).padStart(2, '0')}" to speed.toString()
            }
            OBDDataType.ENGINE_LOAD -> {
                val load = rand.nextInt(100)
                val hex = (load * 255) / 100
                "41 04 ${hex.toString(16).padStart(2, '0')}" to "$load%"
            }
            OBDDataType.COOLANT_TEMP -> {
                val temp = 80 + rand.nextInt(20)
                val hex = temp + 40
                "41 05 ${hex.toString(16).padStart(2, '0')}" to "${temp}°C"
            }
            OBDDataType.FUEL_LEVEL -> {
                val level = 50 + rand.nextInt(50)
                val hex = (level * 255) / 100
                "41 2F ${hex.toString(16).padStart(2, '0')}" to "$level%"
            }
            OBDDataType.INTAKE_AIR_TEMP -> {
                val temp = 20 + rand.nextInt(20)
                val hex = temp + 40
                "41 0F ${hex.toString(16).padStart(2, '0')}" to "${temp}°C"
            }
            OBDDataType.THROTTLE_POSITION -> {
                val pos = rand.nextInt(100)
                val hex = (pos * 255) / 100
                "41 11 ${hex.toString(16).padStart(2, '0')}" to "$pos%"
            }
            OBDDataType.FUEL_PRESSURE -> {
                val pressure = 300 + rand.nextInt(100)
                val hex = pressure / 3
                "41 0A ${hex.toString(16).padStart(2, '0')}" to "${pressure}kPa"
            }
            OBDDataType.BARO_PRESSURE -> {
                val pressure = 98 + rand.nextInt(6)
                "41 33 ${pressure.toString(16).padStart(2, '0')}" to "${pressure}kPa"
            }
            OBDDataType.BATTERY_VOLTAGE -> {
                val voltage = 12.0 + rand.nextDouble(3.0)
                val hex = (voltage * 10).toInt()
                "41 42 ${hex.toString(16).padStart(2, '0')}" to String.format("%.1fV", voltage)
            }
            else -> {
                "NO DATA" to ""
            }
        }
    }

    private fun getRandomError(): String {
        return listOf(
            "NO DATA",
            "UNABLE TO CONNECT",
            "BUS INIT ERROR",
            "CAN ERROR",
            "BUFFER FULL",
            "TIMEOUT",
            "READ ERROR",
            "WRITE ERROR"
        ).random()
    }
}