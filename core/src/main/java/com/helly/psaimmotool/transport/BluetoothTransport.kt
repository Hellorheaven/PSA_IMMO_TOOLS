package com.helly.psaimmotool.transport

import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.protocol.TransportInterface
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * Transport Bluetooth neutre (simulation simple).
 * Tu pourras le relier à ton stack SPP/ELM327 plus tard.
 */
class BluetoothTransport : TransportInterface {

    private var isConnected = false
    private var timer: Timer? = null

    override fun connect() { isConnected = true }

    override fun disconnect() {
        isConnected = false
        timer?.cancel()
        timer = null
    }

    override fun sendFrame(frame: String) {
        if (!isConnected) return
        // TODO : envoyer sur socket BT quand tu brancheras le vrai backend
    }

    override fun startListening(callback: (CanFrame) -> Unit) {
        if (!isConnected) return
        timer?.cancel()
        // Simulation périodique
        timer = fixedRateTimer("BtSimListen", initialDelay = 2000, period = 3000) {
            val simulated = CanFrame(
                0x7E8,
                byteArrayOf(
                    0x62,
                    0xF1.toByte(),
                    0x90.toByte(),
                    'B'.code.toByte(), 'T'.code.toByte(), '0'.code.toByte(), '1'.code.toByte()
                )
            )
            callback(simulated)
        }
    }
}
