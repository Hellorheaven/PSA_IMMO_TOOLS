// core/transport/UsbTransport.kt
package com.helly.psaimmotool.transport

import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.protocol.TransportInterface
import java.util.*
import kotlin.concurrent.fixedRateTimer

class UsbTransport : TransportInterface {

    private var isConnected = false
    private var timer: Timer? = null
    private var callback: ((CanFrame) -> Unit)? = null

    override fun connect() { isConnected = true }
    override fun disconnect() { isConnected = false; timer?.cancel() }
    override fun sendFrame(frame: String) { if (!isConnected) return }

    override fun startListening(callback: (CanFrame) -> Unit) {
        if (!isConnected) return
        this.callback = callback
        timer = fixedRateTimer("CanSimListen", initialDelay = 2000, period = 3000) {
            val simulated = CanFrame(0x7E8, byteArrayOf(0x62, 0xF1.toByte(), 0x90.toByte(),
                '1'.code.toByte(), '2'.code.toByte(), '3'.code.toByte(), '4'.code.toByte()))
            callback(simulated)
        }
    }
}
