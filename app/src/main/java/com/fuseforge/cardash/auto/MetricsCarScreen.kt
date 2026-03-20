package com.fuseforge.cardash.auto

import android.content.Intent
import android.os.Build
import android.text.SpannableString
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.ForegroundCarColorSpan
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
import com.fuseforge.cardash.services.obd.ConnectionStatus
import com.fuseforge.cardash.services.obd.OBDService
import com.fuseforge.cardash.services.obd.PollingEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.Locale

class MetricsCarScreen(carContext: CarContext) : Screen(carContext) {

    private var obdService: OBDService? = null
    private var pollingEngine: PollingEngine? = null
    private var preferencesManager: PreferencesManager? = null
    private var metricsUpdateJob: Job? = null

    // 6 Driver-Centric Metrics
    private var rpmText: CharSequence = "--"
    private var engineLoadText: CharSequence = "--"
    private var coolantTempText: CharSequence = "--"
    private var voltageText: CharSequence = "--"
    private var throttlePosText: CharSequence = "--"
    private var intakeAirTempText: CharSequence = "--"

    init {
        val app = carContext.applicationContext as CarDashApp
        obdService = app.obdService
        pollingEngine = app.pollingEngine
        preferencesManager = app.preferencesManager

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)

                obdService?.let { service ->
                    val lastDeviceAddress = preferencesManager?.getLastConnectedDeviceAddress()
                    if (service.connectionStatus.value != ConnectionStatus.CONNECTED && 
                        service.connectionStatus.value != ConnectionStatus.CONNECTING && 
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

                    // Feed off the modern unified dataFlow
                    metricsUpdateJob = pollingEngine?.dataFlow?.onEach { point ->
                        
                        // 1. Exact RPM
                        val rpmVal = point.rpm ?: 0
                        val rpmStr = if (rpmVal > 0) rpmVal.toString() else "--"
                        val rpmColor = when {
                            rpmVal >= 4500 -> CarColor.RED
                            rpmVal >= 3000 -> CarColor.YELLOW
                            rpmVal > 0 -> CarColor.GREEN
                            else -> CarColor.DEFAULT
                        }
                        rpmText = colorize(rpmStr, rpmColor)

                        // 2. Engine Load
                        val loadVal = point.engineLoad?.toFloat() ?: 0f
                        val loadStr = if (loadVal > 0f) String.format(Locale.US, "%.1f%%", loadVal) else "--"
                        val loadColor = when {
                            loadVal >= 90f -> CarColor.RED
                            loadVal >= 70f -> CarColor.YELLOW
                            else -> CarColor.DEFAULT
                        }
                        engineLoadText = colorize(loadStr, loadColor)

                        // 3. Coolant Temp
                        val coolantVal = point.coolantTemp ?: 0
                        val coolantStr = if (coolantVal > 0) "$coolantVal°C" else "--"
                        val coolantColor = when {
                            coolantVal >= 105 -> CarColor.RED
                            coolantVal >= 95 -> CarColor.YELLOW
                            else -> CarColor.DEFAULT
                        }
                        coolantTempText = colorize(coolantStr, coolantColor)

                        // 4. Battery Voltage
                        val voltVal = point.batteryVoltage ?: 0f
                        val voltStr = if (voltVal > 0f) String.format(Locale.US, "%.1f V", voltVal) else "--"
                        val voltColor = when {
                            voltVal < 12.0f && voltVal > 0f -> CarColor.RED
                            voltVal > 15.0f -> CarColor.RED
                            voltVal in 12.0f..12.8f -> CarColor.YELLOW
                            voltVal > 12.8f -> CarColor.GREEN
                            else -> CarColor.DEFAULT
                        }
                        voltageText = colorize(voltStr, voltColor)

                        // 5. Throttle Position
                        val throttleVal = point.throttlePosition ?: 0
                        val throttleStr = if (throttleVal > 0) "$throttleVal%" else "--"
                        throttlePosText = colorize(throttleStr, CarColor.DEFAULT)

                        // 6. Intake Air Temp
                        val iatVal = point.intakeAirTemp ?: 0
                        val iatStr = if (iatVal > 0 || point.rpm != null) "$iatVal°C" else "--"
                        intakeAirTempText = colorize(iatStr, CarColor.DEFAULT)

                        invalidate() // Request a template refresh
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

    private fun colorize(text: String, color: CarColor): CharSequence {
        if (color == CarColor.DEFAULT || text == "--") return text
        val spannable = SpannableString(text)
        spannable.setSpan(ForegroundCarColorSpan.create(color), 0, text.length, 0)
        return spannable
    }

    override fun onGetTemplate(): Template {
        val itemListBuilder = ItemList.Builder()

        // Create the 6 GridItems with the formatted CharSequences
        itemListBuilder.addItem(GridItem.Builder().setTitle(rpmText).setText("Engine RPM").build())
        itemListBuilder.addItem(GridItem.Builder().setTitle(engineLoadText).setText("Engine Load").build())
        itemListBuilder.addItem(GridItem.Builder().setTitle(coolantTempText).setText("Coolant Temp").build())
        itemListBuilder.addItem(GridItem.Builder().setTitle(voltageText).setText("Battery / Module").build())
        itemListBuilder.addItem(GridItem.Builder().setTitle(throttlePosText).setText("Throttle Position").build())
        itemListBuilder.addItem(GridItem.Builder().setTitle(intakeAirTempText).setText("Intake Air Temp").build())

        return GridTemplate.Builder()
            .setSingleList(itemListBuilder.build())
            .setHeaderAction(Action.APP_ICON)
            .setTitle("CarDash Live Health")
            .build()
    }
} 