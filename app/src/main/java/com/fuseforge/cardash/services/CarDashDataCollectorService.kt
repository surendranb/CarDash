package com.fuseforge.cardash.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.core.app.NotificationCompat
import com.fuseforge.cardash.MainActivity // Assuming MainActivity is the entry point
import com.fuseforge.cardash.R // Assuming R class is in com.fuseforge.cardash
import com.fuseforge.cardash.services.obd.BluetoothManager
import com.fuseforge.cardash.services.obd.BluetoothReceiver
import com.fuseforge.cardash.services.obd.OBDService
import android.content.IntentFilter
import com.fuseforge.cardash.CarDashApp // Import CarDashApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import android.os.PowerManager
import com.fuseforge.cardash.utils.HeartbeatAggregator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class CarDashDataCollectorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var obdService: OBDService

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private var bluetoothReceiver: BluetoothReceiver? = null
    private var lastAttemptedAddress: String? = null
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var heartbeatAggregator: HeartbeatAggregator? = null

    // Notification
    private val NOTIFICATION_CHANNEL_ID = "CarDashOBDServiceChannel"
    private val NOTIFICATION_ID = 1337

    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE_ADDRESS"
    }

    private var collectorJob: Job? = null
    private var currentSessionId: String? = null
    private var isEngineOn = false

    override fun onCreate() {
        super.onCreate()
        bluetoothManager = BluetoothManager(applicationContext)
        val app = applicationContext as CarDashApp
        obdService = app.obdService
        
        setupBluetoothReceiver()
        createNotificationChannel()
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CarDash::OBDDataCollectionWakeLock")
        
        // --- V2: PERSISTENT COLLECTOR track start ---
        // We start the collector once in onCreate. It survives Bluetooth drops.
        startGlobalCollector()
        
        println("CarDashDataCollectorService: onCreate - persistent trace started")
    }

    private fun startGlobalCollector() {
        val app = applicationContext as CarDashApp
        collectorJob?.cancel()
        collectorJob = serviceScope.launch {
            app.pollingEngine.dataFlow.collect { point ->
                // 1. Evaluate Engine State 
                val rpm = point.rpm ?: 0
                val load = point.engineLoad ?: 0
                val runningNow = rpm > 200 || load > 0
                
                // Initialize the session immediately on the first data point.
                // Do not wait for > 200 RPM, which causes critical data drops on slow starts.
                if (heartbeatAggregator == null) {
                    Log.d("CarDashService", "Genesis Data Point Received. Instantiating Master Ledger.")
                    app.obdServiceDiagnostics.startLoggingSession()
                    currentSessionId = app.obdServiceDiagnostics.getSessionId()
                    heartbeatAggregator = HeartbeatAggregator(currentSessionId!!)
                }

                if (runningNow) {
                    isEngineOn = true
                }

                // 2. Update Notification
                val text = "RPM: ${point.rpm ?: 0} | Engine: ${if (isEngineOn) "ON" else "OFF"}"
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, createNotification(text))
                
                // 3. Feed the pulse (Only if we have a valid session)
                heartbeatAggregator?.let { aggregator ->
                    val heartbeat = aggregator.push(point)
                    if (heartbeat != null) {
                        Log.d("CarDashService", "Minute rollover [${currentSessionId}]: inserting heartbeat to DB")
                        withContext(Dispatchers.IO) {
                            try {
                                app.database.obdLogDao().insertHeartbeat(heartbeat)
                            } catch (e: Exception) {
                                Log.e("CarDashService", "Critical Ledger Error: Failed to insert heartbeat. Coroutine survived.", e)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupBluetoothReceiver() {
        bluetoothReceiver = BluetoothReceiver(
            onBluetoothEnabled = {
                Log.d("CarDashService", "Bluetooth turned back ON. Attempting reconnect.")
                attemptAutoReconnect()
            },
            onDeviceDisconnected = { address ->
                if (address == lastAttemptedAddress) {
                    Log.d("CarDashService", "Monitored device disconnected. Triggering reconnect loop.")
                    _isConnected.value = false
                    attemptAutoReconnect()
                }
            }
        )
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    private fun attemptAutoReconnect() {
        val address = lastAttemptedAddress ?: (applicationContext as CarDashApp).preferencesManager.getLastConnectedDeviceAddress()
        if (address != null && !isConnected.value) {
            startConnectionTask(address)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CarDashService", "onStartCommand - Action: ${intent?.action}")
        
        startForeground(NOTIFICATION_ID, createNotification("Awaiting OBD-II link..."))

        when (intent?.action) {
            ACTION_START_SERVICE -> {
                val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (deviceAddress != null) {
                    lastAttemptedAddress = deviceAddress
                    startConnectionTask(deviceAddress)
                } 
            }
            ACTION_STOP_SERVICE -> {
                stopServiceInternal()
            }
        }
        return START_STICKY
    }

    private fun startConnectionTask(deviceAddress: String) {
        serviceScope.launch {
            Log.d("CarDashService", "Attempting connection to $deviceAddress")
            val connectionResult = obdService.connect(deviceAddress)
            if (connectionResult is OBDService.ConnectionResult.Success) {
                _isConnected.value = true
                val app = applicationContext as CarDashApp
                app.pollingEngine.start()
                startForeground(NOTIFICATION_ID, createNotification("Connected to OBD-II"))
                
                // Acquire WakeLock
                wakeLock?.acquire(10 * 60 * 60 * 1000L)
                
                // Save address
                app.preferencesManager.saveLastConnectedDeviceAddress(deviceAddress)
            } else {
                _isConnected.value = false
                Log.e("CarDashService", "Connection failed")
            }
        }
    }

    private fun stopServiceInternal() {
        Log.d("CarDashService", "stopServiceInternal called")
        // Use a persistent scope if possible or runBlocking for the final flush
        serviceScope.launch {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            
            // Final heartbeat flush - use IO dispatcher explicitly
            heartbeatAggregator?.flush()?.let { finalHeartbeat ->
                Log.d("CarDashService", "Flushing final partial heartbeat to DB")
                withContext(Dispatchers.IO) {
                    (applicationContext as CarDashApp).database.obdLogDao().insertHeartbeat(finalHeartbeat)
                }
            }

            val app = applicationContext as CarDashApp
            app.pollingEngine.stop()
            obdService.disconnect()
            _isConnected.value = false
            lastAttemptedAddress = null // Clear on manual stop
            stopForeground(true)
            stopSelf()
            Log.d("CarDashService", "Service stopped and polling engine stopped.")
        }
    }

    override fun onDestroy() {
        Log.d("CarDashService", "onDestroy - cleanup starting")
        bluetoothReceiver?.let { unregisterReceiver(it) }
        
        // Manual cleanup to ensure the final flush coroutine has a chance to start 
        // before we cancel the supervisor job
        stopServiceInternal()
        
        // Give it a tiny window for the I/O to initiate if not already finished
        serviceScope.launch {
            kotlinx.coroutines.delay(500)
            serviceJob.cancel()
            Log.d("CarDashService", "onDestroy - Coroutines cancelled")
        }
        
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