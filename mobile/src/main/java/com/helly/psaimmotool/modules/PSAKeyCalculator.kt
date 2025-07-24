package com.helly.psaimmotool.utils

import android.content.Context
import com.helly.psaimmotool.R

object PsaKeyCalculator {

    data class KeyParams(
        val appKey: String,
        val sec1: IntArray,
        val sec2: IntArray
    )

    // Dernière opération (pour affichage dans rapport)
    var lastCalculation: Pair<String, String>? = null

    // Base des véhicules pris en charge
    private val seedKeyDatabase: Map<Triple<String, String, Int>, KeyParams> = mapOf(
        Triple("Peugeot", "207", 2008) to KeyParams("D91C", intArrayOf(0xB2, 0x3F, 0xAA), intArrayOf(0xB1, 0x02, 0xAB)),
        Triple("Peugeot", "207", 2010) to KeyParams("D91C", intArrayOf(0xB2, 0x3F, 0xAA), intArrayOf(0xB1, 0x02, 0xAB)),
        Triple("Peugeot", "307", 2007) to KeyParams("1A2B", intArrayOf(0xA9, 0x01, 0xAC), intArrayOf(0xB1, 0x02, 0xAB)),
        Triple("Citroën", "C3", 2009) to KeyParams("A55A", intArrayOf(0xB2, 0x3F, 0xAA), intArrayOf(0xB1, 0x02, 0xAB)),
        Triple("DS", "DS3", 2015) to KeyParams("1A2B", intArrayOf(0xB2, 0x3F, 0xAA), intArrayOf(0xB1, 0x02, 0xAB)),
        Triple("Toyota", "ProAce", 2014) to KeyParams("C00L", intArrayOf(0xB2, 0x3F, 0xAA), intArrayOf(0xB1, 0x02, 0xAB)),
        Triple("Ducati", "848", 2009) to KeyParams("BEEF", intArrayOf(0xB2, 0x3F, 0xAA), intArrayOf(0xB1, 0x02, 0xAB))
    )



    fun hasKeyAlgoFor(vehicle: Triple<String, String, Int>): Boolean {
        return seedKeyDatabase.containsKey(vehicle)
    }

    fun calculateKey(context: Context, seed: ByteArray): ByteArray {
        fun transform(data: Int, sec: IntArray): Int {
            var d = data
            if (d > 32767) d = -(32768 - (d % 32768))
            var result = (((d % sec[0]) * sec[2]) and 0x0FFFFFFF) -
                    (((d / sec[0]) and 0xFFFFFFFF.toInt()) * sec[1] and 0x0FFFFFFF)
            result = result and 0xFFFFFFFF.toInt()
            if (result < 0) result += (sec[0] * sec[2]) + sec[1]
            return result and 0xFFFF
        }

        val vehicle = VehicleManager.selectedVehicle
        val params = seedKeyDatabase[vehicle]

        if (params == null) {
            UiUpdater.appendLog(context.getString(R.string.pin_step_no_key_algo))
            return byteArrayOf(0x00, 0x00)
        }

        val seedHex = seed.joinToString("") { "%02X".format(it) }.padStart(8, '0')
        val s0 = seedHex.substring(0, 2)
        val s1 = seedHex.substring(2, 4)
        val s2 = seedHex.substring(4, 6)
        val s3 = seedHex.substring(6, 8)

        val ak0 = params.appKey.substring(0, 2)
        val ak1 = params.appKey.substring(2, 4)

        val resMsb = transform((ak0 + ak1).toInt(16), params.sec1) or
                transform((s0 + s3).toInt(16), params.sec2)
        val resLsb = transform((s1 + s2).toInt(16), params.sec1) or
                transform(resMsb, params.sec2)

        val result = ((resMsb and 0xFFFF) shl 16) or (resLsb and 0xFFFF)
        val hex = result.toUInt().toString(16).padStart(8, '0').uppercase()

        lastCalculation = Pair(
            seed.joinToString(" ") { "%02X".format(it) },
            hex.chunked(2).joinToString(" ")
        )

        return byteArrayOf(
            hex.substring(0, 2).toInt(16).toByte(),
            hex.substring(2, 4).toInt(16).toByte()
        )

    }
    fun addVehicleKey(vehicle: Triple<String, String, Int>, appKey: String) {
        val defaultSec1 = intArrayOf(0xB2, 0x3F, 0xAA)
        val defaultSec2 = intArrayOf(0xB1, 0x02, 0xAB)
        val newEntry = KeyParams(appKey, defaultSec1, defaultSec2)

        // Clone la map mutable
        val updated = seedKeyDatabase.toMutableMap()
        updated[vehicle] = newEntry

        // Remplace la map (non idéale, mais simple ici)
        @Suppress("UNCHECKED_CAST")
        val field = PsaKeyCalculator::class.java.getDeclaredField("seedKeyDatabase")
        field.isAccessible = true
        field.set(this, updated)
    }
}
