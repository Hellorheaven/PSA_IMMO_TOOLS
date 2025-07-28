package com.helly.psaimmotool.utils

import android.content.Context
import android.content.SharedPreferences
import android.widget.TextView
import androidx.preference.PreferenceManager

object UiUpdater {

    private const val KEY_AUTO_SCROLL = "auto_scroll_enabled"

    private var statusText: TextView? = null
    private var outputText: TextView? = null

    fun init(statusView: TextView, outputView: TextView) {
        statusText = statusView
        outputText = outputView
    }

    fun appendLog(outputView: TextView?, message: String) {
        outputView?.append("$message\n")
    }

    fun appendLog(message: String) {
        appendLog(outputText, message)
    }

    fun clearLog(outputView: TextView?) {
        outputView?.text = ""
    }

    fun clearLog() {
        clearLog(outputText)
    }

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

    fun setConnectedStatus(status: String, module: String) {
        setConnectedStatus(statusText, outputText, status, module)
    }

    fun setAutoScrollEnabled(context: Context, enabled: Boolean) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(KEY_AUTO_SCROLL, enabled).apply()
    }

    fun isAutoScrollEnabled(context: Context): Boolean {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(KEY_AUTO_SCROLL, true)
    }
}
