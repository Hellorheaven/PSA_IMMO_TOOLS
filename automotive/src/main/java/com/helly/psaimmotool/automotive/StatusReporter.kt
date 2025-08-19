package com.helly.psaimmotool.automotive

import android.content.Context
import com.helly.psaimmotool.protocol.Reporter
import com.helly.psaimmotool.ports.StatusPort

class StatusReporter(
    private val context: Context,
    private val port: StatusPort
) : Reporter {

    override fun log(line: String) {
        port.appendLog(line)
    }

    override fun logRes(resId: Int, vararg args: Any) {
        val txt = context.getString(resId, *args)
        port.appendLog(txt)
    }

    override fun setStatus(text: String, module: String) {
        port.setConnectedStatus(text, module)
    }
}