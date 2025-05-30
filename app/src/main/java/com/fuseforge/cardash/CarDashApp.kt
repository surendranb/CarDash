package com.fuseforge.cardash

import android.app.Application
import com.fuseforge.cardash.data.PreferencesManager
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.services.obd.BluetoothManager
import com.fuseforge.cardash.services.obd.OBDService
import com.fuseforge.cardash.services.obd.OBDServiceWithDiagnostics
import com.fuseforge.cardash.utils.MockDataGenerator
import com.fuseforge.cardash.utils.MockDiagnosticGenerator
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

    // Base OBD service with sequential command queue
    val obdService: OBDService by lazy {
        OBDService(bluetoothManager, applicationScope)
    }
    
    // OBD service with diagnostics wrapping the base service
    val obdServiceDiagnostics: OBDServiceWithDiagnostics by lazy {
        OBDServiceWithDiagnostics(obdService, applicationContext)
    }
    
    val mockDataGenerator: MockDataGenerator by lazy {
        MockDataGenerator(applicationContext)
    }
    
    val mockDiagnosticGenerator: MockDiagnosticGenerator by lazy {
        MockDiagnosticGenerator(applicationContext)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Start a diagnostic session when the app starts
        applicationScope.launch {
            obdServiceDiagnostics.startLoggingSession()
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Ensure service cleanup on app termination
        applicationScope.launch {
            obdService.disconnect()
        }
    }
}