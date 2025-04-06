package com.example.cardash.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.bluetooth.BluetoothDevice
import com.example.cardash.services.obd.ObdConnectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class MetricViewModel(
    private val obdConnectionService: ObdConnectionService
) : ViewModel() {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _rpm = MutableStateFlow(0)
    val rpm = _rpm.asStateFlow()
    
    private val _engineLoad = MutableStateFlow(0)
    val engineLoad = _engineLoad.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            _errorMessage.emit("Connecting to $deviceAddress...")
            
            when (val result = obdConnectionService.connect(deviceAddress)) {
                is ObdConnectionService.ConnectionResult.Success -> {
                    _connectionState.value = ConnectionState.Connected
                    _errorMessage.emit("Successfully connected to OBD2 adapter")
                    startRpmCollection()
                }
                is ObdConnectionService.ConnectionResult.Error -> {
                    _connectionState.value = ConnectionState.Failed(result.message)
                    _errorMessage.emit("Connection error: ${result.message}")
                }
            }
        }
    }

    private fun startRpmCollection() {
        viewModelScope.launch {
            while (_connectionState.value is ConnectionState.Connected) {
                try {
                    val response = obdConnectionService.sendCommand("01 0C")
                    _rpm.value = parseRpm(response)
                    delay(500) // Poll every 500ms
                } catch (e: Exception) {
                    _errorMessage.emit("RPM read error: ${e.message}")
                    _connectionState.value = ConnectionState.Failed("RPM monitoring failed")
                    disconnect()
                }
            }
        }
    }

    private fun parseRpm(response: String): Int {
        val values = response.split(" ")
        return try {
            if (values.size >= 2) {
                val byteA = values[1].toIntOrNull(16) ?: 0
                val byteB = if (values.size > 2) values[2].toIntOrNull(16) ?: 0 else 0
                ((byteA * 256) + byteB) / 4
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            obdConnectionService.disconnect()
            _connectionState.value = ConnectionState.Disconnected
            _rpm.value = 0
        }
    }

    suspend fun getPairedDevices(): Set<BluetoothDevice> {
        return withContext(Dispatchers.IO) {
            obdConnectionService.getPairedDevices()
        }
    }

    fun checkPermissions(): String? {
        return obdConnectionService.checkPermissions()
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        class Failed(val message: String) : ConnectionState()
    }
}
