package com.helly.psaimmotool.utils

import com.helly.psaimmotool.R

object VehicleCapabilities {

    data class Capabilities(
        val brandResId: Int = 0,
        val modelResId: Int = 0,
        val brand: String = "",
        val model: String = "",
        val supportsObd2: Boolean,
        val supportsCan: Boolean,
        val supportsKLine: Boolean,
        val compatibleModules: List<String>,
        val defaultFilters: List<String> = emptyList()
    )

    private val capabilitiesMap: MutableMap<Pair<String, String>, Capabilities> = mutableMapOf(

        // --- PEUGEOT ---
        "Peugeot" to "207" to Capabilities(
            R.string.brand_peugeot,
            R.string.model_207,
            supportsCan = true,
            supportsObd2 = true,
            supportsKLine = true,
            compatibleModules = listOf(
                "OBD2 USB",
                "OBD2 Bluetooth",
                "K-Line USB",
                "CANBUS UART",
                "CANBUS",
                "CAN DEMO"
            )
        ),
        "Peugeot" to "307" to Capabilities(
            R.string.brand_peugeot,
            R.string.model_307,
            supportsCan = true,
            supportsObd2 = true,
            supportsKLine = true,
            compatibleModules = listOf(
                "OBD2 USB",
                "OBD2 Bluetooth",
                "K-Line USB",
                "CANBUS",
                "CAN DEMO"
            )
        ),

        // --- DUCATI ---
        "Ducati" to "848" to Capabilities(
            R.string.brand_ducati,
            R.string.model_848,
            supportsCan = true,
            supportsObd2 = true,
            supportsKLine = false,
            compatibleModules = listOf(
                "OBD2 USB",
                "OBD2 Bluetooth",
                "CANBUS UART",
                "CAN DEMO"
            )
        ),
        "Ducati" to "1098" to Capabilities(
            R.string.brand_ducati,
            R.string.model_1098,
            supportsCan = false,
            supportsObd2 = true,
            supportsKLine = false,
            compatibleModules = listOf(
                "OBD2 USB",
                "OBD2 Bluetooth"
            )
        ),
        "Ducati" to "1198" to Capabilities(
            R.string.brand_ducati,
            R.string.model_1198,
            supportsCan = false,
            supportsObd2 = true,
            supportsKLine = false,
            compatibleModules = listOf(
                "OBD2 USB",
                "OBD2 Bluetooth"
            )
        ),

        // --- MG ---
        "MG" to "4" to Capabilities(
            R.string.brand_mg,
            R.string.model_4,
            supportsCan = true,
            supportsObd2 = true,
            supportsKLine = false,
            compatibleModules = listOf(
                "OBD2 USB",
                "OBD2 Bluetooth",
                "CANBUS",
                "CAN DEMO"
            )
        ),
        "MG" to "3" to Capabilities(
            R.string.brand_mg,
            R.string.model_3,
            supportsCan = false,
            supportsObd2 = true,
            supportsKLine = false,
            compatibleModules = listOf("OBD2 USB", "OBD2 Bluetooth")
        ),
        "MG" to "Marvel R" to Capabilities(
            R.string.brand_mg,
            R.string.model_marvel_r,
            supportsCan = true,
            supportsObd2 = true,
            supportsKLine = false,
            compatibleModules = listOf("OBD2 USB", "OBD2 Bluetooth", "CANBUS UART")
        ),

        // --- FORD ---
        "Ford" to "Mustang Mach-E" to Capabilities(
            R.string.brand_ford,
            R.string.model_mustang_mache,
            supportsCan = true,
            supportsObd2 = true,
            supportsKLine = false,
            compatibleModules = listOf(
                "OBD2 USB",
                "OBD2 Bluetooth",
                "CANBUS UART",
                "CANBUS",
                "CAN DEMO"
            )
        ),
        "Ford" to "Mustang (2000+)" to Capabilities(
            R.string.brand_ford,
            R.string.model_mustang_series,
            supportsCan = false,
            supportsObd2 = true,
            supportsKLine = false,
            compatibleModules = listOf("OBD2 USB", "OBD2 Bluetooth")
        ),
        "Ford" to "Kuga" to Capabilities(
            R.string.brand_ford,
            R.string.model_kuga,
            supportsCan = false,
            supportsObd2 = true,
            supportsKLine = false,
            compatibleModules = listOf("OBD2 USB", "OBD2 Bluetooth")
        ),
        "Ford" to "Puma" to Capabilities(
            R.string.brand_ford,
            R.string.model_puma,
            supportsCan = false,
            supportsObd2 = true,
            supportsKLine = false,
            compatibleModules = listOf("OBD2 USB", "OBD2 Bluetooth")
        ),
        "Ford" to "Fiesta" to Capabilities(
            R.string.brand_ford,
            R.string.model_fiesta,
            supportsCan = false,
            supportsObd2 = true,
            supportsKLine = false,
            compatibleModules = listOf("OBD2 USB", "OBD2 Bluetooth")
        ),

        // --- TOYOTA ---
        "Toyota" to "Corolla" to Capabilities(
            R.string.brand_toyota,
            R.string.model_corolla,
            supportsCan = true,
            supportsObd2 = true,
            supportsKLine = false,
            compatibleModules = listOf("OBD2 USB", "OBD2 Bluetooth", "CANBUS")
        )
    )

    fun getCapabilities(brand: String?, model: String?): Capabilities? {
        if (brand == null || model == null) return null
        return capabilitiesMap[brand to model]
    }

    fun getCompatibleModules(): List<String> {
        val vehicle = VehicleManager.selectedVehicle
        return getCapabilities(vehicle.first, vehicle.second)?.compatibleModules ?: emptyList()
    }
    fun overrideCapabilities(key: String, capabilities: Capabilities) {
        val parts = key.split(" ", limit = 2)
        if (parts.size == 2) {
            val brand = parts[0]
            val model = parts[1]
            capabilitiesMap[brand to model] = capabilities
        }
    }
}
