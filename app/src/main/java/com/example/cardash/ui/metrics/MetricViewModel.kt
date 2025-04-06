package com.example.cardash.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cardash.services.obd.OBDService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MetricViewModel(
    private val obdService: OBDService
) : ViewModel() {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _rpm = MutableStateFlow(0)
    val rpm = _rpm.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            _errorMessage.emit("Connecting to $deviceAddress...")
            
            try {
                if (obdService.connect(deviceAddress)) {
                    _connectionState.value = ConnectionState.Connected
                    _errorMessage.emit("Connected to OBD2 adapter")
                    startRpmCollection()
                } else {
                    _connectionState.value = ConnectionState.Failed("Connection failed") 
                    _errorMessage.emit("Failed to connect - check adapter")
                }
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Failed(e.message ?: "Unknown error")
                _errorMessage.emit("Error: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private fun startRpmCollection() {
        println("MV: Starting RPM collection")
        viewModelScope.launch {
            obdService.rpmFlow
                .catch { e -> 
                    _errorMessage.emit(e.message ?: "RPM read error")
                    _connectionState.value = ConnectionState.Failed("RPM monitoring failed")
                }
                .collect { rpmValue ->
                    _rpm.value = rpmValue
                }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            obdService.disconnect()
            _connectionState.value = ConnectionState.Disconnected
            _rpm.value = 0
        }
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        class Failed(val message: String) : ConnectionState()
    }
}
