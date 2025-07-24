package com.helly.psaimmotool.modules

import android.content.Context
import android.hardware.usb.*
import com.helly.psaimmotool.R
import com.helly.psaimmotool.can.CanFrame
import com.helly.psaimmotool.utils.*

class KLineUsbModule(private val context: Context) : BaseModule() {

    private var device: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private var input: UsbEndpoint? = null
    private var output: UsbEndpoint? = null
    private var isConnected = false

    override fun connect() {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        device = manager.deviceList.values.firstOrNull()

        if (device == null) {
            UiUpdater.appendLog(context.getString(R.string.no_usb_found))
            return
        }

        if (!manager.hasPermission(device)) {
            UiUpdater.appendLog(context.getString(R.string.error_usb_permission))
            return
        }

        val iface = device!!.getInterface(0)
        input = iface.getEndpoint(0)
        output = iface.getEndpoint(1)

        connection = manager.openDevice(device)
        connection?.claimInterface(iface, true)

        isConnected = true
        UiUpdater.setConnectedStatus(context.getString(R.string.connected_to, "K-Line USB"), "K-Line USB")
        DiagnosticRecorder.setConnectionStatus(true)

        Thread {
            val buffer = ByteArray(64)
            while (isConnected) {
                val len = connection?.bulkTransfer(input, buffer, buffer.size, 200)
                if (len != null && len > 0) {
                    val hex = buffer.copyOf(len).joinToString(" ") { "%02X".format(it) }
                    UiUpdater.appendLog("⬅️ $hex")
                    DiagnosticRecorder.addRawFrame(hex)

                    val decoded = FrameInterpreter.decode(CanFrame.parse(hex))
                    if (decoded.isNotBlank()) {
                        UiUpdater.appendLog("✅ $decoded")
                        DiagnosticRecorder.addDecodedFrame(decoded)
                        if (decoded.contains("DTC")) {
                            DiagnosticRecorder.addDtc(decoded)
                        }
                    }
                }
            }
        }.start()
    }

    override fun sendCustomFrame(frame: String) {
        if (!isConnected || output == null || connection == null) {
            UiUpdater.appendLog(context.getString(R.string.no_module_connected))
            return
        }

        val data = frame.trim().split(" ").mapNotNull {
            try { it.toInt(16).toByte() } catch (_: Exception) { null }
        }.toByteArray()

        val sent = connection!!.bulkTransfer(output, data, data.size, 1000)
        if (sent > 0) {
            UiUpdater.appendLog("➡️ $frame")
        } else {
            UiUpdater.appendLog(context.getString(R.string.error_sending_frame))
        }
    }

    override fun disconnect() {
        isConnected = false
        try { connection?.close() } catch (_: Exception) {}
        connection = null
        UiUpdater.setConnectedStatus(context.getString(R.string.no_module_connected), "")
    }

    override fun requestVin() {
        sendCustomFrame("81 12 F1 01")
    }

    override fun requestPin() {
        sendCustomFrame("81 12 F1 27 01")
    }

    override fun readDtc() {
        sendCustomFrame("81 12 F1 03")
    }

    override fun startCanListening() {
        // Already handled in thread
    }
}
