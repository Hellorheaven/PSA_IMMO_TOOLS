// core/protocol/Obd2Protocol.kt
package com.helly.psaimmotool.protocol

import com.helly.psaimmotool.utils.DiagnosticRecorder
import com.helly.psaimmotool.utils.FrameInterpreter
import com.helly.psaimmotool.can.CanFrame

class Obd2Protocol(private val transport: TransportInterface) {

    private var reporter: Reporter? = null

    fun withReporter(r: Reporter?): Obd2Protocol {
        this.reporter = r
        return this
    }

    fun connect() {
        transport.connect()
        reporter?.setStatus("Connecté OBD2", "OBD2")
        DiagnosticRecorder.setConnectionStatus(true)
    }

    fun disconnect() {
        transport.disconnect()
        reporter?.setStatus("Déconnecté", "")
    }

    fun requestVin() {
        sendFrame("09 02") // PID VIN
    }

    fun requestDtc() {
        sendFrame("03") // Lecture DTC
    }

    fun sendFrame(frame: String) {
        reporter?.log("➡️ OBD2 $frame")
        DiagnosticRecorder.addRawFrame(frame)

        // Décodage de trame CAN → via FrameInterpreter
        val parsed = CanFrame.parse(frame)
        val decoded = FrameInterpreter.decode(parsed)
        if (decoded.isNotBlank()) {
            reporter?.log("✅ $decoded")
            DiagnosticRecorder.addDecodedFrame(decoded)
        }
    }
}
