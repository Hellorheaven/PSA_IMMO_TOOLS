package com.helly.psaimmotool.modules

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.helly.psaimmotool.R
import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.utils.*
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread

class Obd2BluetoothModule(
    private val context: Context,
    private val device: BluetoothDevice?
) : BaseModule() {

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false

    override fun connect() {
        if (device == null) {
            UiUpdater.appendLog(context.getString(R.string.no_bluetooth_device))
            return
        }

        try {
            socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            socket?.connect()
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream

            isConnected = true
            UiUpdater.setConnectedStatus(context.getString(R.string.connected_to, device.name ?: device.address), "OBD2 Bluetooth")
            DiagnosticRecorder.setConnectionStatus(true)

            thread {
                val buffer = ByteArray(1024)
                while (isConnected && inputStream != null) {
                    try {
                        val bytes = inputStream!!.read(buffer)
                        if (bytes > 0) {
                            val hex = buffer.copyOf(bytes).joinToString(" ") { "%02X".format(it) }
                            UiUpdater.appendLog("⬅️ $hex")
                            DiagnosticRecorder.addRawFrame(hex)

                            val frame = CanFrame.parse(hex)
                            val interpreted = FrameInterpreter.decode(frame)

                            if (interpreted.isNotBlank()) {
                                UiUpdater.appendLog("✅ $interpreted")
                                DiagnosticRecorder.addDecodedFrame(interpreted)

                                if (interpreted.contains("DTC", true) || interpreted.contains("code défaut", true)) {
                                    DiagnosticRecorder.addDtc(interpreted)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        UiUpdater.appendLog("❌ ${e.message}")
                        break
                    }
                }
            }
        } catch (e: Exception) {
            UiUpdater.appendLog("❌ ${e.message}")
        }
    }

    override fun sendCustomFrame(frame: String) {
        if (!isConnected || outputStream == null) {
            UiUpdater.appendLog(context.getString(R.string.no_module_connected))
            return
        }

        val bytes = frame.trim().split(" ").mapNotNull {
            try {
                it.toInt(16).toByte()
            } catch (_: Exception) {
                null
            }
        }.toByteArray()

        try {
            outputStream?.write(bytes)
            UiUpdater.appendLog("➡️ $frame")
        } catch (e: Exception) {
            UiUpdater.appendLog("❌ ${e.message}")
        }
    }

    override fun disconnect() {
        try {
            isConnected = false
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {
        }

        inputStream = null
        outputStream = null
        socket = null

        UiUpdater.setConnectedStatus(context.getString(R.string.no_module_connected), "")
    }

    override fun requestVin() {
        sendCustomFrame("7DF 02 01 02")
    }

    override fun requestPin() {
        sendCustomFrame("7DF 02 27 01")
    }

    override fun startCanListening() {
        // Géré dans le thread de lecture Bluetooth
    }

    override fun readDtc() {
        sendCustomFrame("7DF 02 03 00")
    }
}
