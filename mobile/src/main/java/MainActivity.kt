// app/src/main/java/com/helly/psaimmotool/MainActivity.kt
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
import androidx.core.widget.NestedScrollView
import com.helly.psaimmotool.modules.*
import com.helly.psaimmotool.ui.DiagnosticsFragment
import com.helly.psaimmotool.utils.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // === Vues mode "téléphone" (layout immuable) ===
    private var moduleSelector: Spinner? = null
    private var bluetoothDeviceSpinner: Spinner? = null
    private var connectButton: Button? = null
    private var requestVinButton: Button? = null
    private var requestPinButton: Button? = null
    private var startCanListenButton: Button? = null
    private var inputFrameText: EditText? = null
    private var sendFrameButton: Button? = null
    private var exportLogsButton: Button? = null
    private var clearLogsButton: Button? = null
    private var generateReportButton: Button? = null
    private var statusText: TextView? = null
    private var outputText: TextView? = null
    private var mainScroll: NestedScrollView? = null

    // === État courant ===
    private var currentModule: BaseModule? = null
    private var currentModuleName: String = ""
    private var isConnected = false

    // === Bluetooth (mode téléphone) ===
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && !bluetoothDevices.contains(device)) {
                    bluetoothDevices.add(device)
                    val spinner = bluetoothDeviceSpinner
                    if (spinner != null) {
                        val names = bluetoothDevices.map { it.name ?: it.address }
                        spinner.adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, names)
                    }
                }
            }
        }
    }

    private val isTabletOrLandscape by lazy { findViewById<View?>(R.id.fragmentContainer) != null }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppropriateLayout()

        ContextProvider.init(applicationContext)
        DiagnosticRecorder.clear()

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)

        if (!isTabletOrLandscape) {
            bindPhoneViews()
            initPhoneUi()
        } else {
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, DiagnosticsFragment(), FRAG_TAG)
                    .commit()
            }
        }

        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
        }
    }

    private fun setAppropriateLayout() {
        // téléphone : res/layout/activity_main.xml (inchangé)
        // tablette/paysage : res/layout-land|sw600dp/activity_main.xml (avec fragmentContainer)
        setContentView(R.layout.activity_main)
    }

    private fun bindPhoneViews() {
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
        mainScroll = findViewById(R.id.mainScroll)
    }

    private fun initPhoneUi() {
        moduleSelector?.visibility = View.GONE

        UiUpdater.init(statusText!!, outputText!!)
        updateVehicleInfoDisplay()
        showVehicleSummaryPopup()

        refreshModuleSpinner()
        setupPhoneButtons()

        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        val autoScroll = prefs.getBoolean(Prefs.KEY_AUTOSCROLL, true)
        if (autoScroll) {
            outputText?.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                mainScroll?.post { mainScroll?.fullScroll(View.FOCUS_DOWN) }
            }
        }

        bluetoothDeviceSpinner?.visibility = View.GONE
    }

    private fun setupPhoneButtons() {
        connectButton?.setOnClickListener {
            val selected = currentModuleName
            if (selected.isBlank()) {
                Toast.makeText(this, "Choisissez un module dans le menu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            isConnected = false
            currentModule = when (selected) {
                getString(R.string.module_obd2_usb) -> Obd2UsbModule(this)
                getString(R.string.module_obd2_bluetooth) -> {
                    val device = bluetoothDevices.getOrNull(bluetoothDeviceSpinner?.selectedItemPosition ?: -1)
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

        requestVinButton?.setOnClickListener { currentModule?.requestVin() }

        requestPinButton?.setOnClickListener {
            val vehicle = VehicleManager.selectedVehicle
            if (!PsaKeyCalculator.hasKeyAlgoFor(vehicle)) {
                Toast.makeText(this, getString(R.string.no_key_algo_for_vehicle), Toast.LENGTH_LONG).show()
            }
            currentModule?.requestPin()
        }

        startCanListenButton?.setOnClickListener { currentModule?.startCanListening() }

        sendFrameButton?.setOnClickListener {
            val frame = inputFrameText?.text?.toString() ?: ""
            UiUpdater.appendLog("\u2B06\uFE0F $frame")
            currentModule?.sendCustomFrame(frame)
        }

        exportLogsButton?.setOnClickListener {
            val content = outputText?.text?.toString() ?: ""
            LogExporter.exportLogs(this, content)
        }

        clearLogsButton?.setOnClickListener {
            outputText?.text = ""
            UiUpdater.appendLog(getString(R.string.logs_cleared))
        }

        generateReportButton?.setOnClickListener {
            generateDiagnosticReport()
        }
    }

    private fun refreshModuleSpinner() {
        // juste pour récupérer la liste de modules compatibles (utilisée dans le menu)
        VehicleCapabilities.getCompatibleModules()
    }

    // === Menu ===

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val sub = menu.findItem(R.id.menu_modules)?.subMenu ?: return super.onPrepareOptionsMenu(menu)
        sub.clear()
        VehicleCapabilities.getCompatibleModules().forEachIndexed { index, name ->
            sub.add(Menu.NONE, MODULE_MENU_BASE + index, Menu.NONE, name)
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        // Sélection dynamique d'un module
        if (id in MODULE_MENU_BASE until MODULE_MENU_BASE + 1000) {
            val list = VehicleCapabilities.getCompatibleModules()
            val idx = id - MODULE_MENU_BASE
            if (idx in list.indices) {
                currentModuleName = list[idx]

                if (isTabletOrLandscape) {
                    (supportFragmentManager.findFragmentByTag(FRAG_TAG) as? DiagnosticsFragment)
                        ?.onModuleSelected(currentModuleName)
                } else {
                    updateUiForSelectedModulePhone()
                    if (currentModuleName == getString(R.string.module_obd2_bluetooth)) {
                        startBluetoothDiscoveryPhone()
                    }
                }
            }
            return true
        }

        when (id) {
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
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.menu_quit -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // === Visibilité UI (mode téléphone) ===
    private fun updateUiForSelectedModulePhone() {
        val isBt = currentModuleName == getString(R.string.module_obd2_bluetooth)
        bluetoothDeviceSpinner?.visibility = if (isBt) View.VISIBLE else View.GONE

        val supportsPin = supportsPin(currentModuleName)
        val algoAvailable = PsaKeyCalculator.hasKeyAlgoFor(VehicleManager.selectedVehicle)
        requestPinButton?.visibility = if (supportsPin && algoAvailable) View.VISIBLE else View.GONE

        val supportsCanListen = supportsCanListen(currentModuleName)
        startCanListenButton?.visibility = if (supportsCanListen) View.VISIBLE else View.GONE

        requestVinButton?.visibility = View.VISIBLE
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

    // === Bluetooth (mode téléphone) ===
    private fun startBluetoothDiscoveryPhone() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.bluetooth_not_supported), Toast.LENGTH_LONG).show()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 2)
            return
        }

        bluetoothDevices.clear()
        val paired = bluetoothAdapter.bondedDevices
        if (paired.isNotEmpty()) {
            bluetoothDevices.addAll(paired)
            val names = bluetoothDevices.map { it.name ?: it.address }
            bluetoothDeviceSpinner?.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        } else {
            Toast.makeText(this, getString(R.string.no_paired_bt_devices), Toast.LENGTH_SHORT).show()
        }

        bluetoothAdapter.startDiscovery()
        Toast.makeText(this, getString(R.string.bluetooth_scanning), Toast.LENGTH_SHORT).show()
    }

    // === Sélection véhicule ===
    private fun showVehicleSelectionDialog() {
        fun updateAndRefresh(vehicle: Triple<String, String, Int>) {
            VehicleManager.setVehicle(vehicle.first, vehicle.second, vehicle.third)
            invalidateOptionsMenu() // pour regénérer la liste des modules
            updateVehicleInfoDisplay()

            if (isTabletOrLandscape) {
                (supportFragmentManager.findFragmentByTag(FRAG_TAG) as? DiagnosticsFragment)
                    ?.onModuleSelected(currentModuleName)
            } else {
                updateUiForSelectedModulePhone()
            }
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

    // === Rapport (mode téléphone) ===
    private fun generateDiagnosticReport() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val safeDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val (brand, model, year) = VehicleManager.selectedVehicle
        val vehicle = "$brand $model $year"
        val logs = outputText?.text?.toString() ?: ""
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
        statusText?.text = capText
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    companion object {
        const val ACTION_USB_PERMISSION = "com.helly.psaimmotool.USB_PERMISSION"
        private const val MODULE_MENU_BASE = 10_000
        private const val FRAG_TAG = "diag_fragment"
    }
}
