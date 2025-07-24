package com.helly.psaimmotool.utils
import com.helly.psaimmotool.R

object VehicleManager {
    var selectedVehicle: Triple<String, String, Int> = Triple("Peugeot", "207", 2008)

    fun setVehicle(marque: String, modele: String, annee: Int) {
        selectedVehicle = Triple(marque, modele, annee)
        UiUpdater.appendLog("\uD83D\uDE97 ${ContextProvider.getString(R.string.report_vehicle)} : $marque $modele $annee")
    }
}
