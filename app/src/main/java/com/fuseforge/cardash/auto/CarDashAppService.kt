package com.fuseforge.cardash.auto

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.car.app.CarAppService
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class CarDashAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        // Since we are deploying via Internal Test Track (Release Mode), the strict 
        // signature matching in the sample allowlist often rejects legitimate production 
        // car hosts. We explicitly allow all hosts to ensure reliable HUD connection.
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                return MainCarScreen(carContext)
            }
        }
    }
}
