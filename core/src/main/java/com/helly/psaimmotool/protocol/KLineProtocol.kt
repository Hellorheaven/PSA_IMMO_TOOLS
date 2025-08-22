package com.helly.psaimmotool.protocol

import com.helly.psaimmotool.utils.DiagnosticRecorder

class KLineProtocol(private val transport: TransportInterface) : Protocol {

    private var reporter: Reporter? = null

    override fun withReporter(r: Reporter?): Protocol {
        this.reporter = r
        return this
    }

    override fun connect() {
        transport.connect()
        reporter?.setStatus("Connecté au K-Line", "KLine")
        DiagnosticRecorder.setConnectionStatus(true)
    }

    override fun disconnect() {
        transport.disconnect()
        reporter?.setStatus("Déconnecté", "")
    }

    override fun requestVin() {
        sendFrame("81 10 F1 81")
    }

    override fun requestPin() {
        sendFrame("81 12 F1 27 01")
    }

    override fun requestDtc() {
        sendFrame("81 12 F1 03")
    }

    override fun sendFrame(frame: String) {
        reporter?.log("➡️ $frame")
        DiagnosticRecorder.addRawFrame(frame)

        if (frame.contains("81")) {
            reporter?.log("VIN reçu (simulé)")
            DiagnosticRecorder.addDecodedFrame("VIN: DEMO123456")
        }
    }

    override fun startListening() {
        transport.startListening { frame ->
            reporter?.log("📡 ${frame.toString()}")
            DiagnosticRecorder.addRawFrame(frame.toString())
        }
    }
}
