package com.example.cardash.services.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.cardash.CarDashApp
import com.example.cardash.services.obd.OBDService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale
import android.content.Intent
import android.os.Build
import com.example.cardash.services.CarDashDataCollectorService
import com.example.cardash.data.PreferencesManager
import com.example.cardash.services.obd.ConnectionStatus

class MetricsCarScreen(carContext: CarContext) : Screen(carContext) {

    private var obdService: OBDService? = null
    private var preferencesManager: PreferencesManager? = null
    private var metricsUpdateJob: Job? = null

    private var speed: String = "--"
    private var rpm: String = "--"
    private var coolantTemp: String = "--"
    private var engineLoad: String = "--"
    private var fuelLevel: String = "--"
    private var throttlePos: String = "--"
    private var intakeAirTemp: String = "--"
    private var maf: String = "--"
    private var ambientAirTemp: String = "--"
    private var moduleVoltage: String = "--"

    init {
        val app = carContext.applicationContext as CarDashApp
        obdService = app.obdService
        preferencesManager = app.preferencesManager

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)

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

                    metricsUpdateJob = combine(
                        service.speedFlow,
                        service.rpmFlow,
                        service.coolantTempFlow,
                        service.engineLoadFlow,
                        service.fuelLevelFlow,
                        service.throttlePositionFlow,
                        service.intakeAirTempFlow,
                        service.mafFlow,
                        service.ambientAirTempFlow,
                        service.batteryVoltageFlow
                    ) { values ->
                        speed = values[0]?.toString() ?: "--"
                        rpm = values[1]?.toString() ?: "--"
                        coolantTemp = values[2]?.toString()?.let { "$it°C" } ?: "--"
                        engineLoad = values[3]?.toString()?.let { String.format(Locale.US, "%.1f%%", it) } ?: "--"
                        fuelLevel = values[4]?.toString()?.let { String.format(Locale.US, "%.1f%%", it) } ?: "--"
                        throttlePos = values[5]?.toString()?.let { String.format(Locale.US, "%.1f%%", it) } ?: "--"
                        intakeAirTemp = values[6]?.toString()?.let { "$it°C" } ?: "--"
                        maf = values[7]?.toString()?.let { String.format(Locale.US, "%.2f g/s", it) } ?: "--"
                        ambientAirTemp = values[8]?.toString()?.let { "$it°C" } ?: "--"
                        moduleVoltage = values[9]?.toString()?.let { String.format(Locale.US, "%.1fV", it) } ?: "--"
                        invalidate() // Request a template refresh
                    }.onEach { /* Each emission from combine triggers the block above */ }
                    .launchIn(lifecycleScope) // Launch the collection in the lifecycleScope
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                metricsUpdateJob?.cancel()
                metricsUpdateJob = null
            }
        })
    }

    override fun onGetTemplate(): Template {
        val paneBuilder = Pane.Builder()

        paneBuilder.addRow(
            Row.Builder().setTitle("Speed").addText(speed).build()
        )
        paneBuilder.addRow(
            Row.Builder().setTitle("RPM").addText(rpm).build()
        )
        paneBuilder.addRow(
            Row.Builder().setTitle("Coolant Temp").addText(coolantTemp).build()
        )
        paneBuilder.addRow(
            Row.Builder().setTitle("Engine Load").addText(engineLoad).build()
        )
        // For PaneTemplate, usually up to 4-5 rows are immediately visible without scrolling issues.
        // To show more, the user might need to scroll. We will add all 10.
        paneBuilder.addRow(
            Row.Builder().setTitle("Fuel Level").addText(fuelLevel).build()
        )
        paneBuilder.addRow(
            Row.Builder().setTitle("Throttle Pos").addText(throttlePos).build()
        )
        paneBuilder.addRow(
            Row.Builder().setTitle("Intake Air Temp").addText(intakeAirTemp).build()
        )
        paneBuilder.addRow(
            Row.Builder().setTitle("MAF").addText(maf).build()
        )
        paneBuilder.addRow(
            Row.Builder().setTitle("Ambient Air Temp").addText(ambientAirTemp).build()
        )
        paneBuilder.addRow(
            Row.Builder().setTitle("Module Voltage").addText(moduleVoltage).build()
        )

        return PaneTemplate.Builder(paneBuilder.build())
            .setHeaderAction(Action.APP_ICON)
            .setTitle("CarDash Metrics")
            .build()
    }
} 