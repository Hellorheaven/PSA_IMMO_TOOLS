package com.helly.psaimmotool

import android.app.Application
import com.helly.psaimmotool.strings.AndroidStringRepository
import com.helly.psaimmotool.strings.Strings

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialisation du module Strings
        Strings.init(AndroidStringRepository(this))
    }
}
