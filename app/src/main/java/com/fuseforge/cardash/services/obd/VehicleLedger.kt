package com.fuseforge.cardash.services.obd

import android.util.Log
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.data.db.VehicleHeartbeat
import com.fuseforge.cardash.data.PreferencesManager
import com.fuseforge.cardash.model.TelemetryStatus
import com.fuseforge.cardash.model.VehicleState
import com.fuseforge.cardash.services.cloud.BigQuerySyncer
import kotlinx.coroutines.*
import java.util.*

/**
 * The Ledger is responsible for turning the 'Live Pulse' of the vehicle
 * into 'Ground Truth' history. It aggregates 1-second states into 1-minute blocks.
 */
class VehicleLedger(
    private val database: AppDatabase,
    private val telemetrist: Telemetrist,
    private val prefsManager: PreferencesManager,
    private val scope: CoroutineScope
) {
    private val TAG = "VehicleLedger"
    private var ledgerJob: Job? = null
    
    // Aggregation Buffers
    private var pointsInMinute = 0
    private var idlePointsInMinute = 0
    
    private var rpmSum = 0L
    private var loadSum = 0L
    private var speedSum = 0L
    private var throttleSum = 0L
    private var coolantMax = 0
    private var iatSum = 0L
    private var voltMin = 20f
    private var voltMax = 0f
    private var speedMax = 0
    private var rpmMax = 0
    private var fuelLevelLast: Int? = null
    private var baroPressureSum = 0L
    
    private var lastMinuteMark = System.currentTimeMillis()

    fun start() {
        if (ledgerJob != null) return
        
        lastMinuteMark = System.currentTimeMillis()
        pointsInMinute = 0
        idlePointsInMinute = 0
        
        ledgerJob = scope.launch {
            telemetrist.state.collect { state ->
                val isActive = state.connectionStatus == TelemetryStatus.ACTIVE
                val isRunning = state.isEngineRunning
                
                if (isActive && isRunning) {
                    aggregate(state)
                } else if (pointsInMinute > 0) {
                    commitHeartbeat()
                }
                
                val now = System.currentTimeMillis()
                if (now - lastMinuteMark >= 60000) {
                    commitHeartbeat()
                }
            }
        }
    }

    fun stop() {
        ledgerJob?.cancel()
        ledgerJob = null
        if (pointsInMinute > 0) {
            scope.launch(Dispatchers.IO) {
                commitHeartbeat()
            }
        }
    }

    private fun aggregate(state: VehicleState) {
        pointsInMinute++
        if (state.speedKph <= 0) {
            idlePointsInMinute++
        }
        rpmSum += state.rpm
        loadSum += state.engineLoad
        speedSum += state.speedKph
        throttleSum += state.throttlePos
        iatSum += state.intakeAirTemp
        baroPressureSum += state.baroPressure
        coolantMax = maxOf(coolantMax, state.coolantTemp)
        speedMax = maxOf(speedMax, state.speedKph)
        rpmMax = maxOf(rpmMax, state.rpm)
        if (state.batteryVoltage > 0) {
            voltMin = minOf(voltMin, state.batteryVoltage)
            voltMax = maxOf(voltMax, state.batteryVoltage)
        }
        fuelLevelLast = state.fuelLevel
    }

    private suspend fun commitHeartbeat() {
        if (pointsInMinute == 0) return
        
        val now = System.currentTimeMillis()
        val elapsedSeconds = ((now - lastMinuteMark) / 1000).coerceAtMost(60).coerceAtLeast(1).toInt()
        val idleRatio = idlePointsInMinute.toFloat() / pointsInMinute
        val idlingSeconds = (elapsedSeconds * idleRatio).toInt()

        val heartbeat = VehicleHeartbeat(
            tripId = telemetrist.state.value.sessionId,
            timestamp = Date(),
            activeSeconds = elapsedSeconds,
            idlingSeconds = idlingSeconds,
            avgRpm = (rpmSum / pointsInMinute).toInt(),
            avgEngineLoad = (loadSum / pointsInMinute).toInt(),
            avgSpeed = (speedSum / pointsInMinute).toInt(),
            avgThrottlePosition = (throttleSum / pointsInMinute).toInt(),
            avgIntakeAirTemp = (iatSum / pointsInMinute).toInt(),
            avgBatteryVoltage = if (voltMin < 20) (voltMin) else null, // Simplified
            minBatteryVoltage = if (voltMin < 20) (voltMin) else null,
            maxBatteryVoltage = if (voltMax > 0) (voltMax) else null,
            maxSpeed = if (speedMax > 0) speedMax else null,
            maxRpm = if (rpmMax > 0) rpmMax else null,
            maxCoolantTemp = if (coolantMax > 0) coolantMax else null,
            baroPressure = (baroPressureSum / pointsInMinute).toInt(),
            fuelLevel = fuelLevelLast
        )

        withContext(Dispatchers.IO) {
            try {
                database.obdLogDao().insertHeartbeat(heartbeat)
                Log.i(TAG, "Heartbeat Committed: ${elapsedSeconds}s active, ${idlingSeconds}s idling.")
                
                // Stream to BigQuery if configured
                val bqJson = prefsManager.getBqServiceAccountJson()
                if (bqJson.isNotBlank()) {
                    val datasetId = prefsManager.getBqDatasetId()
                    val tableId = prefsManager.getBqTableId()
                    val syncer = BigQuerySyncer(bqJson, datasetId, tableId)
                    // Fire and forget so we don't block ledger
                    launch(Dispatchers.IO) { syncer.streamRow(heartbeat) }
                }
                Unit
            } catch (e: Exception) {
                Log.e(TAG, "Failed to commit: ${e.message}")
            }
        }

        // Reset
        lastMinuteMark = now
        pointsInMinute = 0
        idlePointsInMinute = 0
        rpmSum = 0; loadSum = 0; speedSum = 0; throttleSum = 0; iatSum = 0; baroPressureSum = 0
        coolantMax = 0; voltMin = 20f; voltMax = 0f; speedMax = 0; rpmMax = 0
    }
}
