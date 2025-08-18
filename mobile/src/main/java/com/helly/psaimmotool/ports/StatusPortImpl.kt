package com.helly.psaimmotool.ports

import android.content.Context
import com.helly.psaimmotool.utils.UiUpdater

/**
 * Implémentation côté mobile : formatte via getString(...) et envoie vers l'UI.
 */
class StatusPortImpl(private val context: Context) : StatusPort {

    override fun appendLog(line: String) {
        UiUpdater.appendLog(line)
    }

    override fun appendLogRes(resId: Int, vararg args: Any) {
        val msg = context.getString(resId, *args)
        UiUpdater.appendLog(msg)
    }

//    override fun appendOutput(line: String) {
//        UiUpdater.appendOutput(line)
//    }

//    override fun setStatus(text: String) {
//        UiUpdater.setStatus(text)
//    }

    override fun setConnectedStatus(text: String, module: String) {
        UiUpdater.setConnectedStatus(text, module)
    }
}