package com.example.cardash

import android.app.Application
import com.example.cardash.services.obd.BluetoothManager
import com.example.cardash.services.obd.OBDService

class CarDashApp : Application() {

    // Lazily initialize dependencies
    val bluetoothManager: BluetoothManager by lazy {
        BluetoothManager(applicationContext)
    }

    val obdService: OBDService by lazy {
        OBDService(bluetoothManager)
    }
}
