// core/transport/DemoTransport.kt
package com.helly.psaimmotool.transport

import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.protocol.TransportInterface
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * Transport purement "démo" (simulation), utile pour tes écrans/tests sans matériel.
 */
class DemoTransport : TransportInterface {

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
        // Rien : démo
    }

    override fun startListening(callback: (CanFrame) -> Unit) {
        if (!isConnected) return
        timer?.cancel()
        timer = fixedRateTimer("DemoSimListen", initialDelay = 1500, period = 2500) {
            // Trame simulée (ex: DTC)
            val simulated = CanFrame(
                0x7E8,
                byteArrayOf(
                    0x59, // simulateur arbitraire
                    0x02,
                    0x10,
                    0x0F
                )
            )
            callback(simulated)
        }
    }
}
