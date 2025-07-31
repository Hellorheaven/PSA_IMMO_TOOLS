package com.helly.psaimmotool.utils

import com.helly.psaimmotool.R
import com.helly.psaimmotool.can.CanFrame


object FrameInterpreter {

    fun decode(frame: CanFrame): String {
        val id = frame.id
        val data = frame.data

        // DTC - Codes défauts
        if (isDtcFrame(data)) {
            val dtcList = parseDtc(data)
            return ContextProvider.getString(R.string.dtc_found, dtcList.joinToString(", "))
        }

        // VIN
        if (isVinFrame(data)) {
            return decodeVin(data)
        }

        // PIN
        if (isPinFrame(data)) {
            return decodePin(data)
        }

        // Tension batterie
        if (isBatteryVoltageFrame(data)) {
            return decodeVoltage(data)
        }

        // Température moteur
        if (isTemperatureFrame(data)) {
            return decodeTemperature(data)
        }

        // Réponses seed/key
        if (isSeedFrame(data)) {
            return decodeSeed(data)
        }

        // Frame inconnue interprétable
        return ContextProvider.getString(R.string.unknown_frame_response)
    }

    private fun isVinFrame(data: ByteArray): Boolean {
        return data.size >= 4 && data[0] == 0x49.toByte() && data[1] == 0x02.toByte()
    }

    private fun decodeVin(data: ByteArray): String {
        val vin = data.drop(2).map { it.toInt().toChar() }.joinToString("").trim()
        return ContextProvider.getString(R.string.vin_received, vin)
    }

    private fun isPinFrame(data: ByteArray): Boolean {
        return data.size >= 4 && data[0] == 0x62.toByte() && data[1] == 0xF1.toByte() && data[2] == 0x90.toByte()
    }

    private fun decodePin(data: ByteArray): String {
        val pin = data.drop(3).map { it.toInt().toChar() }.joinToString("").trim()
        return ContextProvider.getString(R.string.pin_received, pin)
    }

    private fun isBatteryVoltageFrame(data: ByteArray): Boolean {
        return data.size >= 3 && data[0] == 0x62.toByte() && data[1] == 0x21.toByte() && data[2] == 0x01.toByte()
    }

    private fun decodeVoltage(data: ByteArray): String {
        val raw = data.getOrNull(3)?.toInt() ?: return ""
        val volts = raw / 10.0
        return ContextProvider.getString(R.string.voltage_read, volts)
    }

    private fun isTemperatureFrame(data: ByteArray): Boolean {
        return data.size >= 3 && data[0] == 0x62.toByte() && data[1] == 0x05.toByte()
    }

    private fun decodeTemperature(data: ByteArray): String {
        val raw = data.getOrNull(2)?.toInt() ?: return ""
        val temp = raw - 40
        return ContextProvider.getString(R.string.temperature_read, temp)
    }

    private fun isDtcFrame(data: ByteArray): Boolean {
        return data.isNotEmpty() && data[0] == 0x43.toByte()
    }

    private fun parseDtc(data: ByteArray): List<String> {
        val dtcs = mutableListOf<String>()
        var i = 1
        while (i + 1 < data.size) {
            val code = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            val dtc = formatDtc(code)
            dtcs.add(dtc)
            i += 2
        }
        return dtcs
    }

    private fun formatDtc(code: Int): String {
        val firstChar = when ((code shr 14) and 0x03) {
            0 -> 'P'
            1 -> 'C'
            2 -> 'B'
            3 -> 'U'
            else -> '?'
        }
        val numeric = code and 0x3FFF
        return "$firstChar${String.format("%04d", numeric)}"
    }

    private fun isSeedFrame(data: ByteArray): Boolean {
        return data.size >= 4 && data[0] == 0x67.toByte() && data[1] == 0x01.toByte()
    }

    private fun decodeSeed(data: ByteArray): String {
        val seed = data.slice(2..3).joinToString(" ") { "%02X".format(it) }
        return ContextProvider.getString(R.string.seed_received, seed)
    }
}
