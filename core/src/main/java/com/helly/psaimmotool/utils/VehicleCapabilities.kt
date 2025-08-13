package com.helly.psaimmotool.utils

import com.helly.psaimmotool.R

object VehicleCapabilities {

    data class Capabilities(
        val brandResId: Int = 0,
        val modelResId: Int = 0,
        val brand: String = "",
        val model: String = "",
        val year: Int = 0,
        val supportsObd2: Boolean,
        val supportsCan: Boolean,
        val supportsKLine: Boolean,
        val compatibleModules: List<String>,
        val defaultFilters: List<String> = emptyList()
    )

    private val capabilitiesMap: MutableMap<Triple<String, String, Int>, Capabilities> = mutableMapOf(

        // --- PEUGEOT ---
        Triple("Peugeot", "207", 2008) to Capabilities(
            R.string.brand_peugeot, R.string.model_207,
            supportsCan = true, supportsObd2 = true, supportsKLine = true,
            compatibleModules = listOf(
                "OBD2 (USB)", "OBD2 (Bluetooth)", "K-Line (USB)",
                "CANBUS (USB)", "CANBUS (UART)", "CANBUS (Demo)"
            )
        ),
        Triple("Peugeot", "207", 2010) to Capabilities(
            R.string.brand_peugeot, R.string.model_207,
            supportsCan = true, supportsObd2 = true, supportsKLine = true,
            compatibleModules = listOf(
                "OBD2 (USB)", "OBD2 (Bluetooth)", "K-Line (USB)",
                "CANBUS (USB)", "CANBUS (UART)", "CANBUS (Demo)"
            )
        ),
        Triple("Peugeot", "307", 2007) to Capabilities(
            R.string.brand_peugeot, R.string.model_307,
            supportsCan = true, supportsObd2 = true, supportsKLine = true,
            compatibleModules = listOf(
                "OBD2 (USB)", "OBD2 (Bluetooth)", "K-Line (USB)",
                "CANBUS (USB)", "CANBUS (UART)", "CANBUS (Demo)"
            )
        ),

        // --- DUCATI ---
        Triple("Ducati", "848", 2009) to Capabilities(
            R.string.brand_ducati, R.string.model_848,
            supportsCan = true, supportsObd2 = true, supportsKLine = false,
            compatibleModules = listOf(
                "OBD2 (USB)", "OBD2 (Bluetooth)",
                "CANBUS (USB)", "CANBUS (UART)", "CANBUS (Demo)"
            )
        ),
        Triple("Ducati", "1098", 2009) to Capabilities(
            R.string.brand_ducati, R.string.model_1098,
            supportsCan = false, supportsObd2 = true, supportsKLine = false,
            compatibleModules = listOf("OBD2 (USB)", "OBD2 (Bluetooth)")
        ),
        Triple("Ducati", "1198", 2010) to Capabilities(
            R.string.brand_ducati, R.string.model_1198,
            supportsCan = false, supportsObd2 = true, supportsKLine = false,
            compatibleModules = listOf("OBD2 (USB)", "OBD2 (Bluetooth)")
        ),

        // --- MG ---
        Triple("MG", "4", 2022) to Capabilities(
            R.string.brand_mg, R.string.model_4,
            supportsCan = true, supportsObd2 = true, supportsKLine = false,
            compatibleModules = listOf(
                "OBD2 (USB)", "OBD2 (Bluetooth)",
                "CANBUS (USB)", "CANBUS (UART)", "CANBUS (Demo)"
            )
        ),
        Triple("MG", "3", 2018) to Capabilities(
            R.string.brand_mg, R.string.model_3,
            supportsCan = false, supportsObd2 = true, supportsKLine = false,
            compatibleModules = listOf("OBD2 (USB)", "OBD2 (Bluetooth)")
        ),
        Triple("MG", "Marvel R", 2021) to Capabilities(
            R.string.brand_mg, R.string.model_marvel_r,
            supportsCan = true, supportsObd2 = true, supportsKLine = false,
            compatibleModules = listOf("OBD2 (USB)", "OBD2 (Bluetooth)", "CANBUS (UART)")
        ),

        // --- FORD ---
        Triple("Ford", "Mustang Mach-E", 2021) to Capabilities(
            R.string.brand_ford, R.string.model_mustang_mache,
            supportsCan = true, supportsObd2 = true, supportsKLine = false,
            compatibleModules = listOf(
                "OBD2 (USB)", "OBD2 (Bluetooth)",
                "CANBUS (USB)", "CANBUS (UART)", "CANBUS (Demo)"
            )
        ),
        Triple("Ford", "Mustang (2000+)", 2000) to Capabilities(
            R.string.brand_ford, R.string.model_mustang_series,
            supportsCan = false, supportsObd2 = true, supportsKLine = false,
            compatibleModules = listOf("OBD2 (USB)", "OBD2 (Bluetooth)")
        ),
        Triple("Ford", "Kuga", 2019) to Capabilities(
            R.string.brand_ford, R.string.model_kuga,
            supportsCan = false, supportsObd2 = true, supportsKLine = false,
            compatibleModules = listOf("OBD2 (USB)", "OBD2 (Bluetooth)")
        ),
        Triple("Ford", "Puma", 2020) to Capabilities(
            R.string.brand_ford, R.string.model_puma,
            supportsCan = false, supportsObd2 = true, supportsKLine = false,
            compatibleModules = listOf("OBD2 (USB)", "OBD2 (Bluetooth)")
        ),
        Triple("Ford", "Fiesta", 2017) to Capabilities(
            R.string.brand_ford, R.string.model_fiesta,
            supportsCan = false, supportsObd2 = true, supportsKLine = false,
            compatibleModules = listOf("OBD2 (USB)", "OBD2 (Bluetooth)")
        ),

        // --- TOYOTA ---
        Triple("Toyota", "Corolla", 2020) to Capabilities(
            R.string.brand_toyota, R.string.model_corolla,
            supportsCan = true, supportsObd2 = true, supportsKLine = false,
            compatibleModules = listOf("OBD2 (USB)", "OBD2 (Bluetooth)", "CANBUS (USB)")
        )
    )

    fun getAllBrands(): List<String> = capabilitiesMap.keys.map { it.first }.distinct().sorted()
    fun getModelsForBrand(brand: String): List<String> =
        capabilitiesMap.keys.filter { it.first == brand }.map { it.second }.distinct().sorted()
    fun getYearsForModel(brand: String, model: String): List<Int> =
        capabilitiesMap.keys.filter { it.first == brand && it.second == model }
            .map { it.third }.distinct().sorted()

    fun getCapabilities(brand: String?, model: String?, year: Int?): Capabilities? {
        if (brand == null || model == null || year == null) return null
        return capabilitiesMap[Triple(brand, model, year)]
    }
    fun overrideCapabilities(key: Triple<String, String, Int>, capabilities: Capabilities) {
        capabilitiesMap[key] = capabilities
    }

    fun getCompatibleModules(): List<String> {
        val vehicle = VehicleManager.selectedVehicle
        return getCapabilities(vehicle.first, vehicle.second, vehicle.third)?.compatibleModules ?: emptyList()
    }

}
