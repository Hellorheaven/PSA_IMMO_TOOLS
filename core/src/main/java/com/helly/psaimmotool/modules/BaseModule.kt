package com.helly.psaimmotool.modules

import android.os.Handler
import android.os.Looper
import com.helly.psaimmotool.utils.DiagnosticRecorder
import com.helly.psaimmotool.ports.StatusPort
import java.util.concurrent.Executors


abstract class BaseModule {

    private var listening = false
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    // Port d'UI injecté par la couche supérieure (mobile / automotive)
    private var statusPort: StatusPort? = null

    fun withStatusPort(port: StatusPort?): BaseModule {
        this.statusPort = port
        return this
    }

    open fun connect() {
        // À redéfinir dans les sous-classes
    }

    open fun requestVin() {
        // À redéfinir si supporté
    }

    open fun requestPin() {
        // À redéfinir si supporté
    }

    open fun startCanListening() {
        // À redéfinir si supporté
    }

    open fun sendCustomFrame(frame: String) {
        // À redéfinir pour gérer l'envoi
    }

    open fun readDtc() {
        // À redéfinir pour lire les DTC
    }

    /**
     * Lance un écouteur générique simulé.
     * Signature conservée: startListening(context: Context)
     * Le multilingue est géré par la couche UI via appendLogRes().
     */
    protected fun startListening(@Suppress("UNUSED_PARAMETER") context: android.content.Context, resIdSimulatedFrame: Int ) {
        if (listening) return
        listening = true
        executor.execute {
            while (listening) {
                try {
                    Thread.sleep(3000)
                    val fakeFrame = "Simulated CAN Frame ID:672 DATA:0A FF"
                    handler.post {
                        // → La traduction/formatage se fait dans l'implémentation de StatusPort
                        statusPort?.appendLogRes(resIdSimulatedFrame, fakeFrame)
                        DiagnosticRecorder.addRawFrame(fakeFrame)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }

    /** Aide : log brut (sans ressource) */
    protected fun report(line: String) {
        try { statusPort?.appendLog(line) } catch (_: Throwable) {}
    }

    /** Aide : log via ressource (multilingue) */
    protected fun reportRes(resId: Int, vararg args: Any) {
        try { statusPort?.appendLogRes(resId, *args) } catch (_: Throwable) {}
    }

    /**
     * Arrête l'écoute du thread générique.
     */
    open fun stopListening() {
        listening = false
    }

    open fun disconnect() {
        // Peut être surchargé par les modules
    }
}
