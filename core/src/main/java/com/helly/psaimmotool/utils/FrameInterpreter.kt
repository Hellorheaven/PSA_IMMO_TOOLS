package com.helly.psaimmotool.utils

import android.annotation.SuppressLint
import com.helly.psaimmotool.can.CanFrame

object FrameInterpreter {

    fun decode(frame: CanFrame): String {
        val data = frame.data

        return when {
            isDtcFrame(data) -> {
                val dtcList = parseDtc(data)
                "DTC found: ${dtcList.joinToString(", ")}"
            }
            isVinFrame(data) -> decodeVin(data)
            isPinFrame(data) -> decodePin(data)
            isBatteryVoltageFrame(data) -> decodeVoltage(data)
            isTemperatureFrame(data) -> decodeTemperature(data)
            isSeedFrame(data) -> decodeSeed(data)
            else -> "Unknown frame response"
        }
    }

    private fun isVinFrame(data: ByteArray): Boolean {
        return data.size >= 4 && data[0] == 0x49.toByte() && data[1] == 0x02.toByte()
    }

    private fun decodeVin(data: ByteArray): String {
        val vin = data.drop(2).map { it.toInt().toChar() }.joinToString("").trim()
        return "VIN received: $vin"
    }

    private fun isPinFrame(data: ByteArray): Boolean {
        return data.size >= 4 &&
                data[0] == 0x62.toByte() &&
                data[1] == 0xF1.toByte() &&
                data[2] == 0x90.toByte()
    }

    private fun decodePin(data: ByteArray): String {
        val pin = data.drop(3).map { it.toInt().toChar() }.joinToString("").trim()
        return "PIN received: $pin"
    }

    private fun isBatteryVoltageFrame(data: ByteArray): Boolean {
        return data.size >= 3 &&
                data[0] == 0x62.toByte() &&
                data[1] == 0x21.toByte() &&
                data[2] == 0x01.toByte()
    }

    private fun decodeVoltage(data: ByteArray): String {
        val raw = data.getOrNull(3)?.toInt() ?: return ""
        val volts = raw / 10.0
        return "Voltage: $volts V"
    }

    private fun isTemperatureFrame(data: ByteArray): Boolean {
        return data.size >= 3 &&
                data[0] == 0x62.toByte() &&
                data[1] == 0x05.toByte()
    }

    private fun decodeTemperature(data: ByteArray): String {
        val raw = data.getOrNull(2)?.toInt() ?: return ""
        val temp = raw - 40
        return "Engine temperature: $temp °C"
    }

    private fun isDtcFrame(data: ByteArray): Boolean {
        return data.isNotEmpty() && data[0] == 0x43.toByte()
    }

    private fun parseDtc(data: ByteArray): List<String> {
        val dtcs = mutableListOf<String>()
        var i = 1
        while (i + 1 < data.size) {
            val code = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            dtcs.add(formatDtc(code))
            i += 2
        }
        return dtcs
    }

    @SuppressLint("DefaultLocale")
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
        return "Seed received: $seed"
    }
}
