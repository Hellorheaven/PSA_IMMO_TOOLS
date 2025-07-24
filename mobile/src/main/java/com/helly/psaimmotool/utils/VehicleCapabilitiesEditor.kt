package com.helly.psaimmotool.utils

object VehicleCapabilitiesEditor {

    fun registerVehicle(
        brand: String,
        model: String,

        supportsObd2: Boolean,
        supportsCan: Boolean,
        supportsKLine: Boolean,
        compatibleModules: List<String>,
        defaultFilters: List<String> = emptyList()
    ) {
        val key = "$brand $model"
        val caps = VehicleCapabilities.Capabilities(
            brand = brand,
            model = model,
            supportsObd2 = supportsObd2,
            supportsCan = supportsCan,
            supportsKLine = supportsKLine,
            compatibleModules = compatibleModules,
            defaultFilters = defaultFilters
        )
        VehicleCapabilities.overrideCapabilities(key, caps)
    }
}
