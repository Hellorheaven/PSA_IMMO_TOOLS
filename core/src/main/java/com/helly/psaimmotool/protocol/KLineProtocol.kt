// core/protocol/KLineProtocol.kt
package com.helly.psaimmotool.protocol

import com.helly.psaimmotool.utils.DiagnosticRecorder

class KLineProtocol(private val transport: TransportInterface) {

    private var reporter: Reporter? = null

    fun withReporter(r: Reporter?): KLineProtocol {
        this.reporter = r
        return this
    }

    fun connect() {
        transport.connect()
        reporter?.setStatus("Connecté au K-Line", "KLine")
        DiagnosticRecorder.setConnectionStatus(true)
    }

    fun disconnect() {
        transport.disconnect()
        reporter?.setStatus("Déconnecté", "")
    }

    fun requestVin() {
        sendFrame("81 10 F1 81")
    }

    fun requestPin() {
        sendFrame("81 12 F1 27 01")
    }
    fun requestDtc() {
        sendFrame("81 12 F1 03")
    }
    fun sendFrame(frame: String) {
        reporter?.log("➡️ $frame")
        DiagnosticRecorder.addRawFrame(frame)

        // Ici tu pourras ajouter ton décodage K-Line spécifique
        // Exemple : réponse VIN
        if (frame.contains("81")) {
            reporter?.log("VIN reçu (simulé)")
            DiagnosticRecorder.addDecodedFrame("VIN: DEMO123456")
        }
    }
}
