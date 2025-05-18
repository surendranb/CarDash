package com.example.cardash.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.cardash.MainActivity // Assuming MainActivity is the entry point
import com.example.cardash.R // Assuming R class is in com.example.cardash
import com.example.cardash.services.obd.BluetoothManager
import com.example.cardash.services.obd.OBDService
import com.example.cardash.CarDashApp // Import CarDashApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CarDashDataCollectorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var bluetoothManager: BluetoothManager
    // lateinit var obdService: OBDService // Commented out: Made public for now for easier access from UI
    private lateinit var obdService: OBDService // Use the singleton from CarDashApp
                                        // Consider a cleaner approach (Binder, Broadcast, public Flows) later

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    // Notification
    private val NOTIFICATION_CHANNEL_ID = "CarDashOBDServiceChannel"
    private val NOTIFICATION_ID = 1337

    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE_ADDRESS"
    }

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = BluetoothManager(applicationContext)
        // obdService = OBDService(bluetoothManager, CoroutineScope(Dispatchers.IO + serviceJob)) // Pass serviceJob for cancellation
        val app = applicationContext as CarDashApp
        obdService = app.obdService // Use the singleton instance
        createNotificationChannel()
        println("CarDashDataCollectorService: onCreate - using shared OBDService instance")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        println("CarDashDataCollectorService: onStartCommand - Action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (deviceAddress != null) {
                    serviceScope.launch {
                        println("CarDashDataCollectorService: Attempting to connect to $deviceAddress")
                        val connectionResult = obdService.connect(deviceAddress)
                        if (connectionResult is OBDService.ConnectionResult.Success) {
                            _isConnected.value = true
                            startForeground(NOTIFICATION_ID, createNotification("Connected to OBD-II"))
                            println("CarDashDataCollectorService: Connection SUCCESS, service started in foreground.")
                            // Save the successfully connected device address
                            val app = applicationContext as CarDashApp
                            app.preferencesManager.saveLastConnectedDeviceAddress(deviceAddress)
                        } else {
                            _isConnected.value = false
                            val errorMessage = (connectionResult as? OBDService.ConnectionResult.Error)?.message ?: "Unknown connection error"
                            println("CarDashDataCollectorService: Connection FAILED: $errorMessage")
                            // Optionally, update notification to show error or stop service
                            stopSelf() // Stop if connection fails
                        }
                    }
                } else {
                     println("CarDashDataCollectorService: No device address provided, stopping service.")
                    stopSelf()
                }
            }
            ACTION_STOP_SERVICE -> {
                println("CarDashDataCollectorService: Stopping service via action.")
                stopServiceInternal()
            }
            else -> {
                println("CarDashDataCollectorService: Received unhandled action: ${intent?.action}")
            }
        }
        return START_STICKY // Restart if killed, but intent might be null
    }

    private fun stopServiceInternal() {
        serviceScope.launch {
            obdService.disconnect()
            _isConnected.value = false
            stopForeground(true)
            stopSelf()
            println("CarDashDataCollectorService: Service stopped and resources released.")
        }
    }

    override fun onDestroy() {
        println("CarDashDataCollectorService: onDestroy")
        stopServiceInternal() // Ensure cleanup
        serviceJob.cancel() // Cancel all coroutines started in serviceScope
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "CarDash OBD Service Channel",
                NotificationManager.IMPORTANCE_LOW // Use LOW to avoid sound/vibration but still show
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java) // Opens MainActivity on tap
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            pendingIntentFlags
        )

        // TODO: Replace with actual app icon
        val icon = R.drawable.ic_launcher_foreground // Placeholder, ensure this exists or use a default

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("CarDash Active")
            .setContentText(contentText)
            .setSmallIcon(icon) 
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Makes the notification non-dismissible by swiping
            .setOnlyAlertOnce(true) // Don't vibrate/sound for updates if already shown
            .build()
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }
} 