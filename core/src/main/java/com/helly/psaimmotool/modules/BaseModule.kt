package com.helly.psaimmotool.modules

import android.os.Handler
import android.os.Looper
//import com.helly.psaimmotool.utils.UiUpdater
import com.helly.psaimmotool.utils.DiagnosticRecorder
//import com.helly.psaimmotool.R
import java.util.concurrent.Executors

abstract class BaseModule {

    private var listening = false
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

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
     * Peut être utilisé par défaut dans les modules qui ne gèrent pas leur propre thread.
     */
    protected fun startListening(context: android.content.Context) {
        if (listening) return
        listening = true
        executor.execute {
            while (listening) {
                try {
                    Thread.sleep(3000)
                    val fakeFrame = "Simulated CAN Frame ID:672 DATA:0A FF"
                    handler.post {
                        val log = context.getString(R.string.simulated_frame_received, fakeFrame)
                        statusPort?.appendLog(log)
                        DiagnosticRecorder.addRawFrame(fakeFrame)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }
    private var statusPort: com.helly.psaimmotool.ports.StatusPort? = null

    fun withStatusPort(port: com.helly.psaimmotool.ports.StatusPort?): BaseModule {
        this.statusPort = port
        return this
    }

    protected fun report(line: String) {
        try {
            statusPort?.appendOutput(line)
        } catch (_: Throwable) {}
        try { statusPort?.appendLog(line) } catch (_: Throwable) {}
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
