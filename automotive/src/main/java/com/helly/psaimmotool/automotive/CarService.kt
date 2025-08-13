package com.helly.psaimmotool.automotive

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator

class CarService : CarAppService() {
    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR // usage perso / dev

    override fun onCreateSession(): Session = MainSession()
}
