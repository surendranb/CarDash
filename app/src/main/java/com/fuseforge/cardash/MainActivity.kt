package com.fuseforge.cardash

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.app.ActivityCompat
import com.fuseforge.cardash.ui.MainScreen
import com.fuseforge.cardash.ui.theme.CarDashTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val permissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (!allGranted) {
            // Handle denied permissions
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check and request permissions
        if (permissions.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            permissionLauncher.launch(permissions)
        }

        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            
            CarDashTheme {
                MainScreen(
                    onPermissionNeeded = {
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Bluetooth permissions required",
                                actionLabel = "Grant",
                                duration = SnackbarDuration.Indefinite
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                permissionLauncher.launch(permissions)
                            }
                        }
                    }
                )
            }
        }
    }
}
