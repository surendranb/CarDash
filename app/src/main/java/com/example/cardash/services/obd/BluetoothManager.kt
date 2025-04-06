package com.example.cardash.services.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import java.io.IOException
import java.util.UUID

class BluetoothManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val sppUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID

    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null && 
               context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled ?: false
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): Set<BluetoothDevice> {
        return if (isBluetoothEnabled()) {
            bluetoothAdapter?.bondedDevices ?: emptySet()
        } else {
            emptySet()
        }
    }

    @SuppressLint("MissingPermission")
    fun createSocket(deviceAddress: String): BluetoothSocket? {
        if (!isBluetoothEnabled()) {
            println("BluetoothManager: Bluetooth not enabled")
            return null
        }
        
        return try {
            println("BluetoothManager: Creating socket for $deviceAddress")
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            println("BluetoothManager: Found device: ${device?.name ?: "Unknown"}")
            // Try secure RFCOMM first
            try {
                val socket = device?.createRfcommSocketToServiceRecord(sppUUID)
                println("BluetoothManager: Secure RFCOMM socket created")
                return socket
            } catch (e: IOException) {
                println("BluetoothManager: Secure RFCOMM failed, trying insecure: ${e.message}")
                // Fallback to insecure RFCOMM
                try {
                    val socket = device?.javaClass?.getMethod(
                        "createInsecureRfcommSocketToServiceRecord", 
                        UUID::class.java
                    )?.invoke(device, sppUUID) as? BluetoothSocket
                    println("BluetoothManager: Insecure RFCOMM socket created")
                    return socket
                } catch (e: Exception) {
                    println("BluetoothManager: Both secure and insecure RFCOMM failed")
                    throw e
                }
            }
        } catch (e: IOException) {
            println("BluetoothManager: Socket creation failed: ${e.javaClass.simpleName} - ${e.message}")
            null
        } catch (e: Exception) {
            println("BluetoothManager: Unexpected error: ${e.javaClass.simpleName} - ${e.message}")
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun isDevicePaired(deviceAddress: String): Boolean {
        return if (isBluetoothEnabled()) {
            bluetoothAdapter?.bondedDevices?.any { it.address == deviceAddress } ?: false
        } else {
            false
        }
    }

    companion object {
        // Bluetooth permissions required for Android 12+
        val REQUIRED_PERMISSIONS = arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
