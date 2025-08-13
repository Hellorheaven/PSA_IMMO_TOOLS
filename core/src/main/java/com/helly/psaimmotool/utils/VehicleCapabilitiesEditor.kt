package com.helly.psaimmotool.utils

object VehicleCapabilitiesEditor {

    fun registerVehicle(
        brand: String,
        model: String,
        year: Int,
        supportsObd2: Boolean,
        supportsCan: Boolean,
        supportsKLine: Boolean,
        compatibleModules: List<String>,
        defaultFilters: List<String> = emptyList()
    ) {
        val key = Triple(brand, model, year)
        val caps = VehicleCapabilities.Capabilities(
            brand = brand,
            model = model,
            year = year,
            supportsObd2 = supportsObd2,
            supportsCan = supportsCan,
            supportsKLine = supportsKLine,
            compatibleModules = compatibleModules,
            defaultFilters = defaultFilters
        )
        VehicleCapabilities.overrideCapabilities(key, caps)
    }
}

