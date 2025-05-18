package com.fuseforge.cardash.services.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.model.CarText
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.fuseforge.cardash.CarDashApp
import com.fuseforge.cardash.services.obd.OBDService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale
import android.content.Intent
import android.os.Build
import com.fuseforge.cardash.services.CarDashDataCollectorService
import com.fuseforge.cardash.data.PreferencesManager
import com.fuseforge.cardash.services.obd.ConnectionStatus
import android.content.ComponentName
import androidx.car.app.model.Toggle
import com.github.eltonvs.obd.command.ObdCommand
import com.github.eltonvs.obd.command.ObdResponse
import com.github.eltonvs.obd.command.control.TroubleCodesCommand
import com.github.eltonvs.obd.command.fuel.FuelLevelCommand

class MetricsCarScreen(carContext: CarContext) : Screen(carContext) {

    private var obdService: OBDService? = null
    private var preferencesManager: PreferencesManager? = null
    private var metricsUpdateJob: Job? = null
    private var isServiceAvailable: Boolean = false

    private var speed: String = "--"
    private var rpm: String = "--"
    private var coolantTemp: String = "--"
    private var engineLoad: String = "--"
    private var fuelLevel: String = "--"
    private var throttlePos: String = "--"
    private var intakeAirTemp: String = "--"
    private var fuelPressure: String = "--"
    private var baroPressure: String = "--"
    private var moduleVoltage: String = "--"

    init {
        //val app = carContext.applicationContext as CarDashApp
        //obdService = app.obdService // REMOVE from init
        //preferencesManager = app.preferencesManager // REMOVE from init

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)

                // Fetch services here, as they might be initialized lazily by CarDashApp
                val app = carContext.applicationContext as CarDashApp
                obdService = app.obdServiceDiagnostics.getOBDService()
                preferencesManager = app.preferencesManager

                if (obdService == null) {
                    println("MetricsCarScreen: OBDService (from Diagnostics) is NULL in onStart. Cannot update metrics.")
                    isServiceAvailable = false
                    invalidate()
                    return
                }
                isServiceAvailable = true

                if (preferencesManager == null) {
                    println("MetricsCarScreen: PreferencesManager is NULL in onStart. Cannot check last device.")
                }

