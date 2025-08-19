package com.helly.psaimmotool.protocol

/**
 * Interface de log/reportage neutre.
 * Le module ne connaît pas Android, juste ce contrat.
 */
interface Reporter {
    fun log(line: String)
    fun logRes(resId: Int, vararg args: Any)
    fun setStatus(text: String, module: String = "")
}