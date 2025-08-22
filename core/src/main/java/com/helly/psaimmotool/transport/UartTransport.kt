// core/transport/UartTransport.kt
package com.helly.psaimmotool.transport

import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.protocol.TransportInterface
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * Transport "UART" simulé, même comportement que UsbTransport (timer de frames).
 * Pas de renommage de méthodes.
 */
class UartTransport : TransportInterface {

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
        // Ici tu enverras réellement sur l’UART quand tu brancheras ton backend
    }

    override fun startListening(callback: (CanFrame) -> Unit) {
        if (!isConnected) return
        timer?.cancel()
        timer = fixedRateTimer("UartSimListen", initialDelay = 2000, period = 3000) {
            // Trame simulée (ex: réponse lecture PIN)
            val simulated = CanFrame(
                0x7E8,
                byteArrayOf(
                    0x62,
                    0xF1.toByte(),
                    0x90.toByte(),
                    '1'.code.toByte(),
                    '2'.code.toByte(),
                    '3'.code.toByte(),
                    '4'.code.toByte()
                )
            )
            callback(simulated)
        }
    }
}
