package com.helly.psaimmotool.utils

import android.widget.TextView

object UiUpdater {

    private var statusText: TextView? = null
    private var outputText: TextView? = null

    /**
     * Initialise les vues à mettre à jour (statut et log de sortie).
     */
    fun init(statusView: TextView, outputView: TextView) {
        statusText = statusView
        outputText = outputView
    }

    /**
     * Ajoute un message dans la zone de logs.
     */
    fun appendLog(message: String) {
        outputText?.append("$message\n")
    }

    /**
     * Efface la zone de logs.
     */
    fun clearLog() {
        outputText?.text = ""
    }

    /**
     * Met à jour l’état de la connexion avec un message et un identifiant de module.
     */
    fun setConnectedStatus(status: String, module: String) {
        statusText?.text = "$status"
        if (module.isNotBlank()) {
            appendLog("📡 $status")
        }
    }
}
