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
import androidx.core.app.NotificationCompat
import com.fuseforge.cardash.MainActivity
import com.fuseforge.cardash.R
import com.fuseforge.cardash.CarDashApp
import com.fuseforge.cardash.model.TelemetryStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import android.os.PowerManager

class CarDashDataCollectorService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var collectorJob: Job? = null

    private val NOTIFICATION_CHANNEL_ID = "CarDashOBDServiceChannel"
    private val NOTIFICATION_ID = 1337

    companion object {
        const val ACTION_START_SERVICE = "ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"
        const val EXTRA_DEVICE_ADDRESS = "EXTRA_DEVICE_ADDRESS"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CarDash::OBDDataCollectionWakeLock")
        
        startReactorObserver()
    }

    private fun startReactorObserver() {
        val app = applicationContext as CarDashApp
        collectorJob?.cancel()
        collectorJob = serviceScope.launch {
            app.telemetrist.state.collect { state ->
                if (state.connectionStatus == TelemetryStatus.ACTIVE) {
                    val text = "RPM: ${state.rpm} | Speed: ${state.speedKph} km/h"
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, createNotification(text))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification("Awaiting OBD-II link..."),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification("Awaiting OBD-II link..."))
        }

        when (intent?.action) {
            ACTION_START_SERVICE -> {
                val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (address != null) {
                    startConnectionTask(address)
                } 
            }
            ACTION_STOP_SERVICE -> stopServiceInternal()
        }
        return START_STICKY
    }

    private fun startConnectionTask(deviceAddress: String) {
        val app = applicationContext as CarDashApp
        app.telemetrist.start(deviceAddress)
        app.vehicleLedger.start()
        
        wakeLock?.acquire(10 * 60 * 60 * 1000L)
        app.preferencesManager.saveLastConnectedDeviceAddress(deviceAddress)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification("Connected to Telemetry Reactor"),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification("Connected to Telemetry Reactor"))
        }
    }

    private fun stopServiceInternal() {
        val app = applicationContext as CarDashApp
        app.telemetrist.stop()
        app.vehicleLedger.stop()
        
        if (wakeLock?.isHeld == true) wakeLock?.release()
        
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopServiceInternal()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "CarDash OBD", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("CarDash Active")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground) 
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    override fun onBind(intent: Intent): IBinder? = null
}