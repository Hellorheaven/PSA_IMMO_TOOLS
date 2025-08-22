package com.helly.psaimmotool.ui

import android.content.Context
import com.helly.psaimmotool.protocol.Reporter
import com.helly.psaimmotool.utils.UiUpdater

/**
 * Reporter minimal qui délègue à UiUpdater.
 * - log(...) et logRes(...) -> appendLog
 * - setStatus(...) -> on logge aussi (safe, pas de dépendance à StatusPort)
 *
 * NB: on ne renomme rien dans le core. Ce fichier vit côté UI (mobile).
 */
class UiReporter(
    private val context: Context? = null
) : Reporter {

    override fun log(line: String) {
        UiUpdater.appendLog(line)
    }

    override fun logRes(resId: Int, vararg args: Any) {
        val msg = if (context != null) {
            try { context.getString(resId, *args) } catch (_: Exception) { "res:$resId" }
        } else {
            "res:$resId"
        }
        UiUpdater.appendLog(msg)
    }

    override fun setStatus(text: String, module: String) {
        val prefix = if (module.isNotBlank()) "[$module] " else ""
        // On reste non-invasif: on trace dans le log (toujours visible)
        UiUpdater.appendLog(prefix + text)
        // Si jamais UiUpdater possède une API de statut, tu peux décommenter la bonne ligne :
        // UiUpdater.setStatus(prefix + text)
        // UiUpdater.setConnectedStatus(prefix + text)   // seulement si ça existe déjà dans ton UiUpdater
    }
}
