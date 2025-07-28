package com.helly.psaimmotool.utils

import android.content.Context
import android.content.SharedPreferences
import android.widget.TextView
import androidx.preference.PreferenceManager

object UiUpdater {

    private const val KEY_AUTO_SCROLL = "auto_scroll_enabled"

    /**
     * Ajoute un message à la zone de logs.
     */
    fun appendLog(outputView: TextView?, message: String) {
        outputView?.append("$message\n")
    }

    /**
     * Efface la zone de logs.
     */
    fun clearLog(outputView: TextView?) {
        outputView?.text = ""
    }

    /**
     * Met à jour l’état de la connexion avec un message et un identifiant de module.
     */
    fun setConnectedStatus(
        statusView: TextView?,
        outputView: TextView?,
        status: String,
        module: String
    ) {
        statusView?.text = status
        if (module.isNotBlank()) {
            appendLog(outputView, "📡 $status")
        }
    }

    /**
     * Active ou désactive le scroll automatique.
     */
    fun setAutoScrollEnabled(context: Context, enabled: Boolean) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(KEY_AUTO_SCROLL, enabled).apply()
    }

    /**
     * Retourne l’état du scroll automatique.
     */
    fun isAutoScrollEnabled(context: Context): Boolean {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(KEY_AUTO_SCROLL, true) // true par défaut
    }
}
