package com.helly.psaimmotool.protocol

import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.utils.DiagnosticRecorder
import com.helly.psaimmotool.utils.FrameInterpreter
import com.helly.psaimmotool.modules.PsaKeyCalculator

class CanProtocol(private val transport: TransportInterface) : Protocol {

    private var reporter: Reporter? = null
    private var awaitingSeed = false
    private var awaitingPin = false

    override fun withReporter(r: Reporter?): Protocol {
        this.reporter = r
        return this
    }

    override fun connect() {
        transport.connect()
        reporter?.setStatus("Connecté au CAN", "CAN")
        DiagnosticRecorder.setConnectionStatus(true)
    }

    override fun disconnect() {
        transport.disconnect()
        reporter?.setStatus("Déconnecté", "")
    }

    override fun sendFrame(frame: String) {
        reporter?.log("➡️ $frame")
        DiagnosticRecorder.addRawFrame(frame)

        val parsed = CanFrame.parse(frame)
        handlePinSequenceResponse(parsed)

        val decoded = FrameInterpreter.decode(parsed)
        if (decoded.isNotBlank()) {
            reporter?.log("✅ $decoded")
            DiagnosticRecorder.addDecodedFrame(decoded)
            if (decoded.contains("DTC", true)) {
                DiagnosticRecorder.addDtc(decoded)
            }
        }
    }

    override fun requestVin() {
        sendFrame("7DF 02 01 02")
    }

    override fun requestPin() {
        awaitingSeed = true
        sendFrame("7E0 02 27 01")
    }

    override fun requestDtc() {
        sendFrame("7DF 02 03 00")
    }

    override fun startListening() {
        transport.startListening { frame ->
            reporter?.log("📡 ${frame.toString()}")
            DiagnosticRecorder.addRawFrame(frame.toString())

            val decoded = FrameInterpreter.decode(frame)
            if (decoded.isNotBlank()) {
                reporter?.log("✅ $decoded")
                DiagnosticRecorder.addDecodedFrame(decoded)
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
            reporter?.log("Seed reçu: $seedStr")

            val key = PsaKeyCalculator.calculateKey(seed)
            val keyStr = key.joinToString(" ") { "%02X".format(it) }
            sendFrame("7E0 04 27 02 $keyStr")
            reporter?.log("Clé envoyée: $keyStr")
            awaitingPin = true
            return
        }

        if (awaitingPin && data.size >= 2 && data[0] == 0x67.toByte() && data[1] == 0x02.toByte()) {
            awaitingPin = false
            reporter?.log("Déverrouillé ✅")
            sendFrame("7E0 03 22 F1 90")
            return
        }

        if (data.size >= 5 && data[0] == 0x62.toByte() && data[1] == 0xF1.toByte() && data[2] == 0x90.toByte()) {
            val pinBytes = data.slice(3 until data.size).toByteArray()
            val pin = pinBytes.toString(Charsets.US_ASCII).trim()
            reporter?.log("PIN reçu: $pin")
            DiagnosticRecorder.addDecodedFrame("PIN: $pin")
        }
    }
}
