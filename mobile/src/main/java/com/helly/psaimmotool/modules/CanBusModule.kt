package com.helly.psaimmotool.modules

import android.content.Context
import com.helly.psaimmotool.R
import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.utils.*

class CanBusModule(private val context: Context) : BaseModule() {

    private var awaitingSeed = false
    private var awaitingPin = false

    override fun connect() {
        UiUpdater.setConnectedStatus(context.getString(R.string.connected_to, "CANBUS"), "CANBUS")
        DiagnosticRecorder.setConnectionStatus(true)
    }

    override fun disconnect() {
        UiUpdater.setConnectedStatus(context.getString(R.string.no_module_connected), "")
    }

    override fun requestVin() {
        sendCustomFrame("7DF 02 01 02")
    }

    override fun requestPin() {
        awaitingSeed = true
        sendCustomFrame("7E0 02 27 01")
    }

    override fun readDtc() {
        sendCustomFrame("7DF 02 03 00")
    }

    override fun startCanListening() {
        // À implémenter si nécessaire
    }

    override fun sendCustomFrame(frame: String) {
        UiUpdater.appendLog("➡️ $frame")
        DiagnosticRecorder.addRawFrame(frame)

        val parsed = CanFrame.parse(frame)
        handlePinSequenceResponse(parsed)

        val decoded = FrameInterpreter.decode(parsed)
        if (decoded.isNotBlank()) {
            UiUpdater.appendLog("✅ $decoded")
            DiagnosticRecorder.addDecodedFrame(decoded)
            if (decoded.contains("DTC", true)) {
                DiagnosticRecorder.addDtc(decoded)
            }
        }
    }

    private fun handlePinSequenceResponse(frame: CanFrame) {
        val data = frame.data
        if (data.isEmpty()) return

        if (awaitingSeed && data.size >= 4 && data[0] == 0x67.toByte() && data[1] == 0x01.toByte()) {
            awaitingSeed = false
            val seed = byteArrayOf(data[2], data[3])
            val seedStr = seed.joinToString(" ") { "%02X".format(it) }
            UiUpdater.appendLog(context.getString(R.string.pin_step_seed_received, seedStr))

            val key = PsaKeyCalculator.calculateKey(context, seed)
            val keyStr = key.joinToString(" ") { "%02X".format(it) }

            sendCustomFrame("7E0 04 27 02 $keyStr")
            UiUpdater.appendLog(context.getString(R.string.pin_step_key_sent, keyStr))
            awaitingPin = true
            return
        }

        if (awaitingPin && data.size >= 2 && data[0] == 0x67.toByte() && data[1] == 0x02.toByte()) {
            awaitingPin = false
            UiUpdater.appendLog(context.getString(R.string.pin_step_unlocked))
            sendCustomFrame("7E0 03 22 F1 90")
            return
        }

        if (data.size >= 5 && data[0] == 0x62.toByte() && data[1] == 0xF1.toByte() && data[2] == 0x90.toByte()) {
            val pinBytes = data.slice(3 until data.size).toByteArray()
            val pin = pinBytes.toString(Charsets.US_ASCII).trim()
            UiUpdater.appendLog(context.getString(R.string.pin_received, pin))
            DiagnosticRecorder.addDecodedFrame("PIN: $pin")
        }
    }
}
