package com.helly.psaimmotool.modules

import android.content.Context
import com.helly.psaimmotool.R
import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.utils.DiagnosticRecorder
import com.helly.psaimmotool.utils.FrameInterpreter
import com.helly.psaimmotool.utils.UiUpdater

class GenericCanDemoModule(private val context: Context) : BaseModule() {

    private var isListening = false
    private var timer: java.util.Timer? = null

    override fun connect() {
        statusPort?.setConnectedStatus(context.getString(R.string.connected_to, "CAN DEMO"), "CAN DEMO")
        DiagnosticRecorder.setConnectionStatus(true)
    }

    override fun disconnect() {
        stopListening()
        UiUpdater.setConnectedStatus(context.getString(R.string.no_module_connected), "")
    }

    override fun sendCustomFrame(frame: String) {
        UiUpdater.appendLog("➡️ $frame")
        simulateFrameReception(frame)
    }

    override fun requestVin() {
        sendCustomFrame("7DF 02 01 02")
    }

    override fun requestPin() {
        sendCustomFrame("7E0 03 22 F1 90")
    }

    override fun readDtc() {
        sendCustomFrame("7DF 02 03 00")
    }

    override fun startCanListening() {
        startListening(context)
    }

    override fun stopListening() {
        stopListening()
    }

    private fun simulateFrameReception(input: String) {
        val parts = input.trim().split(" ")
        if (parts.size < 3) return

        try {
            val id = 0x7E8
            val data = ByteArray(parts.size - 1) {
                parts[it + 1].toInt(16).toByte()
            }
            val frame = CanFrame(id, data)
            handleFrame(frame)
        } catch (_: Exception) {
        }
    }

    private fun handleFrame(frame: CanFrame) {
        val hex = frame.toHexString()
        UiUpdater.appendLog("⬅️ $hex")
        DiagnosticRecorder.addRawFrame(hex)

        val interpreted = FrameInterpreter.decode(frame)
        if (interpreted.isNotBlank()) {
            UiUpdater.appendLog("✅ $interpreted")
            DiagnosticRecorder.addDecodedFrame(interpreted)

            if (interpreted.contains("DTC") || interpreted.contains("code défaut")) {
                DiagnosticRecorder.addDtc(interpreted)
            }
        }
    }
}
