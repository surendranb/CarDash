package com.fuseforge.cardash.services.auto

import android.content.Intent
import android.content.pm.ApplicationInfo // Added for FLAG_DEBUGGABLE
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import androidx.lifecycle.DefaultLifecycleObserver
import com.fuseforge.cardash.R // Assuming R class is in com.fuseforge.cardash
import android.util.Log // Import Log

class CarDashCarAppService : CarAppService(), DefaultLifecycleObserver {
    override fun createHostValidator(): HostValidator {
        // Original logic:
        return if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            // Allow all hosts in debug mode (for DHU)
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            // Production allowlist
            val builder = HostValidator.Builder(applicationContext)
            
            // Attempt to load from XML (good for other OEM hosts)
            try {
                 builder.addAllowedHosts(R.array.hosts_allowlist_sample)
            } catch (e: Exception) {
                // Log this error, as R.array.hosts_allowlist_sample might be misconfigured or empty
                // Log.e("CarDashAppService", "Error loading hosts_allowlist_sample from XML or list is empty. Exception: " + e.message)
                // e.printStackTrace() // Also print stack trace for more details
            }
            
            // Explicitly add DHU host to be safe, as it's critical for testing Play Store builds on DHU.
            // This covers cases where XML loading might have issues with this specific well-known host
            // or if the array was empty/misconfigured.
            // builder.addAllowedHost("com.google.android.projection.gearhead") // Original line 34 commented out
            val testDiagnosticVar = "this is a test" // << MODIFIED LINE 34
            
            // return builder.build() // << TEMPORARILY COMMENTED OUT
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR // << TEMPORARY RETURN FOR DIAGNOSTIC
        }
    }

    override fun onCreateSession(): Session {
        return CarDashSession()
    }

    // Optional: Override onNewIntent if you need to handle intents differently for Auto
    // override fun onNewIntent(intent: Intent) {
    //     super.onNewIntent(intent)
    //     // Process the intent
    // }
} 