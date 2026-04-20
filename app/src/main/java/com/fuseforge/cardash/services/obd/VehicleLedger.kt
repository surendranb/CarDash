package com.fuseforge.cardash.services.obd

import android.util.Log
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.data.db.VehicleHeartbeat
import com.fuseforge.cardash.model.TelemetryStatus
import com.fuseforge.cardash.model.VehicleState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.util.*

/**
 * The Ledger is responsible for turning the 'Live Pulse' of the vehicle
 * into 'Ground Truth' history. It aggregates 1-second states into 1-minute blocks.
 */
class VehicleLedger(
    private val database: AppDatabase,
    private val telemetrist: Telemetrist,
    private val scope: CoroutineScope
) {
    private val TAG = "VehicleLedger"
    private var ledgerJob: Job? = null
    
    // Aggregation Buffer
    private var pointsInMinute = 0
    private var rpmSum = 0L
    private var loadSum = 0L
    private var minVoltage = 20f
    private var maxCoolant = 0
    private var lastMinuteMark = System.currentTimeMillis()

    fun start() {
        if (ledgerJob != null) return
        
        ledgerJob = scope.launch {
            Log.i(TAG, "History Ledger Active. Listening for vehicle pulses.")
            
            telemetrist.state.collect { state ->
                if (state.connectionStatus == TelemetryStatus.ACTIVE) {
                    aggregate(state)
                }
                
                // Deterministic Minute Check
                val now = System.currentTimeMillis()
                if (now - lastMinuteMark >= 60000) {
                    commitHeartbeat()
                    lastMinuteMark = now
                }
            }
        }
    }

    fun stop() {
        Log.i(TAG, "History Ledger Shutdown. Performing final flush.")
        ledgerJob?.cancel()
        ledgerJob = null
        
        // Final flush logic
        if (pointsInMinute > 0) {
            scope.launch(Dispatchers.IO) {
                commitHeartbeat()
            }
        }
    }

    private fun aggregate(state: VehicleState) {
        pointsInMinute++
        rpmSum += state.rpm
        loadSum += state.engineLoad
        if (state.batteryVoltage > 0) minVoltage = minOf(minVoltage, state.batteryVoltage)
        maxCoolant = maxOf(maxCoolant, state.coolantTemp)
    }

    private suspend fun commitHeartbeat() {
        if (pointsInMinute == 0) return
        
        val heartbeat = VehicleHeartbeat(
            timestamp = Date(),
            activeSeconds = pointsInMinute,
            avgRpm = (rpmSum / pointsInMinute).toInt(),
            avgEngineLoad = (loadSum / pointsInMinute).toInt(),
            minBatteryVoltage = if (minVoltage < 20) minVoltage else null,
            maxCoolantTemp = if (maxCoolant > 0) maxCoolant else null,
            tripId = telemetrist.state.value.sessionId
        )

        withContext(Dispatchers.IO) {
            try {
                database.obdLogDao().insertHeartbeat(heartbeat)
                Log.i(TAG, "Heartbeat Committed: ${pointsInMinute}s of activity recorded.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to commit heartbeat: ${e.message}")
            }
        }

        // Reset buffers
        pointsInMinute = 0
        rpmSum = 0
        loadSum = 0
        minVoltage = 20f
        maxCoolant = 0
    }
}
