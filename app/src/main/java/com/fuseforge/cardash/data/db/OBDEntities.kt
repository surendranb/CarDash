package com.fuseforge.cardash.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "obd_logs")
@TypeConverters(DateConverter::class)
data class OBDLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Date = Date(),
    val command: String,
    val rawResponse: String,
    val parsedValue: String,
    val dataType: OBDDataType,
    val sessionId: String,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

// Projection class for fuel level readings (for Room query)
data class FuelLevelReading(
    val fuelLevel: Int,
    val timestamp: Date
)

enum class OBDDataType {
    RPM,
    SPEED,
    ENGINE_LOAD,
    COOLANT_TEMP,
    FUEL_LEVEL,
    INTAKE_AIR_TEMP,
    THROTTLE_POSITION,
    FUEL_PRESSURE,
    BARO_PRESSURE,
    BATTERY_VOLTAGE,
    MAF,
    AMBIENT_AIR_TEMP,
    CONNECTION,
    INITIALIZATION,
    UNKNOWN
}

@Entity(tableName = "obd_sessions")
@TypeConverters(DateConverter::class)
data class OBDSession(
    @PrimaryKey
    val sessionId: String,
    val deviceAddress: String,
    val deviceName: String?,
    val startTime: Date = Date(),
    val endTime: Date? = null,
    val isActive: Boolean = true,
    val vehicleInfo: String? = null
)

/**
 * This entity represents a single "row" of OBD data with all parameters collected at a specific time.
 * It's used for historical data display.
 */
@Entity(tableName = "obd_combined_readings")
@TypeConverters(DateConverter::class)
data class OBDCombinedReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Date = Date(),
    val sessionId: String,
    
    // Vehicle parameters
    val rpm: Int? = null,
    val speed: Int? = null,
    val engineLoad: Int? = null,
    val coolantTemp: Int? = null,
    val fuelLevel: Int? = null,
    val intakeAirTemp: Int? = null,
    val throttlePosition: Int? = null,
    val fuelPressure: Int? = null,
    val baroPressure: Int? = null,
    val batteryVoltage: Float? = null
)