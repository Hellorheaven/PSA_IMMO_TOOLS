package com.helly.psaimmotool.automotive

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class MainSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = MainScreen(carContext)
}
