package com.helly.psaimmotool.modules

import android.content.Context
import android.hardware.usb.*
import android.util.Log
import com.helly.psaimmotool.R
import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.utils.*

class Obd2UsbModule(private val context: Context) : BaseModule() {

    private val moduleTAG = "Obd2UsbModule"
    private var device: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private var inputEndpoint: UsbEndpoint? = null
    private var outputEndpoint: UsbEndpoint? = null
    private var isConnected = false

    override fun connect() {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList
        device = deviceList.values.firstOrNull()

        if (device == null) {
            UiUpdater.appendLog(context.getString(R.string.no_usb_found))
            return
        }

        if (!usbManager.hasPermission(device)) {
            UiUpdater.appendLog(context.getString(R.string.error_usb_permission))
            return
        }

        val usbInterface = device!!.getInterface(0)
        inputEndpoint = usbInterface.getEndpoint(0)
        outputEndpoint = usbInterface.getEndpoint(1)

        connection = usbManager.openDevice(device)
        connection?.claimInterface(usbInterface, true)

        isConnected = true
        UiUpdater.setConnectedStatus(context.getString(R.string.connected_to, "OBD2 USB"), "OBD2 USB")
        DiagnosticRecorder.setConnectionStatus(true)

        Thread {
            val buffer = ByteArray(64)
            while (isConnected) {
                try {
                    val len = connection?.bulkTransfer(inputEndpoint, buffer, buffer.size, 200)
                    if (len != null && len > 0) {
                        val hexString = buffer.copyOf(len).joinToString(" ") { String.format("%02X", it) }
                        UiUpdater.appendLog("⬅️ $hexString")
                        DiagnosticRecorder.addRawFrame(hexString)

                        val frame = CanFrame.parse(hexString)
                        val interpreted = FrameInterpreter.decode(frame)

                        if (interpreted.isNotBlank()) {
                            UiUpdater.appendLog("✅ $interpreted")
                            DiagnosticRecorder.addDecodedFrame(interpreted)

                            if (interpreted.contains("DTC") || interpreted.contains("code défaut")) {
                                DiagnosticRecorder.addDtc(interpreted)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(moduleTAG, "Erreur lecture USB : ${e.message}")
                }
            }
        }.start()
    }

    override fun sendCustomFrame(frame: String) {
        if (!isConnected || connection == null || outputEndpoint == null) {
            UiUpdater.appendLog(context.getString(R.string.no_module_connected))
            return
        }

        val frameBytes = frame.trim().split(" ").mapNotNull {
            try {
                it.toInt(16).toByte()
            } catch (_: Exception) {
                null
            }
        }.toByteArray()

        val sent = connection!!.bulkTransfer(outputEndpoint, frameBytes, frameBytes.size, 1000)
        if (sent > 0) {
            UiUpdater.appendLog("➡️ $frame")
        } else {
            UiUpdater.appendLog(context.getString(R.string.error_sending_frame))
        }
    }

    override fun disconnect() {
        isConnected = false
        try {
            connection?.close()
        } catch (_: Exception) {}
        connection = null
        device = null
        UiUpdater.setConnectedStatus(context.getString(R.string.no_module_connected), "")
    }

    override fun requestVin() {
        sendCustomFrame("7DF 02 01 02")
    }

    override fun requestPin() {
        sendCustomFrame("7DF 02 27 01")
    }

    override fun startCanListening() {
        // La lecture est faite en continu dans le thread USB
    }

    override fun readDtc() {
        sendCustomFrame("7DF 02 03 00")
    }
}
