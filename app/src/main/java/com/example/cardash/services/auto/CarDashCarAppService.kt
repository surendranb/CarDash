package com.example.cardash.services.auto

import android.content.Intent
import android.content.pm.ApplicationInfo // Added for FLAG_DEBUGGABLE
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.example.cardash.R // Assuming R class is in com.example.cardash

class CarDashCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            // Allow all hosts in debug mode (for DHU)
            HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
        } else {
            // Production allowlist - ensure R.array.hosts_allowlist_sample is properly defined for release
            HostValidator.Builder(applicationContext)
                .addAllowedHosts(R.array.hosts_allowlist_sample) // Sample allowlist, adjust as needed
                .build()
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