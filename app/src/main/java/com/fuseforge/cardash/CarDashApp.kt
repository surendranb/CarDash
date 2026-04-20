package com.fuseforge.cardash

import android.app.Application
import com.fuseforge.cardash.data.PreferencesManager
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.data.db.DTCDataSeeder
import com.fuseforge.cardash.model.TelemetryStatus
import com.fuseforge.cardash.services.obd.BluetoothManager
import com.fuseforge.cardash.services.obd.Telemetrist
import com.fuseforge.cardash.services.obd.VehicleLedger
import com.fuseforge.cardash.services.sensors.SensorCollector
import com.fuseforge.cardash.utils.OBDLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CarDashApp : Application() {

    // Application scope for coroutines that should live as long as the app
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Lazily initialize dependencies
    val bluetoothManager: BluetoothManager by lazy {
        BluetoothManager(applicationContext)
    }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(applicationContext)
    }

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(applicationContext)
    }
    
    val obdLogger: OBDLogger by lazy {
        OBDLogger(applicationContext)
    }

    val sensorCollector: SensorCollector by lazy {
        SensorCollector(applicationContext)
    }

    // V3: The high-fidelity Telemetry Reactor
    val telemetrist: Telemetrist by lazy {
        Telemetrist(bluetoothManager, sensorCollector, applicationScope)
    }

    val vehicleLedger: VehicleLedger by lazy {
        VehicleLedger(database, telemetrist, applicationScope)
    }
    
    override fun onCreate() {
        super.onCreate()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Ensure service cleanup on app termination
        telemetrist.stop()
        vehicleLedger.stop()
    }
}