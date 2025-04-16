package com.example.cardash.utils

import android.content.Context
import com.example.cardash.data.db.AppDatabase
import com.example.cardash.data.db.OBDCombinedReading
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
 * Utility class to generate mock data for testing
 */
class MockDataGenerator(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.obdLogDao()
    
    /**
     * Generate mock data for testing
     * @param numReadings Number of readings to generate
     */
    suspend fun generateMockData(numReadings: Int = 25) {
        withContext(Dispatchers.IO) {
            // Create a mock session
            val sessionId = UUID.randomUUID().toString()
            val session = OBDSession(
                sessionId = sessionId,
                deviceAddress = "00:11:22:33:44:55",
                deviceName = "Mock OBD Adapter",
                startTime = Date(),
                isActive = true
            )
            
            dao.insertSession(session)
            
            // Generate diagnostic logs for the session
            generateMockLogs(sessionId, 10)
            
            // Generate combined readings
            generateMockReadings(sessionId, numReadings)
        }
    }
    
    private suspend fun generateMockLogs(sessionId: String, count: Int) {
        val calendar = Calendar.getInstance()
        
        // Add connection logs
        dao.insertLogEntry(
            OBDLogEntry(
                timestamp = calendar.time,
                command = "CONNECT",
                rawResponse = "Attempting connection to 00:11:22:33:44:55",
                parsedValue = "",
                dataType = OBDDataType.CONNECTION,
                sessionId = sessionId
            )
        )
        
        calendar.add(Calendar.SECOND, 1)
        
        dao.insertLogEntry(
            OBDLogEntry(
                timestamp = calendar.time,
                command = "SOCKET_CONNECT",
                rawResponse = "Socket connected successfully",
                parsedValue = "",
                dataType = OBDDataType.CONNECTION,
                sessionId = sessionId
            )
        )
        
        calendar.add(Calendar.SECOND, 1)
        
        // Add initialization logs
        dao.insertLogEntry(
            OBDLogEntry(
                timestamp = calendar.time,
                command = "ATZ",
                rawResponse = "ELM327 v1.5",
                parsedValue = "Reset OBD Interface",
                dataType = OBDDataType.INITIALIZATION,
                sessionId = sessionId
            )
        )
        
        calendar.add(Calendar.SECOND, 1)
        
        dao.insertLogEntry(
            OBDLogEntry(
                timestamp = calendar.time,
                command = "ATE0",
                rawResponse = "OK",
                parsedValue = "Echo Off",
                dataType = OBDDataType.INITIALIZATION,
                sessionId = sessionId
            )
        )
        
        calendar.add(Calendar.SECOND, 1)
        
        dao.insertLogEntry(
            OBDLogEntry(
                timestamp = calendar.time,
                command = "ATSP0",
                rawResponse = "OK",
                parsedValue = "Auto Protocol",
                dataType = OBDDataType.INITIALIZATION,
                sessionId = sessionId
            )
        )
        
        calendar.add(Calendar.SECOND, 1)
        
        // Add some command logs
        val commands = mapOf(
            "01 0C" to OBDDataType.RPM,
            "01 0D" to OBDDataType.SPEED,
            "01 04" to OBDDataType.ENGINE_LOAD,
            "01 05" to OBDDataType.COOLANT_TEMP,
            "01 2F" to OBDDataType.FUEL_LEVEL
        )
        
        for (i in 0 until count) {
            for ((command, dataType) in commands) {
                calendar.add(Calendar.SECOND, 1)
                
                val rawResponse = when (dataType) {
                    OBDDataType.RPM -> "41 0C 0B 1A"
                    OBDDataType.SPEED -> "41 0D 32"
                    OBDDataType.ENGINE_LOAD -> "41 04 3F"
                    OBDDataType.COOLANT_TEMP -> "41 05 5A"
                    OBDDataType.FUEL_LEVEL -> "41 2F C8"
                    else -> "41 XX YY"
                }
                
                val parsedValue = when (dataType) {
                    OBDDataType.RPM -> "712"
                    OBDDataType.SPEED -> "50"
                    OBDDataType.ENGINE_LOAD -> "25"
                    OBDDataType.COOLANT_TEMP -> "90"
                    OBDDataType.FUEL_LEVEL -> "78"
                    else -> "0"
                }
                
                dao.insertLogEntry(
                    OBDLogEntry(
                        timestamp = calendar.time,
                        command = command,
                        rawResponse = rawResponse,
                        parsedValue = parsedValue,
                        dataType = dataType,
                        sessionId = sessionId
                    )
                )
            }
        }
        
        // Add one error log
        calendar.add(Calendar.SECOND, 1)
        dao.insertLogEntry(
            OBDLogEntry(
                timestamp = calendar.time,
                command = "01 XX",
                rawResponse = "",
                parsedValue = "",
                dataType = OBDDataType.UNKNOWN,
                sessionId = sessionId,
                isError = true,
                errorMessage = "Command not supported"
            )
        )
    }
    
    private suspend fun generateMockReadings(sessionId: String, count: Int) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -count) // Start from [count] minutes ago
        val rand = Random(System.currentTimeMillis())
        
        // Generate more realistic data with patterns
        
        // Create some base pattern values
        var baseRpm = 800
        var baseSpeed = 0
        var baseEngineLoad = 15
        var baseCoolantTemp = 80
        var baseThrottlePosition = 5
        var baseFuelLevel = 80
        
        // For creating realistic patterns
        var tripPhase = 0 // 0: parked, 1: accelerating, 2: cruising, 3: decelerating
        var tripDuration = 0
        var maxTripDuration = rand.nextInt(10) + 5 // Random trip length
        
        for (i in 0 until count) {
            // Determine trip phase based on current position
            if (tripPhase == 0 && rand.nextInt(5) == 0 && i < count - maxTripDuration) {
                // Start a new trip
                tripPhase = 1
                tripDuration = 0
                maxTripDuration = rand.nextInt(10) + 5 // Random trip length
            } else if (tripPhase > 0) {
                tripDuration++
                
                if (tripDuration >= maxTripDuration) {
                    // End the trip
                    tripPhase = 0
                } else if (tripPhase == 1 && tripDuration > maxTripDuration / 3) {
                    // Transition to cruising
                    tripPhase = 2
                } else if (tripPhase == 2 && tripDuration > maxTripDuration * 2 / 3) {
                    // Transition to decelerating
                    tripPhase = 3
                }
            }
            
            // Set values based on the trip phase
            when (tripPhase) {
                0 -> { // Parked
                    baseRpm = if (rand.nextInt(10) < 8) 0 else 800 // Sometimes engine is running
                    baseSpeed = 0
                    baseEngineLoad = if (baseRpm > 0) 15 else 0
                    baseThrottlePosition = if (baseRpm > 0) 5 else 0
                }
                1 -> { // Accelerating
                    baseRpm = 1500 + (tripDuration * 200)
                    baseSpeed = tripDuration * 10
                    baseEngineLoad = 30 + (tripDuration * 5)
                    baseThrottlePosition = 50 - (tripDuration * 2)
                }
                2 -> { // Cruising
                    baseRpm = 2000 + rand.nextInt(500)
                    baseSpeed = 60 + rand.nextInt(20)
                    baseEngineLoad = 40 + rand.nextInt(10)
                    baseThrottlePosition = 25 + rand.nextInt(5)
                }
                3 -> { // Decelerating
                    baseRpm = 1500 - (tripDuration * 100)
                    baseSpeed = (60 + rand.nextInt(20)) - (tripDuration * 8)
                    baseEngineLoad = 20 - (tripDuration * 2)
                    baseThrottlePosition = 10 - (tripDuration)
                }
            }
            
            // Make sure values are valid
            baseRpm = baseRpm.coerceAtLeast(0)
            baseSpeed = baseSpeed.coerceAtLeast(0)
            baseEngineLoad = baseEngineLoad.coerceIn(0, 100)
            baseThrottlePosition = baseThrottlePosition.coerceIn(0, 100)
            
            // Add some randomness
            val rpm = (baseRpm + rand.nextInt(100) - 50).coerceAtLeast(0)
            val speed = (baseSpeed + rand.nextInt(6) - 3).coerceAtLeast(0)
            val engineLoad = (baseEngineLoad + rand.nextInt(5) - 2).coerceIn(0, 100)
            
            // Temperature increases slightly while driving
            if (tripPhase > 0) {
                baseCoolantTemp = (baseCoolantTemp + 0.2).coerceAtMost(95.0).toInt()
            } else {
                baseCoolantTemp = (baseCoolantTemp - 0.1).coerceAtLeast(80.0).toInt()
            }
            
            // Fuel decreases over time
            if (tripPhase > 0) {
                baseFuelLevel = (baseFuelLevel - 0.2).coerceAtLeast(0.0).toInt()
            }
            
            val coolantTemp = baseCoolantTemp + rand.nextInt(3) - 1
            val fuelLevel = baseFuelLevel + rand.nextInt(2) - 1
            
            // Create a reading
            val reading = OBDCombinedReading(
                timestamp = calendar.time,
                sessionId = sessionId,
                rpm = rpm,
                speed = speed,
                engineLoad = engineLoad,
                coolantTemp = coolantTemp,
                fuelLevel = fuelLevel,
                intakeAirTemp = 25 + rand.nextInt(5) + (if (tripPhase > 0) 5 else 0),
                throttlePosition = baseThrottlePosition + rand.nextInt(3) - 1,
                fuelPressure = if (rpm > 0) 350 + rand.nextInt(20) else 0,
                baroPressure = 101 + rand.nextInt(2) - 1,
                batteryVoltage = if (rpm > 0) 14.2f + (rand.nextFloat() * 0.4f - 0.2f) else 12.5f
            )
            
            dao.insertCombinedReading(reading)
            
            // Move time forward
            calendar.add(Calendar.MINUTE, 1)
        }
    }
}