// core/modules/BaseModule.kt
package com.helly.psaimmotool.modules

import android.os.Handler
import android.os.Looper
import com.helly.psaimmotool.protocol.Reporter
import com.helly.psaimmotool.utils.DiagnosticRecorder
import java.util.concurrent.Executors

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

    open fun connect() {}
    open fun disconnect() {}
    open fun requestVin() {}
    open fun requestPin() {}
    open fun readDtc() {}
    open fun startCanListening() {}
    open fun sendCustomFrame(frame: String) {}

    /** Lance un écouteur générique simulé. */
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

    /** Helpers */
    protected fun report(line: String) {
        reporter?.log(line)
    }
    protected fun reportRes(resId: Int, vararg args: Any) {
        reporter?.logRes(resId, *args)
    }

    open fun stopListening() { listening = false }
}
