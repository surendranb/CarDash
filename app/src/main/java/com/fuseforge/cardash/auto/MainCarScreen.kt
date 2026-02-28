package com.fuseforge.cardash.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

class MainCarScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return MessageTemplate.Builder("CarDash HUD is ready.\n\nPlease start the engine and connect OBD-II via the phone app if not already connected.")
            .setTitle("CarDash OBD")
            .setHeaderAction(Action.APP_ICON)
            .build()
    }
}
