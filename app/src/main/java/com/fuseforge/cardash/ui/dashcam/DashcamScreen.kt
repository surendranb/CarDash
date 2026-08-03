package com.fuseforge.cardash.ui.dashcam

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.PlayerView
import com.fuseforge.cardash.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DashcamScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    var streamUrl by remember { mutableStateOf(prefs.getDashcamUrl()) }
    var isPlaying by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    
    // Store callback reference so we can unregister it later
    var networkCallback by remember { mutableStateOf<ConnectivityManager.NetworkCallback?>(null) }

    // Manage ExoPlayer lifecycle
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer?.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer?.release()
            
            // Unregister network callback on dispose
            networkCallback?.let { callback ->
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (e: Exception) {
                    // Ignore if already unregistered
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Dashcam Feed", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = streamUrl,
            onValueChange = { 
                streamUrl = it
                prefs.setDashcamUrl(it)
            },
            label = { Text("RTSP Stream URL") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (connectionError != null) {
            Text(
                text = connectionError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (isConnecting || isPlaying) return@Button
                    
                    isConnecting = true
                    connectionError = null
                    
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    
                    // Unregister old callback if exists
                    networkCallback?.let { 
                        try { connectivityManager.unregisterNetworkCallback(it) } catch (e: Exception) {} 
                    }
                    
                    val request = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build()
                        
                    val callback = object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            scope.launch(Dispatchers.Main) {
                                isConnecting = false
                                connectionError = null
                                
                                if (exoPlayer == null) {
                                    exoPlayer = ExoPlayer.Builder(context).build()
                                }
                                
                                val mediaSource = RtspMediaSource.Factory()
                                    .setForceUseRtpTcp(true)
                                    .setSocketFactory(network.socketFactory)
                                    .createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl)))
                                
                                exoPlayer?.setMediaSource(mediaSource)
                                exoPlayer?.prepare()
                                exoPlayer?.playWhenReady = true
                                isPlaying = true
                            }
                        }
                        
                        override fun onLost(network: Network) {
                            scope.launch(Dispatchers.Main) {
                                isConnecting = false
                                connectionError = "Connection to camera lost (WiFi disconnected)"
                                exoPlayer?.stop()
                                exoPlayer?.clearMediaItems()
                                isPlaying = false
                            }
                        }
                        
                        override fun onUnavailable() {
                            scope.launch(Dispatchers.Main) {
                                isConnecting = false
                                connectionError = "Camera WiFi not found. Please connect first."
                            }
                        }
                    }
                    
                    networkCallback = callback
                    // requestNetwork with a timeout of 5 seconds to catch if wifi isn't available
                    connectivityManager.requestNetwork(request, callback, 5000)
                },
                enabled = !isConnecting && !isPlaying
            ) {
                Text(if (isConnecting) "Connecting..." else "Connect")
            }
            
            Button(
                onClick = {
                    exoPlayer?.stop()
                    exoPlayer?.clearMediaItems()
                    isPlaying = false
                    isConnecting = false
                    
                    // Unregister network callback
                    networkCallback?.let { callback ->
                        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        try {
                            connectivityManager.unregisterNetworkCallback(callback)
                        } catch (e: Exception) { }
                        networkCallback = null
                    }
                },
                enabled = isPlaying || isConnecting
            ) {
                Text("Stop")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (exoPlayer != null && isPlaying) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Text(if (isConnecting) "Waiting for WiFi network..." else "Not connected")
            }
        }
    }
}
