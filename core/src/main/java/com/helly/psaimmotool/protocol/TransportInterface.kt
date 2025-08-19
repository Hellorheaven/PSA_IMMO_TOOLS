// core/protocol/TransportInterface.kt
package com.helly.psaimmotool.protocol

import com.helly.psaimmotool.can.CanFrame

interface TransportInterface {
    fun connect()
    fun disconnect()
    fun sendFrame(frame: String)
    fun startListening(callback: (CanFrame) -> Unit)
}
