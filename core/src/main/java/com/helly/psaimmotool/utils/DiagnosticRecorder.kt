package com.helly.psaimmotool.utils

object DiagnosticRecorder {
    private val decodedLines = mutableListOf<String>()
    private val dtcList = mutableListOf<String>()

    fun addDecodedInfo(info: String) {
        decodedLines.add(info)
    }

    fun addDtc(code: String) {
        dtcList.add(code)
    }

    fun getDecodedSummary(): String {
        return if (decodedLines.isEmpty()) "Aucune donnée PID ou CAN décodée."
        else decodedLines.joinToString(separator = "\n")
    }

    fun getDtcSummary(): String {
        return if (dtcList.isEmpty()) "Aucun code défaut détecté."
        else dtcList.joinToString(separator = "\n") { "❗ $it" }
    }
    private var connectionOk = false
    private val rawFrames = mutableListOf<String>()
    private val decodedFrames = mutableListOf<String>()

    fun setConnectionStatus(success: Boolean) {
        connectionOk = success
    }

    fun addRawFrame(frame: String) {
        rawFrames.add(frame)
    }

    fun addDecodedFrame(decoded: String) {
        decodedFrames.add(decoded)
    }

    fun clear() {
        decodedLines.clear()
        dtcList.clear()
    }
}
