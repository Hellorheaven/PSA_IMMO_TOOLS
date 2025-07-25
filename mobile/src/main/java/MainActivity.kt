package com.helly.psaimmotool

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.helly.psaimmotool.modules.*
import com.helly.psaimmotool.utils.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // ---- UI ----
    private lateinit var moduleSelector: Spinner
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

    // ---- State ----
    private var currentModule: BaseModule? = null
    private var currentModuleName: String = ""
    private var isConnected = false

    // ---- Bluetooth ----
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    private lateinit var btNamesAdapter: ArrayAdapter<String>
    private var btDialog: AlertDialog? = null

    // Receiver temps réel pour le scan
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null && bluetoothDevices.none { it.address == device.address }) {
                        bluetoothDevices.add(device)
                        btNamesAdapter.add(device.name ?: device.address ?: getString(R.string.unknown_device))
                        btNamesAdapter.notifyDataSetChanged()
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    // Le scan s'arrête, on pourrait en relancer un si on veut loop
                }
            }
        }
    }

    // ---- Lifecycle ----
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ContextProvider.init(applicationContext)
        DiagnosticRecorder.clear()

        bindViews()
        initToolbar()
        forceHideSpinners() // on n’utilise plus les spinners pour module / BT
        setupButtons()

        UiUpdater.init(statusText, outputText)
        updateVehicleInfoDisplay()
        showVehicleSummaryPopup()

        // Permissions BT Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                REQ_BT_PERMS
            )
        }

        // Enregistrer le receiver pour le scan
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) { }
        stopBtDiscovery()
    }

    // ---- Toolbar / menu ----
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Véhicule
            R.id.menu_select_vehicle -> {
                showVehicleSelectionDialog()
                return true
            }
            // Module
            R.id.menu_select_module -> {
                showModuleSelectionDialog()
                return true
            }
            // Thème
            R.id.menu_theme_light -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                return true
            }
            R.id.menu_theme_dark -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                return true
            }
            // Langue
            R.id.menu_language_fr -> {
                LocaleUtils.setLocaleAndRestart(this, "fr")
                recreate()
                return true
            }
            R.id.menu_language_en -> {
                LocaleUtils.setLocaleAndRestart(this, "en")
                recreate()
                return true
            }
            // Editeur véhicule
            R.id.menu_vehicle_editor -> {
                startActivity(Intent(this, VehicleEditorActivity::class.java))
                return true
            }
            // Update
            R.id.menu_update -> {
                try {
                    UpdateManager.showUpdateDialog(this)
                } catch (_: Throwable) {
                    Toast.makeText(this, "UpdateManager not available", Toast.LENGTH_SHORT).show()
                }
                return true
            }
            // Quitter
            R.id.menu_quit -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // ---- Dialogues ----

    /** Choix du module dans un menu, et si OBD2 (Bluetooth) => on ouvre DIRECT la liste BT live */
    private fun showModuleSelectionDialog() {
        val modules = VehicleCapabilities.getCompatibleModules()
        if (modules.isEmpty()) {
            Toast.makeText(this, R.string.no_module_connected, Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_module))
            .setItems(modules.toTypedArray()) { _, which ->
                currentModuleName = modules[which]

                if (currentModuleName == getString(R.string.module_obd2_bluetooth)) {
                    // Directement le picker BT **live**
                    currentModule = null // le module sera créé après sélection du device
                    updateUiVisibilityForModule()
                    openBluetoothLivePicker()
                } else {
                    buildModuleForName(currentModuleName)
                    updateUiVisibilityForModule()
                }
            }
            .show()
    }

    /** Liste des périphériques Bluetooth proches, qui s’actualise en temps réel tant que le scan tourne */
    private fun openBluetoothLivePicker() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_LONG).show()
            return
        }

        if (!ensureBtPermissions()) return

        bluetoothDevices.clear()

        val listView = ListView(this)
        btNamesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        listView.adapter = btNamesAdapter

        listView.setOnItemClickListener { _, _, position, _ ->
            if (position in bluetoothDevices.indices) {
                val device = bluetoothDevices[position]
                stopBtDiscovery()
                btDialog?.dismiss()

                currentModule = Obd2BluetoothModule(this, device)
                currentModuleName = getString(R.string.module_obd2_bluetooth)
                updateUiVisibilityForModule()

                Toast.makeText(this, "Selected: ${device.name ?: device.address}", Toast.LENGTH_SHORT).show()
            }
        }

        btDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.bluetooth_devices))
            .setView(listView)
            .setNegativeButton(android.R.string.cancel) { d, _ ->
                stopBtDiscovery()
                d.dismiss()
            }
            .create()

        btDialog?.show()

        startBtDiscovery()
        Toast.makeText(this, getString(R.string.bluetooth_scanning), Toast.LENGTH_SHORT).show()
    }

    private fun showVehicleSelectionDialog() {
        val vehicules = listOf(
            Triple("Peugeot", "207", 2008),
            Triple("Peugeot", "307", 2010),
            Triple("Ducati", "848", 2009),
            Triple("MG", "4", 2023),
            Triple("Ford", "Mustang Mach-E", 2021)
        )
        val labels = vehicules.map { "${it.first} ${it.second} ${it.third}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_vehicle))
            .setItems(labels) { _, which ->
                val v = vehicules[which]
                VehicleManager.setVehicle(v.first, v.second, v.third)
                updateVehicleInfoDisplay()
                updateUiVisibilityForModule()
            }
            .show()
    }

    // ---- Bluetooth helpers ----
    private fun startBtDiscovery() {
        stopBtDiscovery()
        if (bluetoothAdapter?.isDiscovering == true) return
        bluetoothAdapter?.startDiscovery()
    }

    private fun stopBtDiscovery() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (_: Exception) { }
    }

    private fun ensureBtPermissions(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val scanOk = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        val connectOk = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        if (!scanOk || !connectOk) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), REQ_BT_PERMS)
            return false
        }
        return true
    }

    // ---- Logique UI / Modules ----

    private fun buildModuleForName(name: String) {
        currentModule = when (name) {
            getString(R.string.module_obd2_usb) -> Obd2UsbModule(this)
            getString(R.string.module_obd2_bluetooth) -> null // créé après choix device
            getString(R.string.module_kline_usb) -> KLineUsbModule(this)
            getString(R.string.module_canbus) -> CanBusModule(this)
            getString(R.string.module_canbus_uart) -> CanBusUartModule(this)
            getString(R.string.module_can_demo) -> GenericCanDemoModule(this)
            else -> null
        }
    }

    private fun updateUiVisibilityForModule() {
        val isCanModule = currentModuleName.contains("CANBUS", ignoreCase = true)
                || currentModule is CanBusModule
                || currentModule is CanBusUartModule
                || currentModule is GenericCanDemoModule

        val isBluetoothObd2 = currentModuleName == getString(R.string.module_obd2_bluetooth)

        // boutons visibles / cachés
        requestVinButton.isVisible = currentModule != null || isBluetoothObd2
        requestPinButton.isVisible = isCanModule
        startCanListenButton.isVisible = isCanModule

        // le spinner BT de l’UI reste caché (on ne l’utilise plus)
        bluetoothDeviceSpinner.visibility = View.GONE
    }

    private fun setupButtons() {
        connectButton.setOnClickListener {
            currentModule?.connect()
            isConnected = currentModule != null
        }
        requestVinButton.setOnClickListener { currentModule?.requestVin() }
        requestPinButton.setOnClickListener { currentModule?.requestPin() }
        startCanListenButton.setOnClickListener { currentModule?.startCanListening() }

        sendFrameButton.setOnClickListener {
            val frame = inputFrameText.text.toString()
            UiUpdater.appendLog("\u2B06\uFE0F $frame")
            currentModule?.sendCustomFrame(frame)
        }

        exportLogsButton.setOnClickListener {
            LogExporter.exportLogs(this, outputText.text.toString())
        }

        clearLogsButton.setOnClickListener {
            outputText.text = ""
            UiUpdater.appendLog(getString(R.string.logs_cleared))
        }

        generateReportButton.setOnClickListener {
            generateDiagnosticReport()
        }
    }

    private fun generateDiagnosticReport() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val safeDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val (brand, model, year) = VehicleManager.selectedVehicle
        val vehicle = "$brand $model $year"
        val logs = outputText.text.toString()
        val module = currentModuleName.ifBlank { getString(R.string.module_unknown) }
        val connectionStatus =
            if (isConnected) getString(R.string.connection_success) else getString(R.string.connection_failed)

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
            val dir = File(getExternalFilesDir(null), "PSAImmoTool")
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
        showVehicleSummaryPopup()
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
    }

    private fun showVehicleSummaryPopup() {
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
        Toast.makeText(this, capText, Toast.LENGTH_LONG).show()
    }

    private fun bindViews() {
        moduleSelector = findViewById(R.id.moduleSelector)
        bluetoothDeviceSpinner = findViewById(R.id.bluetoothDeviceSpinner)
        connectButton = findViewById(R.id.connectButton)
        requestVinButton = findViewById(R.id.requestVinButton)
        requestPinButton = findViewById(R.id.requestPinButton)
        startCanListenButton = findViewById(R.id.startCanListenButton)
        inputFrameText = findViewById(R.id.inputFrameText)
        sendFrameButton = findViewById(R.id.sendFrameButton)
        exportLogsButton = findViewById(R.id.exportLogsButton)
        clearLogsButton = findViewById(R.id.clearLogsButton)
        generateReportButton = findViewById(R.id.generateReportButton)
        statusText = findViewById(R.id.statusText)
        outputText = findViewById(R.id.outputText)
    }

    private fun initToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)
    }

    private fun forceHideSpinners() {
        moduleSelector.visibility = View.GONE
        bluetoothDeviceSpinner.visibility = View.GONE
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.helly.psaimmotool.USB_PERMISSION"
        const val REQ_BT_PERMS = 1001
    }
}
