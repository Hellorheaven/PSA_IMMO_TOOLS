package com.helly.psaimmotool.modules

import android.content.Context
import com.helly.psaimmotool.R
import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.utils.*
import java.io.InputStream
import java.io.OutputStream

class CanBusUartModule(private val context: Context) : BaseModule() {

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false

    private var awaitingSeed = false
    private var awaitingPin = false

    override fun connect() {
        isConnected = true
        UiUpdater.setConnectedStatus(context.getString(R.string.connected_to, "CANBUS UART"), "CANBUS UART")
        DiagnosticRecorder.setConnectionStatus(true)

        Thread {
            val buffer = ByteArray(128)
            while (isConnected) {
                try {
                    val len = inputStream?.read(buffer) ?: -1
                    if (len > 0) {
                        val hex = buffer.copyOf(len).joinToString(" ") { "%02X".format(it) }
                        UiUpdater.appendLog("⬅️ $hex")
                        DiagnosticRecorder.addRawFrame(hex)

                        val frame = CanFrame.parse(hex)
                        handlePinSequenceResponse(frame)

                        val decoded = FrameInterpreter.decode(frame)
                        if (decoded.isNotBlank()) {
                            UiUpdater.appendLog("✅ $decoded")
                            DiagnosticRecorder.addDecodedFrame(decoded)
                            if (decoded.contains("DTC", true)) {
                                DiagnosticRecorder.addDtc(decoded)
                            }
                        }
                    }
                } catch (e: Exception) {
                    UiUpdater.appendLog("❌ ${e.message}")
                }
            }
        }.start()
    }

    override fun sendCustomFrame(frame: String) {
        if (!isConnected || outputStream == null) {
            UiUpdater.appendLog(context.getString(R.string.no_module_connected))
            return
        }

        val bytes = frame.trim().split(" ").mapNotNull {
            try { it.toInt(16).toByte() } catch (_: Exception) { null }
        }.toByteArray()

        try {
            outputStream?.write(bytes)
            UiUpdater.appendLog("➡️ $frame")
        } catch (e: Exception) {
            UiUpdater.appendLog("❌ ${e.message}")
        }
    }

    override fun disconnect() {
        isConnected = false
        try { inputStream?.close(); outputStream?.close() } catch (_: Exception) {}
        inputStream = null
        outputStream = null
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
        // Géré par le thread de lecture
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
