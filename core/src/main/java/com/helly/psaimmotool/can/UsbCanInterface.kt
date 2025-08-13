package com.helly.psaimmotool.can

import android.content.Context
import com.helly.psaimmotool.utils.UiUpdater
import java.util.*
import kotlin.concurrent.fixedRateTimer

class UsbCanInterface(private val context: Context) : CanCommunicationInterface {

    private var isConnected = false
    private var timer: Timer? = null
    private var callback: ((CanFrame) -> Unit)? = null

    override fun connect() {
        isConnected = true
        UiUpdater.appendLog("✅ USB CAN simulé connecté")
    }

    override fun disconnect() {
        isConnected = false
        timer?.cancel()
        UiUpdater.appendLog("🛑 USB CAN simulé déconnecté")
    }

    override fun sendFrame(frame: CanFrame) {
        if (!isConnected) return
        UiUpdater.appendLog("🧪 [CAN Simulé] Envoi : ${frame.toHexString()}")
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
        }

        UiUpdater.appendLog("👂 Écoute CAN simulée démarrée")
    }
}
