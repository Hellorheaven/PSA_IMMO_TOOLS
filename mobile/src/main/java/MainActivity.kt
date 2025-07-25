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
import androidx.core.view.isVisible
import com.helly.psaimmotool.modules.*
import com.helly.psaimmotool.utils.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // --- UI (layout immuable : on garde les références, mais certains widgets ne sont plus utilisés visuellement)
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

    // --- État
    private var currentModule: BaseModule? = null
    private var currentModuleName: String = ""
    private var isConnected = false

    // --- Bluetooth
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // --- BroadcastReceiver Bluetooth (optionnel, on conserve pour découvrir en live)
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && !bluetoothDevices.contains(device)) {
                    bluetoothDevices.add(device)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ContextProvider.init(applicationContext)
        DiagnosticRecorder.clear()

        bindViews()
        initToolbar()
        forceHideLegacySpinners()
        setupStaticListeners()

        UiUpdater.init(statusText, outputText)
        updateVehicleInfoDisplay()
        showVehicleSummaryPopup()

        // Permissions BT pour Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                1
            )
        }

        // Receiver découverte BT (même si on n'utilise plus le spinner)
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    // -------------------------------------------------------------
    // Menu
    // -------------------------------------------------------------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        // On laisse la construction de sous-menus modules & BT via dialogues
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            // VEHICULE
            R.id.menu_select_vehicle -> {
                showVehicleSelectionDialog()
                return true
            }

            // MODULE
            R.id.menu_select_module -> {
                showModuleSelectionDialog()
                return true
            }

            // BLUETOOTH (sélection périphérique)
            R.id.menu_select_bt_device -> {
                showBluetoothDeviceDialog()
                return true
            }

            // THEME
            R.id.menu_theme_light -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                return true
            }
            R.id.menu_theme_dark -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                return true
            }

            // LANGUE
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

            // ÉDITEUR DE VÉHICULE
            R.id.menu_vehicle_editor -> {
                startActivity(Intent(this, VehicleEditorActivity::class.java))
                return true
            }

            // UPDATE (si tu as gardé UpdateManager)
            R.id.menu_update -> {
                try {
                    UpdateManager.showUpdateDialog(this)
                } catch (_: Throwable) {
                    Toast.makeText(this, "UpdateManager absent", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            // QUIT
            R.id.menu_quit -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // -------------------------------------------------------------
    // Dialogues
    // -------------------------------------------------------------
    private fun showModuleSelectionDialog() {
        val modules = VehicleCapabilities.getCompatibleModules()
        if (modules.isEmpty()) {
            Toast.makeText(this, "No module available for this vehicle", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_module))
            .setItems(modules.toTypedArray()) { _, which ->
                currentModuleName = modules[which]
                buildModuleForName(currentModuleName)
                updateUiVisibilityForModule()
            }
            .show()
    }

    private fun showBluetoothDeviceDialog() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.bluetooth_not_supported), Toast.LENGTH_LONG).show()
            return
        }

        bluetoothDevices.clear()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.no_bluetooth_device), Toast.LENGTH_SHORT).show()
                return
            }
        }

        val paired = bluetoothAdapter.bondedDevices
        if (paired.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_paired_bt_devices), Toast.LENGTH_SHORT).show()
        } else {
            bluetoothDevices.addAll(paired)
        }

        val names = if (bluetoothDevices.isEmpty()) {
            arrayOf(getString(R.string.no_paired_bt_devices))
        } else {
            bluetoothDevices.map { it.name ?: it.address ?: getString(R.string.unknown_device) }.toTypedArray()
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.bluetooth_devices))
            .setItems(names) { _, which ->
                if (bluetoothDevices.isNotEmpty()) {
                    val selectedDevice = bluetoothDevices[which]
                    currentModule = Obd2BluetoothModule(this, selectedDevice)
                    currentModuleName = getString(R.string.module_obd2_bluetooth)
                    updateUiVisibilityForModule()
                    Toast.makeText(this, "Selected: ${selectedDevice.name ?: selectedDevice.address}", Toast.LENGTH_SHORT).show()
                }
            }
            .show()

        // Optionnel : lancer la découverte pour les devices non appairés
        try {
            bluetoothAdapter.startDiscovery()
            Toast.makeText(this, getString(R.string.bluetooth_scanning), Toast.LENGTH_SHORT).show()
        } catch (_: Exception) { }
    }

    private fun showVehicleSelectionDialog() {
        fun updateAndRefresh(vehicle: Triple<String, String, Int>) {
            VehicleManager.setVehicle(vehicle.first, vehicle.second, vehicle.third)
            // reset module courant (obliger à re-choisir)
            currentModule = null
            currentModuleName = ""
            updateVehicleInfoDisplay()
            updateUiVisibilityForModule()
        }

        val vehicules = listOf(
            Triple("Peugeot", "207", 2008),
            Triple("Peugeot", "207", 2010),
            Triple("Ducati", "848", 2009),
            Triple("MG", "4", 2023),
            Triple("Ford", "Mustang Mach-E", 2021)
        )
        val labels = vehicules.map { "${it.first} ${it.second} ${it.third}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_vehicle))
            .setItems(labels) { _, which ->
                val v = vehicules[which]
                updateAndRefresh(v)
            }
            .show()
    }

    // -------------------------------------------------------------
    // Actions / logique
    // -------------------------------------------------------------
    private fun buildModuleForName(name: String) {
        currentModule = when (name) {
            getString(R.string.module_obd2_usb) -> Obd2UsbModule(this)
            getString(R.string.module_obd2_bluetooth) -> {
                // On ne connaît pas encore l'appareil, l'utilisateur devra ouvrir "Select Bluetooth device"
                null
            }
            getString(R.string.module_kline_usb) -> KLineUsbModule(this)
            getString(R.string.module_canbus) -> CanBusModule(this)
            getString(R.string.module_canbus_uart) -> CanBusUartModule(this)
            getString(R.string.module_can_demo) -> GenericCanDemoModule(this)
            else -> null
        }
    }

    private fun connectCurrentModule() {
        currentModule?.connect()
        isConnected = currentModule != null
    }

    private fun updateUiVisibilityForModule() {
        // Par défaut : tout OFF
        requestVinButton.isVisible = false
        requestPinButton.isVisible = false
        startCanListenButton.isVisible = false
        inputFrameText.isVisible = false
        sendFrameButton.isVisible = false

        // Le module BT spinner (layout) reste invisible (on passe par menu)
        bluetoothDeviceSpinner.visibility = View.GONE
        moduleSelector.visibility = View.GONE

        if (currentModuleName.isBlank()) {
            connectButton.isVisible = false
            return
        }

        // Connect toujours dispo si module sélectionné (sauf OBD2 Bluetooth sans device sélectionné)
        connectButton.isVisible = true

        // VIN : disponible pour tous nos modules implémentés
        requestVinButton.isVisible = true

        // PIN / CAN listen : seulement modules CAN
        val isCan =
            currentModuleName == getString(R.string.module_canbus) ||
                    currentModuleName == getString(R.string.module_canbus_uart) ||
                    currentModuleName == getString(R.string.module_can_demo)

        requestPinButton.isVisible = isCan
        startCanListenButton.isVisible = isCan

        // Trames custom : on l'autorise pour CAN et K-Line (USB) & OBD (USB/BT)
        inputFrameText.isVisible = true
        sendFrameButton.isVisible = true

        // Si OBD2 BT sélectionné mais pas encore d'appareil choisi → inviter à choisir
        if (currentModuleName == getString(R.string.module_obd2_bluetooth) && currentModule == null) {
            Toast.makeText(this, getString(R.string.no_bluetooth_device), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupStaticListeners() {
        connectButton.setOnClickListener {
            if (currentModule == null) {
                Toast.makeText(this, "Select a module first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            connectCurrentModule()
        }

        requestVinButton.setOnClickListener { currentModule?.requestVin() }

        requestPinButton.setOnClickListener {
            val vehicle = VehicleManager.selectedVehicle
            if (!PsaKeyCalculator.hasKeyAlgoFor(vehicle)) {
                Toast.makeText(this, getString(R.string.no_key_algo_for_vehicle), Toast.LENGTH_LONG).show()
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
            LogExporter.exportLogs(this, content)
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

    private fun forceHideLegacySpinners() {
        // on force à GONE ces spinners pour respecter l'immuabilité du layout sans les utiliser
        moduleSelector.visibility = View.GONE
        bluetoothDeviceSpinner.visibility = View.GONE
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.helly.psaimmotool.USB_PERMISSION"
    }
}
