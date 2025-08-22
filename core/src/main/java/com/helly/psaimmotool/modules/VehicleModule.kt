package com.helly.psaimmotool.modules

import com.helly.psaimmotool.protocol.Protocol

class VehicleModule(private val protocol: Protocol) {

    fun connect() = protocol.connect()
    fun disconnect() = protocol.disconnect()

    fun requestVin() = protocol.requestVin()
    fun requestPin() = protocol.requestPin()
    fun requestDtc() = protocol.requestDtc()
    fun startCanListening() = protocol.startListening()
    fun sendCustomFrame(frame: String) = protocol.sendFrame(frame)
}
