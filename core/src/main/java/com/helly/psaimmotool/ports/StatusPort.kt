package com.helly.psaimmotool.ports

/**
 * Petit port optionnel pour relayer l'état et les logs vers Android Auto
 * sans renommer vos classes/fonctions existantes.
 */
interface StatusPort {
    fun setStatus(text: String)
    fun appendLog(line: String)
    fun appendOutput(line: String)
}
