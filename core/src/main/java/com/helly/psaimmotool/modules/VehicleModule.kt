package com.helly.psaimmotool.modules

import com.helly.psaimmotool.protocol.CanProtocol
import com.helly.psaimmotool.protocol.Protocol

class VehicleModule(private val protocol: Protocol) {

    fun connect() = protocol.connect()
    fun disconnect() = protocol.disconnect()

    fun requestVin() = protocol.requestVin()
    fun requestPin() = protocol.requestPin()
    fun requestDtc() = protocol.requestDtc()

    fun startCanListening() {
        when (protocol) {
            is CanProtocol -> protocol.startListening()
            // K-Line/OBD2 : si tu veux un "listen", on pourra l’ajouter plus tard
        }
    }
    fun sendCustomFrame(frame: String) = protocol.sendFrame(frame)
}
