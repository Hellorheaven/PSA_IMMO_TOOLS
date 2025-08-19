// core/modules/VehicleModule.kt
package com.helly.psaimmotool.modules

import com.helly.psaimmotool.protocol.*
import com.helly.psaimmotool.transport.*

class VehicleModule(
    private val protocol: Any, // CanProtocol | KLineProtocol | Obd2Protocol
) {
    fun connect() {
        when (protocol) {
            is CanProtocol -> protocol.connect()
            is KLineProtocol -> protocol.connect()
            is Obd2Protocol -> protocol.connect()
        }
    }

    fun disconnect() {
        when (protocol) {
            is CanProtocol -> protocol.disconnect()
            is KLineProtocol -> protocol.disconnect()
            is Obd2Protocol -> protocol.disconnect()
        }
    }

    fun requestVin() {
        when (protocol) {
            is CanProtocol -> protocol.requestVin()
            is KLineProtocol -> protocol.requestVin()
            is Obd2Protocol -> protocol.requestVin()
        }
    }

    fun requestPin() {
        when (protocol) {
            is CanProtocol -> protocol.requestPin()
            is KLineProtocol -> protocol.requestPin()
        }
    }

    fun requestDtc() {
        when (protocol) {
            is CanProtocol -> protocol.requestDtc()
            is KLineProtocol -> protocol.requestDtc()
            is Obd2Protocol -> protocol.requestDtc()
        }
    }
}
