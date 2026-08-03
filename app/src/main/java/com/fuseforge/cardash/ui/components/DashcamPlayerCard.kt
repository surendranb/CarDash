package com.fuseforge.cardash.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.fuseforge.cardash.ui.theme.Neutral
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DashcamPlayerCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val streamUrl = prefs.getDashcamUrl()
    var isPlaying by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }
    var networkCallback by remember { mutableStateOf<ConnectivityManager.NetworkCallback?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    // Connect automatically on mount
    LaunchedEffect(streamUrl) {
        if (streamUrl.isBlank()) {
            connectionError = "No stream URL configured."
            return@LaunchedEffect
        }
        
        isConnecting = true
        connectionError = null
        
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
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
        try {
            connectivityManager.requestNetwork(request, callback, 5000)
        } catch (e: SecurityException) {
            isConnecting = false
            connectionError = "Permission denied to request network."
        }
    }

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
            networkCallback?.let { callback ->
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                try {
                    connectivityManager.unregisterNetworkCallback(callback)
                } catch (e: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Neutral.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        if (exoPlayer != null && isPlaying) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = connectionError ?: if (isConnecting) "Connecting to Dashcam..." else "Waiting...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
