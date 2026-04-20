package com.fuseforge.cardash.auto

import android.content.Intent
import android.os.Build
import android.text.SpannableString
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.fuseforge.cardash.CarDashApp
import com.fuseforge.cardash.data.PreferencesManager
import com.fuseforge.cardash.services.CarDashDataCollectorService
import com.fuseforge.cardash.services.obd.Telemetrist
import com.fuseforge.cardash.model.TelemetryStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale

class MetricsCarScreen(carContext: CarContext) : Screen(carContext) {

    private var telemetrist: Telemetrist? = null
    private var preferencesManager: PreferencesManager? = null
    private var metricsUpdateJob: Job? = null
    private var lastInvalidateTime = 0L
    private val REFRESH_THROTTLE_MS = 2500L // Safety Guardrail for AA Quota
    private var lastKnownConnectionStatus: TelemetryStatus? = null

    // 6 Diagnostic-Centric Metrics (Harden to non-OEM essentials)
    private var baroPressureText = "--"
    private var engineLoadText = "--"
    private var coolantTempText = "--"
    private var voltageText = "--"
    private var throttlePosText = "--"
    private var intakeAirTempText = "--"

    // Status Colors
    private var baroColor = CarColor.DEFAULT
    private var loadColor = CarColor.DEFAULT
    private var coolantColor = CarColor.DEFAULT
    private var voltageColor = CarColor.DEFAULT
    private var throttleColor = CarColor.DEFAULT
    private var iatColor = CarColor.DEFAULT

    init {
        val app = carContext.applicationContext as CarDashApp
        telemetrist = app.telemetrist
        preferencesManager = app.preferencesManager

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)

                telemetrist?.let { reactor ->
                    val lastDeviceAddress = preferencesManager?.getLastConnectedDeviceAddress()
                    if (reactor.state.value.connectionStatus == TelemetryStatus.DISCONNECTED && 
                        !lastDeviceAddress.isNullOrBlank()) {
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
                        } catch (e: Exception) {
                            // Ignored during startup validation
                        }
                    }

                    // V3: Observe the Telemetrist Reactor
                    metricsUpdateJob = reactor.state.onEach { state ->
                        val isConnected = state.connectionStatus == TelemetryStatus.ACTIVE
                        
                        // 1. Barometric Pressure
                        val baroVal = state.baroPressure
                        val newBaroText = if (isConnected) "${baroVal}kPa" else "--"

                        // 2. Engine Load
                        val loadVal = state.engineLoad.toFloat()
                        val newLoadText = if (isConnected) String.format(Locale.US, "%.0f%%", loadVal) else "--"
                        val newLoadColor = when {
                            loadVal >= 85f -> CarColor.RED
                            loadVal >= 65f -> CarColor.YELLOW
                            loadVal > 5f -> CarColor.GREEN
                            else -> CarColor.DEFAULT
                        }

                        // 3. Coolant Temp
                        val coolantVal = state.coolantTemp
                        val newCoolantText = if (isConnected) "$coolantVal°C" else "--"
                        val newCoolantColor = when {
                            coolantVal >= 105 -> CarColor.RED
                            coolantVal >= 95 -> CarColor.YELLOW
                            coolantVal > 50 -> CarColor.GREEN
                            else -> CarColor.DEFAULT
                        }

                        // 4. Battery Voltage
                        val voltVal = state.batteryVoltage
                        val newVoltageText = if (isConnected) String.format(Locale.US, "%.1fV", voltVal) else "--"
                        val newVoltageColor = when {
                            voltVal < 11.8f && voltVal > 0f -> CarColor.RED
                            voltVal > 15.2f -> CarColor.RED
                            voltVal in 11.8f..12.6f -> CarColor.YELLOW
                            voltVal > 12.6f -> CarColor.GREEN
                            else -> CarColor.DEFAULT
                        }

                        // 5. Throttle Position
                        val throttleVal = state.throttlePos
                        val newThrottleText = if (isConnected) "$throttleVal%" else "--"

                        // 6. Intake Air Temp
                        val iatVal = state.intakeAirTemp
                        val newIatText = if (isConnected) "$iatVal°C" else "--"

                        // Cycle management
                        val currentTime = System.currentTimeMillis()
                        val connectionStatus = state.connectionStatus

                        if (currentTime - lastInvalidateTime >= REFRESH_THROTTLE_MS) {
                            val connectionChanged = connectionStatus != lastKnownConnectionStatus

                            // Assess change AGAINST currently painted state
                            val hasChanged = newBaroText != baroPressureText || newLoadText != engineLoadText || 
                                           newCoolantText != coolantTempText || newVoltageText != voltageText ||
                                           newThrottleText != throttlePosText || newIatText != intakeAirTempText ||
                                           newLoadColor != loadColor || newCoolantColor != coolantColor ||
                                           newVoltageColor != voltageColor || connectionChanged

                            if (hasChanged) {
                                baroPressureText = newBaroText
                                engineLoadText = newLoadText
                                coolantTempText = newCoolantText
                                voltageText = newVoltageText
                                throttlePosText = newThrottleText
                                intakeAirTempText = newIatText
                                
                                loadColor = newLoadColor
                                coolantColor = newCoolantColor
                                voltageColor = newVoltageColor
                                lastKnownConnectionStatus = connectionStatus

                                lastInvalidateTime = currentTime
                                invalidate() 
                            }
                        }
                    }?.launchIn(lifecycleScope)
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
        val itemListBuilder = ItemList.Builder()
        val status = telemetrist?.state?.value?.connectionStatus
        val isConnecting = status == TelemetryStatus.CONNECTING || status == TelemetryStatus.HANDSHAKING
        val isDisconnected = status == TelemetryStatus.DISCONNECTED || status == TelemetryStatus.ERROR

        // Minimalist Grid Items - No space-wasting logos, just Number + Label
        addMetricItem(itemListBuilder, engineLoadText, "ENG LOAD", loadColor, isConnecting)
        addMetricItem(itemListBuilder, coolantTempText, "COOLANT", coolantColor, isConnecting)
        addMetricItem(itemListBuilder, voltageText, "BATTERY", voltageColor, isConnecting)
        addMetricItem(itemListBuilder, baroPressureText, "BARO PSI", CarColor.DEFAULT, isConnecting)
        addMetricItem(itemListBuilder, throttlePosText, "THROTTLE", CarColor.DEFAULT, isConnecting)
        addMetricItem(itemListBuilder, intakeAirTempText, "INTAKE", CarColor.DEFAULT, isConnecting)

        val headerAction = if (isDisconnected || engineLoadText == "--") Action.APP_ICON else Action.BACK

        return GridTemplate.Builder()
            .setSingleList(itemListBuilder.build())
            .setHeaderAction(headerAction)
            .setTitle(if (isConnecting) "Linking OBD-II..." else "CarDash Diagnostic")
            .build()
    }

    private fun addMetricItem(builder: ItemList.Builder, title: String, label: String, color: CarColor, loading: Boolean) {
        val spannableTitle = SpannableString(title)
        if (color != CarColor.DEFAULT) {
            spannableTitle.setSpan(
                androidx.car.app.model.ForegroundCarColorSpan.create(color),
                0,
                title.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        builder.addItem(
            GridItem.Builder()
                .setTitle(spannableTitle)
                .setText(label)
                .setImage(CarIcon.Builder(androidx.core.graphics.drawable.IconCompat.createWithResource(carContext, com.fuseforge.cardash.R.drawable.ic_dot)).build(), GridItem.IMAGE_TYPE_ICON)
                .setLoading(loading)
                .build()
        )
    }
} 