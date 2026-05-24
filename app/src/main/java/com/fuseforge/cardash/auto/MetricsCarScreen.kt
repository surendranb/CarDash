package com.fuseforge.cardash.auto

import android.content.Intent
import android.os.Build
import android.text.SpannableString
import androidx.car.app.CarAppPermissionActivity
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.fuseforge.cardash.CarDashApp
import com.fuseforge.cardash.data.PreferencesManager
import com.fuseforge.cardash.model.TelemetryStatus
import com.fuseforge.cardash.model.VehicleState
import com.fuseforge.cardash.services.CarDashDataCollectorService
import com.fuseforge.cardash.services.obd.Telemetrist
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale

class MetricsCarScreen(carContext: CarContext) : Screen(carContext) {
    private var telemetrist: Telemetrist? = null
    private var preferencesManager: PreferencesManager? = null
    private var metricsUpdateJob: Job? = null

    private var lastInvalidateTime = 0L
    private val REFRESH_THROTTLE_MS = 2500L
    private var lastKnownConnectionStatus = TelemetryStatus.DISCONNECTED

    // Core HUD state (cached for Template generation)
    private var row1Text = "--"
    private var row2Text = "--"
    private var row3Text = "--"

    init {
        val app = carContext.applicationContext as CarDashApp
        telemetrist = app.telemetrist
        preferencesManager = app.preferencesManager

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                metricsUpdateJob?.cancel()

                telemetrist?.let { reactor ->
                    metricsUpdateJob = reactor.state.onEach { state ->
                        val isConnected = state.connectionStatus == TelemetryStatus.ACTIVE
                        val currentTime = System.currentTimeMillis()
                        
                        if (currentTime - lastInvalidateTime < REFRESH_THROTTLE_MS) return@onEach

                        // Row 1: ENGINE
                        row1Text = if (isConnected) {
                            "RPM: ${state.rpm}  |  Load: ${state.engineLoad}%"
                        } else "RPM: --  |  Load: --"
                        
                        // Row 2: THERMALS
                        row2Text = if (isConnected) {
                            "Coolant: ${state.coolantTemp}°C  |  Intake: ${state.intakeAirTemp}°C"
                        } else "Coolant: --  |  Intake: --"
                        
                        // Row 3: SYSTEM
                        val formattedVolt = String.format(Locale.US, "%.1f", state.batteryVoltage)
                        row3Text = if (isConnected) {
                            "Batt: ${formattedVolt}V  |  Alt: ${state.altitude.toInt()}m"
                        } else "Batt: --  |  Alt: --"

                        lastKnownConnectionStatus = state.connectionStatus
                        lastInvalidateTime = currentTime
                        invalidate()
                    }.launchIn(lifecycleScope)
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                metricsUpdateJob?.cancel()
            }
        })
    }

    override fun onGetTemplate(): Template {
        val isConnecting = lastKnownConnectionStatus == TelemetryStatus.CONNECTING || 
                           lastKnownConnectionStatus == TelemetryStatus.HANDSHAKING

        val itemListBuilder = ItemList.Builder()

        // Replace Grid Item clutter with dense list rows
        addListRow(itemListBuilder, "ENGINE", row1Text)
        addListRow(itemListBuilder, "THERMALS", row2Text)
        addListRow(itemListBuilder, "SYSTEM", row3Text)

        // Root screens must use APP_ICON to pass Android Auto driving UX restrictions
        val headerAction = Action.APP_ICON

        return ListTemplate.Builder()
            .setSingleList(itemListBuilder.build())
            .setHeaderAction(headerAction)
            .setTitle(if (isConnecting) "Linking OBD-II..." else "CarDash Navigator")
            .build()
    }

    private fun addListRow(builder: ItemList.Builder, title: String, text: String) {
        val rowBuilder = Row.Builder()
            .setTitle(title)
            .addText(text)
        
        builder.addItem(rowBuilder.build())
    }

    private fun getCompassDirection(bearing: Float): String {
        val directions = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW", "N")
        return directions[((bearing % 360) / 45).toInt()]
    }
}