package com.fuseforge.cardash.services.obd

import android.util.Log
import com.fuseforge.cardash.model.TelemetryStatus
import com.fuseforge.cardash.model.VehicleState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * The Telemetrist orchestrates the entire Car-to-State reactor.
 * Minimalist, high-reliability design focusing on HUD responsiveness.
 */
class Telemetrist(
    private val bluetoothManager: BluetoothManager,
    private val externalScope: CoroutineScope
) {
    private val TAG = "Telemetrist"
    
    private val _state = MutableStateFlow(VehicleState())
    val state = _state.asStateFlow()

    private var orchestratorJob: Job? = null
    private var activeLink: OBDLink? = null
    private var isScanning = false
    private var ancillaryIndex = 0

    // High-priority core metrics (every cycle)
    private val corePids = listOf(
        ObdProtocol.RPM to { s: VehicleState, r: String -> s.copy(rpm = ObdProtocol.parseRpm(r) ?: s.rpm) },
        ObdProtocol.SPEED to { s: VehicleState, r: String -> s.copy(speedKph = ObdProtocol.parseSpeed(r) ?: s.speedKph) },
        ObdProtocol.ENGINE_LOAD to { s: VehicleState, r: String -> s.copy(engineLoad = ObdProtocol.parseEngineLoad(r) ?: s.engineLoad) },
        ObdProtocol.THROTTLE_POS to { s: VehicleState, r: String -> s.copy(throttlePos = ObdProtocol.parseGenericPercentage(r, "41 11") ?: s.throttlePos) }
    )

    // Low-priority snapshots (interleaved, 1 per cycle)
    private val ancillaryPids = listOf(
        ObdProtocol.COOLANT_TEMP to { s: VehicleState, r: String -> s.copy(coolantTemp = ObdProtocol.parseCoolantTemp(r) ?: s.coolantTemp) },
        ObdProtocol.BATTERY_VOLT to { s: VehicleState, r: String -> s.copy(batteryVoltage = ObdProtocol.parseVoltage(r) ?: s.batteryVoltage) },
        ObdProtocol.FUEL_LEVEL to { s: VehicleState, r: String -> s.copy(fuelLevel = ObdProtocol.parseGenericPercentage(r, "41 2F") ?: s.fuelLevel) },
        ObdProtocol.BARO_PRESS to { s: VehicleState, r: String -> s.copy(baroPressure = ObdProtocol.parseGenericPercentage(r, "41 33") ?: s.baroPressure) },
        ObdProtocol.INTAKE_AIR_TEMP to { s: VehicleState, r: String -> s.copy(intakeAirTemp = ObdProtocol.parseCoolantTemp(r) ?: s.intakeAirTemp) }
    )

    fun start(deviceAddress: String) {
        if (orchestratorJob != null) return
        orchestratorJob = externalScope.launch {
            while (coroutineContext.isActive) {
                try {
                    activeLink = establishLink(deviceAddress)
                    runTelemetryLoop(activeLink!!)
                } catch (e: Exception) {
                    Log.e(TAG, "Reactor Restarting: ${e.message}")
                    updateStatus(TelemetryStatus.ERROR)
                    activeLink?.close()
                    delay(5000)
                }
            }
        }
    }

    fun stop() {
        orchestratorJob?.cancel()
        orchestratorJob = null
        activeLink?.close()
        activeLink = null
        updateStatus(TelemetryStatus.DISCONNECTED)
    }

    private suspend fun establishLink(address: String): OBDLink {
        updateStatus(TelemetryStatus.CONNECTING)
        val socket = bluetoothManager.createSocket(address) ?: throw Exception("Socket Failed")
        withContext(Dispatchers.IO) { socket.connect() }
        
        val link = OBDLink(socket)
        updateStatus(TelemetryStatus.HANDSHAKING)
        link.query(ObdProtocol.RESET, 2500)
        link.query(ObdProtocol.ECHO_OFF)
        link.query(ObdProtocol.PROTOCOL_AUTO, 3000)
        return link
    }

    private suspend fun runTelemetryLoop(link: OBDLink) {
        updateStatus(TelemetryStatus.ACTIVE)
        while (link.isConnected()) {
            yield() // Check for cancellation
            val startTime = System.currentTimeMillis()
            var newState = _state.value.copy(timestamp = Date())

            try {
                if (isScanning) { delay(500); continue }

                // 1. Core Update
                corePids.forEach { (pid, mapper) -> newState = mapper(newState, link.query(pid)) }

                // 2. Interleaved Detail (One per second)
                val (pid, mapper) = ancillaryPids[ancillaryIndex]
                newState = mapper(newState, link.query(pid))
                ancillaryIndex = (ancillaryIndex + 1) % ancillaryPids.size

                newState = newState.copy(
                    isEngineRunning = newState.rpm > 200,
                    connectionStatus = TelemetryStatus.ACTIVE,
                    cycleCount = newState.cycleCount + 1
                )
                _state.emit(newState)

            } catch (e: Exception) {
                if (!link.isConnected()) throw e
                _state.emit(newState.copy(connectionStatus = TelemetryStatus.STALLED))
            }

            delay((2500L - (System.currentTimeMillis() - startTime)).coerceAtLeast(50))
        }
    }

    suspend fun scanTroubleCodes(): List<String> {
        val link = activeLink ?: return emptyList()
        isScanning = true
        return try {
            link.query("03", 5000).split("\r").filter { it.startsWith("43") }.map { it.trim() }
        } finally { isScanning = false }
    }

    private fun updateStatus(status: TelemetryStatus) {
        _state.value = _state.value.copy(connectionStatus = status)
    }
}
