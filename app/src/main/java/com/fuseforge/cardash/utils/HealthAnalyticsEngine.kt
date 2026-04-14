package com.fuseforge.cardash.utils

import com.fuseforge.cardash.data.db.VehicleHeartbeat
import kotlin.math.max

data class VehicleHealthVitals(
    val batteryHealthScore: Int, // 0-100
    val thermalConsistencyScore: Int, // 0-100
    val idlingFatiguePercentage: Int, // 0-100
    val totalActiveMinutes: Int,
    val voltageStressDetected: Boolean
)

object HealthAnalyticsEngine {

    /**
     * Deterministic math logic running locally on the Ledger.
     */
    fun computeVitals(heartbeats: List<VehicleHeartbeat>): VehicleHealthVitals {
        if (heartbeats.isEmpty()) {
            return VehicleHealthVitals(100, 100, 0, 0, false)
        }

        var totalActiveSeconds = 0
        var totalIdlingSeconds = 0
        var minVoltageSeen = 14.0f
        var voltageStressDetected = false

        // Thermal tracking
        var tempReadings = 0
        var tempsOver90 = 0

        for (hb in heartbeats) {
            totalActiveSeconds += hb.activeSeconds
            totalIdlingSeconds += hb.idlingSeconds
            
            val minV = hb.minBatteryVoltage ?: 14.0f
            if (minV < minVoltageSeen && minV > 5.0f) { // Ignore 0 or weird hardware outliers
                minVoltageSeen = minV
            }
            // If it dips below 11.5 during a pulse window, it's struggling
            if (minV < 11.5f && minV > 5.0f) {
                voltageStressDetected = true
            }

            val maxTemp = hb.maxCoolantTemp ?: 0
            if (maxTemp > 0) {
                tempReadings++
                if (maxTemp >= 90 && maxTemp <= 110) tempsOver90++
            }
        }

        val totalActiveMinutes = totalActiveSeconds / 60
        val idlingFatiguePercentage = if (totalActiveSeconds > 0) {
            (totalIdlingSeconds * 100) / totalActiveSeconds
        } else 0

        // Score 0-100 for battery based on lowest crank/sag voltage (12.5V is excellent, 10.5V is failing)
        val batteryHealthScore = max(0, minOf(100, ((minVoltageSeen - 10.5f) / (12.5f - 10.5f) * 100).toInt()))

        // Thermal consistency: what percentage of windows reached comfortable operating temp without overheating
        val thermalConsistencyScore = if (tempReadings > 0 && totalActiveMinutes > 5) {
            (tempsOver90 * 100) / tempReadings
        } else 100 // Default to 100 if trip is too short to reach operational temp

        return VehicleHealthVitals(
            batteryHealthScore = batteryHealthScore,
            thermalConsistencyScore = thermalConsistencyScore,
            idlingFatiguePercentage = minOf(100, idlingFatiguePercentage),
            totalActiveMinutes = totalActiveMinutes,
            voltageStressDetected = voltageStressDetected
        )
    }
}