                obdService?.let { service ->
                    val lastDeviceAddress = preferencesManager?.getLastConnectedDeviceAddress()
                    if (service.connectionStatus.value != ConnectionStatus.CONNECTED && 
                        service.connectionStatus.value != ConnectionStatus.CONNECTING && 
                        !lastDeviceAddress.isNullOrBlank()) {
                        println("MetricsCarScreen: OBD service not connected (${service.connectionStatus.value}). Last device: $lastDeviceAddress. Attempting to start collector service.")
                        val intent = Intent(carContext, CarDashDataCollectorService::class.java).apply {
                            action = CarDashDataCollectorService.ACTION_START_SERVICE
                            putExtra(CarDashDataCollectorService.EXTRA_DEVICE_ADDRESS, lastDeviceAddress)
                        }
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                carContext.startForegroundService(intent)
                            } else {
                                carContext.startService(intent)
                            }
                            println("MetricsCarScreen: Sent start command to CarDashDataCollectorService.")
                        } catch (e: Exception) {
                            println("MetricsCarScreen: Error starting CarDashDataCollectorService: ${e.message}")
                            // Consider showing a message on the car screen, e.g., using a different template or a Toast
                            // For now, it will just show default values if connection fails.
                        }
                    } else {
                         println("MetricsCarScreen: OBD service status: ${service.connectionStatus.value}, Last device: $lastDeviceAddress. Not starting collector service.")
                    }

                    if (service.connectionStatus.value == ConnectionStatus.CONNECTED || service.connectionStatus.value == ConnectionStatus.CONNECTING) {
                        // If already connected, combine will fire with current values, leading to invalidate()
                        // If not, the connection attempt block will run.
                    }

                    // Cancel any previous job
                    metricsUpdateJob?.cancel()

                    // Subscribe to each metric flow individually
                    service.speedFlow.onEach { value ->
                        speed = value?.toString() ?: "--"
                        invalidate()
                    }.launchIn(lifecycleScope)

                    service.rpmFlow.onEach { value ->
                        rpm = value?.toString() ?: "--"
                        invalidate()
                    }.launchIn(lifecycleScope)

                    service.coolantTempFlow.onEach { value ->
                        coolantTemp = value?.let { "$it°C" } ?: "--"
                        invalidate()
                    }.launchIn(lifecycleScope)

                    service.engineLoadFlow.onEach { value ->
                        engineLoad = try {
                            value?.let { String.format(Locale.US, "%.1f%%", it.toFloat()) } ?: "--"
                        } catch (e: NumberFormatException) {
                            println("MetricsCarScreen: Error formatting engineLoad: $value - ${e.message}")
                            "--"
                        }
                        invalidate()
                    }.launchIn(lifecycleScope)

                    service.fuelLevelFlow.onEach { value ->
                        fuelLevel = try {
                            value?.let { String.format(Locale.US, "%.1f%%", it.toFloat()) } ?: "--"
                        } catch (e: NumberFormatException) {
                            println("MetricsCarScreen: Error formatting fuelLevel: $value - ${e.message}")
                            "--"
                        }
                        invalidate()
                    }.launchIn(lifecycleScope)

                    service.throttlePositionFlow.onEach { value ->
                        throttlePos = try {
                            value?.let { String.format(Locale.US, "%.1f%%", it.toFloat()) } ?: "--"
                        } catch (e: NumberFormatException) {
                            println("MetricsCarScreen: Error formatting throttlePos: $value - ${e.message}")
                            "--"
                        }
                        invalidate()
                    }.launchIn(lifecycleScope)

                    service.intakeAirTempFlow.onEach { value ->
                        intakeAirTemp = value?.let { "$it°C" } ?: "--"
                        invalidate()
                    }.launchIn(lifecycleScope)

                    service.fuelPressureFlow.onEach { value ->
                        fuelPressure = value?.toString() ?: "--"
                        invalidate()
                    }.launchIn(lifecycleScope)

                    service.baroPressureFlow.onEach { value ->
                        baroPressure = value?.toString() ?: "--"
                        invalidate()
                    }.launchIn(lifecycleScope)

                    service.batteryVoltageFlow.onEach { value ->
                        moduleVoltage = try {
                            // Assuming batteryVoltageFlow emits Float, not String needing toFloat()
                            value?.let { String.format(Locale.US, "%.1fV", it) } ?: "--" 
                        } catch (e: Exception) { // Broader catch if `value` itself is problematic before formatting
                            println("MetricsCarScreen: Error formatting moduleVoltage: $value - ${e.message}")
                            "--"
                        }
                        invalidate()
                    }.launchIn(lifecycleScope)
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                metricsUpdateJob?.cancel()
                metricsUpdateJob = null
                isServiceAvailable = false
            }
        })
    }

    override fun onGetTemplate(): Template {
        if (!isServiceAvailable) {
            return PaneTemplate.Builder(
                Pane.Builder()
                    .addRow(Row.Builder().setTitle("Status").addText("OBD Service: Not Available").build())
                    .build()
            )
            .setHeaderAction(Action.APP_ICON)
            .setTitle("CarDash Metrics - Error")
            .build()
        }

        // Restore PaneTemplate logic
        val paneBuilder = Pane.Builder()

        // Add metrics as Rows, pairing them up
        // Pair 1: Speed and RPM
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Speed: $speed")
                .addText("RPM: $rpm")
                .build()
        )
        // Pair 2: Coolant Temp and Engine Load
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Coolant Temp: $coolantTemp")
                .addText("Engine Load: $engineLoad")
                .build()
        )
        // Pair 3: Fuel Level and Throttle Pos
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Fuel Level: $fuelLevel")
                .addText("Throttle Pos: $throttlePos")
                .build()
        )
        // Pair 4: Intake Air Temp and Fuel Pressure
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Intake Air Temp: $intakeAirTemp")
                .addText("Fuel Pressure: $fuelPressure")
                .build()
        )
        // Pair 5: Baro Pressure and Module Voltage
        paneBuilder.addRow(
            Row.Builder()
                .setTitle("Baro Pressure: $baroPressure")
                .addText("Module Voltage: $moduleVoltage")
                .build()
        )
        
        return PaneTemplate.Builder(paneBuilder.build())
            .setHeaderAction(Action.APP_ICON)
            .setTitle("CarDash Metrics")
            .build()
    }
} 