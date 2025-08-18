package com.helly.psaimmotool.modules

import android.content.Context
import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.utils.DiagnosticRecorder
import com.helly.psaimmotool.utils.FrameInterpreter
import com.helly.psaimmotool.modules.PsaKeyCalculator

/**
 * Version "Solution 2" :
 * - Aucune référence à R dans :core
 * - Tous les textes passent par reportRes(resId, args...)
 * - Les resId sont injectés par la couche UI (mobile/automotive)
 */
class CanBusModule(
    private val context: Context,
    // resId injectés depuis l'UI (ex: R.string.connected_to, etc.)
    private val resConnectedTo: Int,
    private val resNoModuleConnected: Int,
    private val resPinStepSeedReceived: Int,
    private val resPinStepKeySent: Int,
    private val resPinStepUnlocked: Int,
    private val resPinReceived: Int
) : BaseModule() {

    private var awaitingSeed = false
    private var awaitingPin = false

    override fun connect() {
        reportRes(resConnectedTo, "CANBUS")
        DiagnosticRecorder.setConnectionStatus(true)
    }

    override fun disconnect() {
        reportRes(resNoModuleConnected)
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
        // À implémenter si nécessaire (écoute temps réel, etc.)
    }

    override fun sendCustomFrame(frame: String) {
        // Log brut (pas besoin de traduction)
        report("➡️ $frame")
        DiagnosticRecorder.addRawFrame(frame)

        val parsed = CanFrame.parse(frame)
        handlePinSequenceResponse(parsed)

        val decoded = FrameInterpreter.decode(parsed)
        if (decoded.isNotBlank()) {
            report("✅ $decoded")
            DiagnosticRecorder.addDecodedFrame(decoded)
            if (decoded.contains("DTC", ignoreCase = true)) {
                DiagnosticRecorder.addDtc(decoded)
            }
        }
    }

    private fun handlePinSequenceResponse(frame: CanFrame) {
        val data = frame.data
        if (data.isEmpty()) return

        // 67 01 SS SS → réception du seed
        if (awaitingSeed && data.size >= 4 && data[0] == 0x67.toByte() && data[1] == 0x01.toByte()) {
            awaitingSeed = false
            val seed = byteArrayOf(data[2], data[3])
            val seedStr = seed.joinToString(" ") { "%02X".format(it) }
            reportRes(resPinStepSeedReceived, seedStr)

            val key = PsaKeyCalculator.calculateKey(context, seed)
            val keyStr = key.joinToString(" ") { "%02X".format(it) }

            sendCustomFrame("7E0 04 27 02 $keyStr")
            reportRes(resPinStepKeySent, keyStr)
            awaitingPin = true
            return
        }

        // 67 02 → déverrouillé
        if (awaitingPin && data.size >= 2 && data[0] == 0x67.toByte() && data[1] == 0x02.toByte()) {
            awaitingPin = false
            reportRes(resPinStepUnlocked)
            sendCustomFrame("7E0 03 22 F1 90")
            return
        }

        // 62 F1 90 XX XX ... → PIN ASCII
        if (data.size >= 5 && data[0] == 0x62.toByte() && data[1] == 0xF1.toByte() && data[2] == 0x90.toByte()) {
            val pinBytes = data.slice(3 until data.size).toByteArray()
            val pin = pinBytes.toString(Charsets.US_ASCII).trim()
            reportRes(resPinReceived, pin)
            DiagnosticRecorder.addDecodedFrame("PIN: $pin")
        }
    }
}
