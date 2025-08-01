package com.helly.psaimmotool.utils
import android.content.Context
import android.content.SharedPreferences
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.helly.psaimmotool.*
import androidx.core.content.edit

object UiUpdater {

    private var statusText: TextView? = null
    private var outputText: TextView? = null

    private const val KEY_AUTO_SCROLL = "auto_scroll_enabled"

    fun init(statusView: TextView, outputView: TextView) {
        statusText = statusView
        outputText = outputView
    }



    fun appendLog(outputText: TextView?,message: String) {
        outputText?.append("$message\n")
        autoScrollIfEnabled()
    }

    fun appendLog(message: String) {
        outputText?.append("$message\n")
        autoScrollIfEnabled()
    }

    fun clearLog(context: Context) {
        outputText?.text = ""
        appendLog(context.getString(R.string.logs_cleared))
    }

    fun setConnectedStatus(status: String, module: String) {
        statusText?.text = status
        if (module.isNotBlank()) {
            appendLog("📡 $status")
        }
    }

    fun setAutoScrollEnabled(context: Context, enabled: Boolean) {
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit { putBoolean(KEY_AUTO_SCROLL, enabled) }
    }

//    fun isAutoScrollEnabled(context: Context): Boolean {
//        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
 //       return prefs.getBoolean(KEY_AUTO_SCROLL, true)
 //   }

    private fun autoScrollIfEnabled() {
        // Peut être enrichi pour scroll automatique
        // Exemple : scrollView.fullScroll(View.FOCUS_DOWN)
    }
}
