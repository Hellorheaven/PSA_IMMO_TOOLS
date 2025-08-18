package com.helly.psaimmotool.ports

import androidx.annotation.StringRes
/**
 * Petit port optionnel pour relayer l'état et les logs vers Android Auto
 * sans renommer vos classes/fonctions existantes.
 */
interface StatusPort {
 //   fun setStatus(text: String)
    fun appendLog(line: String)
  //  fun appendOutput(line: String)
    fun setConnectedStatus(text: String, module: String)

    /** Ajoute une ligne de log traduite à partir d'une ressource */
    fun appendLogRes(@StringRes resId: Int, vararg args: Any)

}
