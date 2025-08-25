package com.helly.psaimmotool.utils

object VehicleManager {
    var selectedVehicle: Triple<String, String, Int> = Triple("Peugeot", "207", 2008)

    // Callback optionnel pour notifier l’UI
    var onVehicleChanged: ((Triple<String, String, Int>) -> Unit)? = null

    fun setVehicle(marque: String, modele: String, annee: Int) {
        selectedVehicle = Triple(marque, modele, annee)
        onVehicleChanged?.invoke(selectedVehicle)
    }
}