package com.helly.psaimmotool.can

import com.helly.psaimmotool.ports.StatusPort
import java.util.*
import kotlin.concurrent.fixedRateTimer

class UsbCanInterface(
    private val statusPort: StatusPort?, // injecté depuis mobile
    private val resIdConnected: Int,
    private val resIdDisconnected: Int,
    private val resIdSend: Int,
    private val resIdListeningStarted: Int,
    private val resIdSimulatedFrame: Int
) : CanCommunicationInterface {

    private var isConnected = false
    private var timer: Timer? = null
    private var callback: ((CanFrame) -> Unit)? = null

    override fun connect() {
        isConnected = true
        statusPort?.appendLogRes(resIdConnected)
    }

    override fun disconnect() {
        isConnected = false
        timer?.cancel()
        statusPort?.appendLogRes(resIdDisconnected)
    }

    override fun sendFrame(frame: CanFrame) {
        if (!isConnected) return
        statusPort?.appendLogRes(resIdSend, frame.toHexString())
    }

    override fun startListening(callback: (CanFrame) -> Unit) {
        if (!isConnected) return
        this.callback = callback

        timer = fixedRateTimer("CanSimListen", initialDelay = 2000, period = 3000) {
            val simulated = CanFrame(
                0x7E8,
                byteArrayOf(
                    0x62, 0xF1.toByte(), 0x90.toByte(),
                    '1'.code.toByte(), '2'.code.toByte(), '3'.code.toByte(), '4'.code.toByte()
                )
            )
            callback(simulated)
            statusPort?.appendLogRes(resIdSimulatedFrame, simulated.toHexString())
        }

        statusPort?.appendLogRes(resIdListeningStarted)
    }
}
