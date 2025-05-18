package com.example.cardash.services.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class CarDashSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        //intent.getStringExtra("androidx.car.app.SCREEN_ID") can be used for deep linking
        return MetricsCarScreen(carContext)
    }

    // Optional: Override onNewIntent if your session needs to react to new intents
    // override fun onNewIntent(intent: Intent) {
    //     super.onNewIntent(intent)
    //     // Handle the new intent, possibly navigate to a different screen
    // }
} 