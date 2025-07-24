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
import com.helly.psaimmotool.modules.*
import com.helly.psaimmotool.utils.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

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

    private var currentModule: BaseModule? = null
    private var currentModuleName: String = ""
    private var isConnected = false
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ContextProvider.init(applicationContext)
        DiagnosticRecorder.clear()

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

        UiUpdater.init(statusText, outputText)
        updateVehicleInfoDisplay()

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)

        showVehicleSummaryPopup()
        refreshModuleSpinner()
        setupButtons()

        // Masquer le spinner BT par défaut
        bluetoothDeviceSpinner.visibility = View.GONE

        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
        }

        moduleSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selected = moduleSelector.selectedItem.toString()
                // Démarre le scan BT si OBD2 Bluetooth
                if (selected == getString(R.string.module_obd2_bluetooth)) {
                    bluetoothDevices.clear()
                    bluetoothAdapter?.startDiscovery()
                    Toast.makeText(this@MainActivity, getString(R.string.bluetooth_scanning), Toast.LENGTH_SHORT).show()
                    bluetoothDeviceSpinner.visibility = View.VISIBLE
                } else {
                    bluetoothDeviceSpinner.visibility = View.GONE
                }
                updateUiForSelectedModule(selected)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Met l’UI au bon état pour le module sélectionné par défaut (s’il y en a un)
        (moduleSelector.selectedItem?.toString())?.let { updateUiForSelectedModule(it) }
    }

    /**
     * Montre / masque les boutons en fonction du module choisi (et de l’algo PIN disponible).
     * Aucun changement côté layout : on ne fait que jouer sur la visibilité.
     */
    private fun updateUiForSelectedModule(selected: String) {
        // Valeurs par défaut
        var showRequestVin = true
        var showRequestPin = false
        var showStartCanListen = false
        var showSendFrame = true // on le laisse disponible (tu envoies souvent des trames custom)
        var showGenerateReport = true
        var showExportLogs = true
        var showClearLogs = true

        // Par module
        when (selected) {
            getString(R.string.module_canbus),
            getString(R.string.module_canbus_uart),
            getString(R.string.module_can_demo) -> {
                showRequestPin = true // modules CAN gèrent la séquence seed/key
                showStartCanListen = true
            }

            getString(R.string.module_obd2_bluetooth),
            getString(R.string.module_obd2_usb) -> {
                // Ces modules ne gèrent pas la séquence seed/key PSA (dans ton code actuel)
                showRequestPin = false
                showStartCanListen = false
            }

            getString(R.string.module_kline_usb) -> {
                // K-Line : VIN oui, PIN non (dans ton implémentation)
                showRequestPin = false
                showStartCanListen = false
            }
        }

        // En plus : si pas d’algo PIN pour le véhicule => masquer le bouton PIN
        val vehicle = VehicleManager.selectedVehicle
        if (!PsaKeyCalculator.hasKeyAlgoFor(vehicle)) {
            showRequestPin = false
        }

        requestVinButton.visibility = if (showRequestVin) View.VISIBLE else View.GONE
        requestPinButton.visibility = if (showRequestPin) View.VISIBLE else View.GONE
        startCanListenButton.visibility = if (showStartCanListen) View.VISIBLE else View.GONE

        sendFrameButton.visibility = if (showSendFrame) View.VISIBLE else View.GONE
        generateReportButton.visibility = if (showGenerateReport) View.VISIBLE else View.GONE
        exportLogsButton.visibility = if (showExportLogs) View.VISIBLE else View.GONE
        clearLogsButton.visibility = if (showClearLogs) View.VISIBLE else View.GONE
        inputFrameText.visibility = if (showSendFrame) View.VISIBLE else View.GONE
    }

    private fun refreshModuleSpinner() {
        val moduleNames = VehicleCapabilities.getCompatibleModules()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, moduleNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        moduleSelector.adapter = adapter
    }

    private fun setupButtons() {
        connectButton.setOnClickListener {
            val selected = moduleSelector.selectedItem.toString()
            currentModuleName = selected
            isConnected = false
            currentModule = when (selected) {
                getString(R.string.module_obd2_usb) -> Obd2UsbModule(this)
                getString(R.string.module_obd2_bluetooth) -> {
                    val device = bluetoothDevices.getOrNull(bluetoothDeviceSpinner.selectedItemPosition)
                    Obd2BluetoothModule(this, device)
                }
                getString(R.string.module_kline_usb) -> KLineUsbModule(this)
                getString(R.string.module_canbus) -> CanBusModule(this)
                getString(R.string.module_canbus_uart) -> CanBusUartModule(this)
                getString(R.string.module_can_demo) -> GenericCanDemoModule(this)
                else -> null
            }
            currentModule?.connect()
            isConnected = true
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

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && !bluetoothDevices.contains(device)) {
                    bluetoothDevices.add(device)
                    val names = bluetoothDevices.map { it.name ?: it.address }
                    bluetoothDeviceSpinner.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, names)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_select_vehicle -> {
                showVehicleSelectionDialog()
                return true
            }
            R.id.menu_theme_light -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                return true
            }
            R.id.menu_theme_dark -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                return true
            }
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
            R.id.menu_vehicle_editor -> {
                startActivity(Intent(this, VehicleEditorActivity::class.java))
                return true
            }
            R.id.menu_update -> {
                UpdateManager.showUpdateDialog(this)
                return true
            }
            R.id.menu_quit -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showVehicleSelectionDialog() {
        fun updateAndRefresh(vehicle: Triple<String, String, Int>) {
            VehicleManager.setVehicle(vehicle.first, vehicle.second, vehicle.third)
            refreshModuleSpinner()
            updateVehicleInfoDisplay()
            // remettre à jour l'UI des boutons selon le module actuellement sélectionné
            (moduleSelector.selectedItem?.toString())?.let { updateUiForSelectedModule(it) }
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

    companion object {
        const val ACTION_USB_PERMISSION = "com.helly.psaimmotool.USB_PERMISSION"
    }
}
