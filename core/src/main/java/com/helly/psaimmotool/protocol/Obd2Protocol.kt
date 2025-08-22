package com.helly.psaimmotool.protocol

import com.helly.psaimmotool.utils.DiagnosticRecorder
import com.helly.psaimmotool.utils.FrameInterpreter
import com.helly.psaimmotool.can.CanFrame

class Obd2Protocol(private val transport: TransportInterface) : Protocol {

    private var reporter: Reporter? = null

    override fun withReporter(r: Reporter?): Protocol {
        this.reporter = r
        return this
    }

    override fun connect() {
        transport.connect()
        reporter?.setStatus("Connecté OBD2", "OBD2")
        DiagnosticRecorder.setConnectionStatus(true)
    }

    override fun disconnect() {
        transport.disconnect()
        reporter?.setStatus("Déconnecté", "")
    }

    override fun requestVin() {
        sendFrame("09 02")
    }

    override fun requestPin() {
        // Pas dispo en OBD2 standard → on ne fait rien
    }

    override fun requestDtc() {
        sendFrame("03")
    }

    override fun sendFrame(frame: String) {
        reporter?.log("➡️ OBD2 $frame")
        DiagnosticRecorder.addRawFrame(frame)

        val parsed = CanFrame.parse(frame)
        val decoded = FrameInterpreter.decode(parsed)
        if (decoded.isNotBlank()) {
            reporter?.log("✅ $decoded")
            DiagnosticRecorder.addDecodedFrame(decoded)
        }
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
}
