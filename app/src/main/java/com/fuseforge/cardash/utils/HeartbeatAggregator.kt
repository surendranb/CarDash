package com.fuseforge.cardash.utils

import com.fuseforge.cardash.data.db.TripDataPoint
import com.fuseforge.cardash.data.db.VehicleHeartbeat
import java.util.Date
import kotlin.math.max
import kotlin.math.min

/**
 * Aggregates high-frequency TripDataPoints into a deterministic 1-minute VehicleHeartbeat.
 */
class HeartbeatAggregator(private val tripId: String) {
    
    private val buffer = mutableListOf<TripDataPoint>()
    private var lastMinuteTimestamp: Long = -1

    /**
     * Pushes a raw data point into the aggregator.
     * Returns a VehicleHeartbeat if a minute has passed, otherwise null.
     */
    fun push(dataPoint: TripDataPoint): VehicleHeartbeat? {
        val currentMinute = dataPoint.timestamp.time / 60000
        
        if (lastMinuteTimestamp == -1L) {
            lastMinuteTimestamp = currentMinute
        }

        if (currentMinute > lastMinuteTimestamp) {
            // Minute rolled over, process the buffer
            val heartbeat = processBuffer()
            buffer.clear()
            lastMinuteTimestamp = currentMinute
            buffer.add(dataPoint)
            return heartbeat
        }

        buffer.add(dataPoint)
        return null
    }

    /**
     * Force-processes the current buffer and returns a VehicleHeartbeat.
     * Use this when the session is ending to avoid losing the last minute of data.
     */
    fun flush(): VehicleHeartbeat? {
        if (buffer.isEmpty()) return null
        val heartbeat = processBuffer()
        buffer.clear()
        return heartbeat
    }

    private fun processBuffer(): VehicleHeartbeat {
        if (buffer.isEmpty()) return VehicleHeartbeat(tripId = tripId)

        var sumRpm = 0
        var maxRpm = 0
        var rpmCount = 0

        var sumSpeed = 0
        var maxSpeed = 0
        var speedCount = 0

        var sumLoad = 0
        var loadCount = 0

        var maxTemp = -273
        var tempCount = 0

        var minVoltage = 100f
        var maxVoltage = 0f
        var sumVoltage = 0f
        var voltageCount = 0

        var lastFuel: Int? = null
        var lastBaro: Int? = null
        var lastAmbient: Int? = null
        var lastMaf: Float? = null
        var lastIntake: Int? = null
        var lastThrottle: Int? = null
        
        var idlingSeconds = 0
        var activeSeconds = 0
        
        // We assume points are roughly every 2-5 seconds. 
        // For a more precise calculation, we could look at the time delta between points.
        val intervalEstimate = 5 // Average polling interval in seconds

        buffer.forEach { point ->
            // RPM
            point.rpm?.let {
                sumRpm += it
                maxRpm = max(maxRpm, it)
                rpmCount++
                if (it > 200) activeSeconds += intervalEstimate
                if (it > 200 && (point.speedObd ?: 0) == 0) idlingSeconds += intervalEstimate
            }

            // Speed
            point.speedObd?.let {
                sumSpeed += it
                maxSpeed = max(maxSpeed, it)
                speedCount++
            }

            // Load
            point.engineLoad?.let {
                sumLoad += it
                loadCount++
            }

            // Temp
            point.coolantTemp?.let {
                maxTemp = max(maxTemp, it)
                tempCount++
            }

            // Voltage
            point.batteryVoltage?.let {
                minVoltage = min(minVoltage, it)
                maxVoltage = max(maxVoltage, it)
                sumVoltage += it
                voltageCount++
            }

            // Snapshots (Take the last known value in the minute)
            point.fuelLevel?.let { lastFuel = it }
            point.baroPressure?.let { lastBaro = it }
            point.ambientAirTemp?.let { lastAmbient = it }
            point.maf?.let { lastMaf = it }
            point.intakeAirTemp?.let { lastIntake = it }
            point.throttlePosition?.let { lastThrottle = it }
        }

        return VehicleHeartbeat(
            tripId = tripId,
            timestamp = Date(lastMinuteTimestamp * 60000),
            avgRpm = if (rpmCount > 0) sumRpm / rpmCount else null,
            maxRpm = if (rpmCount > 0) maxRpm else null,
            avgSpeed = if (speedCount > 0) sumSpeed / speedCount else null,
            maxSpeed = if (speedCount > 0) maxSpeed else null,
            avgEngineLoad = if (loadCount > 0) sumLoad / loadCount else null,
            maxCoolantTemp = if (tempCount > 0) maxTemp else null,
            minBatteryVoltage = if (voltageCount > 0) minVoltage else null,
            maxBatteryVoltage = if (voltageCount > 0) maxVoltage else null,
            avgBatteryVoltage = if (voltageCount > 0) sumVoltage / voltageCount else null,
            fuelLevel = lastFuel,
            baroPressure = lastBaro,
            ambientAirTemp = lastAmbient,
            maf = lastMaf,
            avgIntakeAirTemp = lastIntake,
            avgThrottlePosition = lastThrottle,
            activeSeconds = min(60, activeSeconds),
            idlingSeconds = min(60, idlingSeconds)
        )
    }
}
