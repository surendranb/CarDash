package com.fuseforge.cardash.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

/**
 * The "Deterministic Ledger" entry. Precisely one row per 60 seconds of driving.
 * Contains averaged vitals and snapshots of all supported OBD parameters.
 */
@Entity(tableName = "vehicle_heartbeats")
@TypeConverters(DateConverter::class)
data class VehicleHeartbeat(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: String,
    val timestamp: Date = Date(),

    // Core Metrics (Averages over 60 seconds)
    val avgRpm: Int? = null,
    val maxRpm: Int? = null,
    val avgSpeed: Int? = null,
    val maxSpeed: Int? = null,
    val avgEngineLoad: Int? = null,
    val maxCoolantTemp: Int? = null,
    val avgThrottlePosition: Int? = null,
    val avgIntakeAirTemp: Int? = null,

    // Vital Signs (Snapshots/Extremes)
    val minBatteryVoltage: Float? = null,
    val maxBatteryVoltage: Float? = null,
    val avgBatteryVoltage: Float? = null,
    val fuelLevel: Int? = null,
    val baroPressure: Int? = null,
    val fuelPressure: Int? = null,
    val ambientAirTemp: Int? = null,
    val maf: Float? = null,

    // Derived City Metrics (Calculated by the aggregator)
    val activeSeconds: Int = 60, // How many seconds in this minute the pulse was active
    val idlingSeconds: Int = 0, // How many of those were at 0 speed
    val incidentCount: Int = 0  // Number of anomalies detected in this window
)
