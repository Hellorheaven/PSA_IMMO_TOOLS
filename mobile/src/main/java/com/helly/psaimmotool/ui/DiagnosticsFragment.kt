// app/src/main/java/com/helly/psaimmotool/ui/DiagnosticsFragment.kt
package com.helly.psaimmotool.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.helly.psaimmotool.*
import com.helly.psaimmotool.modules.*
import com.helly.psaimmotool.utils.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DiagnosticsFragment : Fragment() {

    private lateinit var bluetoothDeviceSpinner: Spinner
    private lateinit var connectButton: Button
    private lateinit var requestVinButton: Button
    private lateinit var requestPinButton: Button
    private lateinit var startCanListenButton: Button
    private lateinit var inputFrameText: EditText
    private lateinit var sendFrameButton: Button
    private lateinit var exportLogsButton: Button
    private lateinit var clearLogsButton: Button
    private lateinit var generateReportButton: Button
    private lateinit var statusText: TextView
    private lateinit var outputText: TextView
    private lateinit var mainScroll: NestedScrollView

    private var currentModule: BaseModule? = null
    private var currentModuleName: String = ""
    private var isConnected = false

    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && !bluetoothDevices.contains(device)) {
                    bluetoothDevices.add(device)
                    val names = bluetoothDevices.map { it.name ?: it.address }
                    bluetoothDeviceSpinner.adapter =
                        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextProvider.init(requireContext().applicationContext)
        DiagnosticRecorder.clear()
        requireContext().registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    override fun onDestroy() {
        super.onDestroy()
        requireContext().unregisterReceiver(bluetoothReceiver)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Layout spécifique du fragment (copie de ton contenu immuable, sans toolbar)
        return inflater.inflate(R.layout.fragment_diagnostics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bluetoothDeviceSpinner = view.findViewById(R.id.bluetoothDeviceSpinner)
        connectButton = view.findViewById(R.id.connectButton)
        requestVinButton = view.findViewById(R.id.requestVinButton)
        requestPinButton = view.findViewById(R.id.requestPinButton)
        startCanListenButton = view.findViewById(R.id.startCanListenButton)
        inputFrameText = view.findViewById(R.id.inputFrameText)
        sendFrameButton = view.findViewById(R.id.sendFrameButton)
        exportLogsButton = view.findViewById(R.id.exportLogsButton)
        clearLogsButton = view.findViewById(R.id.clearLogsButton)
        generateReportButton = view.findViewById(R.id.generateReportButton)
        statusText = view.findViewById(R.id.statusText)
        outputText = view.findViewById(R.id.outputText)
        mainScroll = view.findViewById(R.id.mainScroll)

        UiUpdater.init(statusText, outputText)
        updateVehicleInfoDisplay()

        // Auto-scroll selon Pref
        val prefs = requireContext().getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        val autoScroll = prefs.getBoolean(Prefs.KEY_AUTOSCROLL, true)
        if (autoScroll) {
            outputText.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                mainScroll.post { mainScroll.fullScroll(View.FOCUS_DOWN) }
            }
        }

        setupButtons()
    }

    fun onModuleSelected(name: String) {
        currentModuleName = name
        updateUiForSelectedModule()
        if (currentModuleName == getString(R.string.module_obd2_bluetooth)) {
            startBluetoothDiscovery()
        }
    }

    private fun setupButtons() {
        connectButton.setOnClickListener {
            val selected = currentModuleName
            if (selected.isBlank()) return@setOnClickListener

            isConnected = false
            currentModule = when (selected) {
                getString(R.string.module_obd2_usb) -> Obd2UsbModule(requireContext())
                getString(R.string.module_obd2_bluetooth) -> {
                    val device = bluetoothDevices.getOrNull(bluetoothDeviceSpinner.selectedItemPosition)
                    Obd2BluetoothModule(requireContext(), device)
                }
                getString(R.string.module_kline_usb) -> KLineUsbModule(requireContext())
                getString(R.string.module_canbus) -> CanBusModule(requireContext())
                getString(R.string.module_canbus_uart) -> CanBusUartModule(requireContext())
                getString(R.string.module_can_demo) -> GenericCanDemoModule(requireContext())
                else -> null
            }
            currentModule?.connect()
            isConnected = true
        }

        requestVinButton.setOnClickListener { currentModule?.requestVin() }

        requestPinButton.setOnClickListener {
            val vehicle = VehicleManager.selectedVehicle
            if (!PsaKeyCalculator.hasKeyAlgoFor(vehicle)) {
                Toast.makeText(requireContext(), getString(R.string.no_key_algo_for_vehicle), Toast.LENGTH_LONG).show()
            }
            currentModule?.requestPin()
        }

        startCanListenButton.setOnClickListener { currentModule?.startCanListening() }

        sendFrameButton.setOnClickListener {
            val frame = inputFrameText.text.toString()
            UiUpdater.appendLog("\u2B06\uFE0F $frame")
            currentModule?.sendCustomFrame(frame)
        }

        exportLogsButton.setOnClickListener {
            val content = outputText.text.toString()
            LogExporter.exportLogs(requireContext(), content)
        }

        clearLogsButton.setOnClickListener {
            outputText.text = ""
            UiUpdater.appendLog(getString(R.string.logs_cleared))
        }

        generateReportButton.setOnClickListener {
            generateDiagnosticReport()
        }
    }

    private fun updateUiForSelectedModule() {
        val isBt = currentModuleName == getString(R.string.module_obd2_bluetooth)
        bluetoothDeviceSpinner.visibility = if (isBt) View.VISIBLE else View.GONE

        val supportsPin = supportsPin(currentModuleName)
        val algoAvailable = PsaKeyCalculator.hasKeyAlgoFor(VehicleManager.selectedVehicle)
        requestPinButton.visibility = if (supportsPin && algoAvailable) View.VISIBLE else View.GONE

        val supportsCanListen = supportsCanListen(currentModuleName)
        startCanListenButton.visibility = if (supportsCanListen) View.VISIBLE else View.GONE

        requestVinButton.visibility = View.VISIBLE
    }

    private fun supportsPin(moduleName: String) =
        moduleName == getString(R.string.module_canbus) ||
                moduleName == getString(R.string.module_canbus_uart) ||
                moduleName == getString(R.string.module_can_demo) ||
                moduleName == getString(R.string.module_kline_usb)

    private fun supportsCanListen(moduleName: String) =
        moduleName == getString(R.string.module_canbus) ||
                moduleName == getString(R.string.module_canbus_uart) ||
                moduleName == getString(R.string.module_can_demo)

    private fun startBluetoothDiscovery() {
        if (bluetoothAdapter == null) {
            Toast.makeText(requireContext(), getString(R.string.bluetooth_not_supported), Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH_SCAN), 2)
            return
        }

        bluetoothDevices.clear()
        val paired = bluetoothAdapter?.bondedDevices ?: emptySet()
        if (paired.isNotEmpty()) {
            bluetoothDevices.addAll(paired)
            val names = bluetoothDevices.map { it.name ?: it.address }
            bluetoothDeviceSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, names)
        } else {
            Toast.makeText(requireContext(), getString(R.string.no_paired_bt_devices), Toast.LENGTH_SHORT).show()
        }

        bluetoothAdapter?.startDiscovery()
        Toast.makeText(requireContext(), getString(R.string.bluetooth_scanning), Toast.LENGTH_SHORT).show()
    }

    private fun generateDiagnosticReport() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val safeDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val (brand, model, year) = VehicleManager.selectedVehicle
        val vehicle = "$brand $model $year"
        val logs = outputText.text.toString()
        val module = currentModuleName.ifBlank { getString(R.string.module_unknown) }
        val connectionStatus = if (isConnected) getString(R.string.connection_success) else getString(R.string.connection_failed)

        val capabilities = VehicleCapabilities.getCapabilities(brand, model)
        val supportsCan = capabilities?.supportsCan?.toString() ?: "N/A"
        val supportsObd2 = capabilities?.supportsObd2?.toString() ?: "N/A"
        val supportsKLine = capabilities?.supportsKLine?.toString() ?: "N/A"
        val compatibleModules = capabilities?.compatibleModules?.joinToString(", ") ?: "N/A"

        val report = StringBuilder()
        report.appendLine(getString(R.string.report_header))
        report.appendLine("${getString(R.string.report_date)} $date")
        report.appendLine("${getString(R.string.report_vehicle)} $vehicle")
        report.appendLine("${getString(R.string.report_module)} $module")
        report.appendLine("${getString(R.string.report_connection)} $connectionStatus")
        report.appendLine(getString(R.string.report_capabilities))
        report.appendLine("CAN: $supportsCan, OBD2: $supportsObd2, K-Line: $supportsKLine")
        report.appendLine("${getString(R.string.report_modules)} $compatibleModules")

        val lastSeedAndKey = PsaKeyCalculator.lastCalculation
        if (lastSeedAndKey != null) {
            report.appendLine(getString(R.string.report_seed_received, lastSeedAndKey.first))
            report.appendLine(getString(R.string.report_key_calculated, lastSeedAndKey.second))
        }

        report.appendLine()
        report.appendLine(getString(R.string.report_pid_section))
        report.appendLine(DiagnosticRecorder.getDecodedSummary())
        report.appendLine()
        report.appendLine(getString(R.string.report_dtc_section))
        report.appendLine(DiagnosticRecorder.getDtcSummary())
        report.appendLine()
        report.appendLine(getString(R.string.report_logs_section))
        report.appendLine(logs)

        try {
            val dir = File(requireContext().getExternalFilesDir(null), "PSAImmoTool")
            if (!dir.exists()) dir.mkdirs()
            val fileName = "rapport_${safeDate}_${brand}_${model}.txt"
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(report.toString().toByteArray()) }
            UiUpdater.appendLog(getString(R.string.report_saved, file.absolutePath))
        } catch (e: Exception) {
            UiUpdater.appendLog(getString(R.string.report_error, e.message ?: ""))
        }
    }

    private fun updateVehicleInfoDisplay() {
        val (brand, model, year) = VehicleManager.selectedVehicle
        val capabilities = VehicleCapabilities.getCapabilities(brand, model)
        val algoAvailable = PsaKeyCalculator.hasKeyAlgoFor(VehicleManager.selectedVehicle)
        val capText = buildString {
            append("$brand $model $year\n")
            if (capabilities != null) {
                append("CAN: ${capabilities.supportsCan}, ")
                append("OBD2: ${capabilities.supportsObd2}, ")
                append("K-Line: ${capabilities.supportsKLine}\n")
                append("Modules: ${capabilities.compatibleModules.joinToString(", ")}\n")
            } else {
                append(getString(R.string.no_key_algo_for_vehicle) + "\n")
            }
            append(getString(if (algoAvailable) R.string.pin_algo_present else R.string.pin_algo_absent))
        }
        statusText.text = capText
        Toast.makeText(requireContext(), capText, Toast.LENGTH_LONG).show()
    }
}
