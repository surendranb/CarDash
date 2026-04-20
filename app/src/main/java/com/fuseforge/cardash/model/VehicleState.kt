package com.fuseforge.cardash.model

import java.util.Date

/**
 * Immutable snapshot of the vehicle's telemetry state.
 * Represents the 'Ground Truth' for all observers.
 */
data class VehicleState(
    val sessionId: String = "",
    val timestamp: Date = Date(),
    val connectionStatus: TelemetryStatus = TelemetryStatus.DISCONNECTED,
    
    // Core Metrics (High-Speed Pulse)
    val rpm: Int = 0,
    val speedKph: Int = 0,
    val engineLoad: Int = 0,
    val throttlePos: Int = 0,

    // Environmental Metrics (Slow-Speed Pulse)
    val coolantTemp: Int = 0,
    val intakeAirTemp: Int = 0,
    val fuelLevel: Int = 0,
    val batteryVoltage: Float = 0f,
    val fuelPressure: Int = 0,
    val baroPressure: Int = 0,

    // Metadata
    val isEngineRunning: Boolean = false,
    val cycleCount: Long = 0
)

enum class TelemetryStatus {
    DISCONNECTED,
    CONNECTING,
    HANDSHAKING,
    ACTIVE,
    STALLED, // Connected but car is silent
    ERROR
}
