package com.helly.psaimmotool.modules

import android.os.Handler
import android.os.Looper
import com.helly.psaimmotool.protocol.Reporter
import com.helly.psaimmotool.utils.DiagnosticRecorder
import java.util.concurrent.Executors

/**
 * Classe de base pour tous les modules (CAN, K-Line, OBD2…).
 * Ne contient aucune logique matérielle → chaque module doit
 * implémenter connect(), sendCustomFrame(), etc.
 *
 * Tous les logs / statuts passent par [reporter] (StatusReporter).
 */
abstract class BaseModule {

    private var listening = false
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    // Reporter injecté par la couche supérieure (mobile / automotive)
    protected var reporter: Reporter? = null

    fun withReporter(port: Reporter?): BaseModule {
        this.reporter = port
        return this
    }

    /** À surcharger par les sous-classes */
    open fun connect() {}
    open fun disconnect() {}
    open fun requestVin() {}
    open fun requestPin() {}
    open fun readDtc() {}
    open fun startCanListening() {}
    open fun sendCustomFrame(frame: String) {}

    /**
     * Lance un écouteur générique simulé.
     * Le multilingue est géré par logRes(resId, args).
     */
    protected fun startListening(resIdSimulatedFrame: Int) {
        if (listening) return
        listening = true
        executor.execute {
            while (listening) {
                try {
                    Thread.sleep(3000)
                    val fakeFrame = "Simulated CAN Frame ID:672 DATA:0A FF"
                    handler.post {
                        reporter?.logRes(resIdSimulatedFrame, fakeFrame)
                        DiagnosticRecorder.addRawFrame(fakeFrame)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }

    /** Helpers de logging */
    protected fun report(line: String) {
        reporter?.log(line)
    }
    protected fun reportRes(resId: Int, vararg args: Any) {
        reporter?.logRes(resId, *args)
    }

    /** Helper de statut (connecté/déconnecté) */
    protected fun reportStatus(text: String, module: String) {
        reporter?.setStatus(text, module)
    }

    open fun stopListening() { listening = false }
}
