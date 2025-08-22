package com.helly.psaimmotool

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.helly.psaimmotool.modules.VehicleModule
import com.helly.psaimmotool.ports.StatusPort
import com.helly.psaimmotool.utils.*
import com.helly.psaimmotool.ports.*
import com.helly.psaimmotool.protocol.*
import com.helly.psaimmotool.transport.*
import com.helly.psaimmotool.mobile.StatusReporter

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

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

    private var currentModule: VehicleModule? = null
    private var currentModuleName: String = ""

    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    private lateinit var btNamesAdapter: ArrayAdapter<String>
    private var btDialog: AlertDialog? = null
    private var isModuleConnected = false

    // Implémentation mobile du StatusPort
    private lateinit var statusPort: StatusPort

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null && bluetoothDevices.none { it.address == device.address }) {
                        bluetoothDevices.add(device)
                        btNamesAdapter.add(device.name ?: device.address ?: getString(R.string.unknown_device))
                        btNamesAdapter.notifyDataSetChanged()
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    context?.let { Toast.makeText(it, R.string.bt_discovery_started, Toast.LENGTH_SHORT).show() }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    context?.let { Toast.makeText(it, R.string.bt_discovery_finished, Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        checkAndRequestAllPermissions(this)
    }

    private fun onPermissionsGranted() {
        bindViews()
        initToolbar()
        setupButtons()

        UiUpdater.init(statusText, outputText)
        ContextProvider.init(applicationContext)

        statusPort = StatusPortImpl(  this)

        registerReceiver(bluetoothReceiver, IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 2001) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onPermissionsGranted()
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun checkAndRequestAllPermissions(activity: Activity) {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionUtils.hasStoragePermission(activity)) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (!PermissionUtils.hasStoragePermission(activity)) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!PermissionUtils.hasBluetoothPermission(activity)) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (!PermissionUtils.hasBluetoothScanPermission(activity)) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        if (!PermissionUtils.hasLocationPermission(activity)) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), 2001)
        } else {
            onPermissionsGranted()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_select_vehicle -> { showBrandSelectionDialog(); true }
            R.id.menu_select_module -> { showModuleSelectionDialog(); true }
            R.id.menu_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
            R.id.menu_quit -> { finish(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showBrandSelectionDialog() {
        val brands = VehicleCapabilities.getAllBrands()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_brand))
            .setItems(brands.toTypedArray()) { _, which ->
                showModelSelectionDialog(brands[which])
            }
            .show()
    }

    private fun showModelSelectionDialog(brand: String) {
        val models = VehicleCapabilities.getModelsForBrand(brand)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_model))
            .setItems(models.toTypedArray()) { _, which ->
                showYearSelectionDialog(brand, models[which])
            }
            .show()
    }

    private fun showYearSelectionDialog(brand: String, model: String) {
        val years = VehicleCapabilities.getYearsForModel(brand, model)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_year))
            .setItems(years.map { it.toString() }.toTypedArray()) { _, which ->
                val selectedYear = years[which]
                VehicleManager.selectedVehicle = Triple(brand, model, selectedYear)
                Toast.makeText(this, getString(R.string.vehicle_selected, brand, model, selectedYear), Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showModuleSelectionDialog() {
        val modules = VehicleCapabilities.getCompatibleModules()
        if (modules.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle(R.string.select_module)
            .setItems(modules.toTypedArray()) { _, which ->
                val selected = modules[which]
                currentModuleName = selected

                if (selected == getString(R.string.module_obd2_bluetooth)) {
                    currentModule = null
                    updateUiVisibilityForModule()
                    openBluetoothLivePicker()
                } else {
                    buildModuleForName(selected)
                    updateUiVisibilityForModule()
                    isModuleConnected = false
                    connectButton.text = getString(R.string.button_connect)
                }
            }.show()
    }

    private fun openBluetoothLivePicker() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, R.string.permission_bt_required, Toast.LENGTH_LONG).show()
            return
        }

        bluetoothAdapter?.cancelDiscovery()
        bluetoothDevices.clear()
        val listView = ListView(this)
        btNamesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listView.adapter = btNamesAdapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = bluetoothDevices[position]
            btDialog?.dismiss()

            val reporter = StatusReporter(this, statusPort)
            val transport = BluetoothTransport()
            val protocol = Obd2Protocol(transport).withReporter(reporter)
            currentModule = VehicleModule(protocol)

            updateUiVisibilityForModule()
        }

        btDialog = AlertDialog.Builder(this)
            .setTitle(R.string.bluetooth_devices)
            .setView(listView)
            .setNegativeButton(android.R.string.cancel, null)
            .show()

        bluetoothAdapter?.startDiscovery()
    }

    private fun updateUiVisibilityForModule() {
        val isCanModule = currentModuleName.contains("CANBUS", true)
        requestVinButton.isVisible = currentModule != null || isCanModule
        requestPinButton.isVisible = isCanModule
        startCanListenButton.isVisible = isCanModule
    }

    private fun setupButtons() {
        connectButton.setOnClickListener {
            if (currentModule == null) {
                Toast.makeText(this, R.string.no_module_connected, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isModuleConnected) {
                currentModule?.connect()
                isModuleConnected = true
                connectButton.text = getString(R.string.button_disconnect)
            } else {
                currentModule?.disconnect()
                isModuleConnected = false
                connectButton.text = getString(R.string.button_connect)
            }
        }

        requestVinButton.setOnClickListener { currentModule?.requestVin() }
        requestPinButton.setOnClickListener { currentModule?.requestPin() }
        startCanListenButton.setOnClickListener { currentModule?.startCanListening() }
        sendFrameButton.setOnClickListener {
            val frame = inputFrameText.text.toString()
            statusPort.appendLog("➡️ $frame")
            currentModule?.sendCustomFrame(frame)
        }
        exportLogsButton.setOnClickListener {
            LogExporter.exportLogs(this, outputText.text.toString())
        }
        clearLogsButton.setOnClickListener {
            UiUpdater.clearLog(this)
        }
        generateReportButton.setOnClickListener {
            ReportGenerator.generate(this, currentModuleName, outputText.text.toString())
        }
    }

    private fun buildModuleForName(name: String) {
        val reporter = StatusReporter(this, statusPort)

        currentModule = when (name) {
            getString(R.string.module_obd2_usb) -> {
                val transport = UsbTransport()
                val protocol = Obd2Protocol(transport).withReporter(reporter)
                VehicleModule(protocol)
            }
            getString(R.string.module_kline_usb) -> {
                val transport = UsbTransport()
                val protocol = KLineProtocol(transport).withReporter(reporter)
                VehicleModule(protocol)
            }
            getString(R.string.module_canbus) -> {
                val transport = UsbTransport()
                val protocol = CanProtocol(transport).withReporter(reporter)
                VehicleModule(protocol)
            }
            getString(R.string.module_canbus_uart) -> {
                val transport = UartTransport()
                val protocol = CanProtocol(transport).withReporter(reporter)
                VehicleModule(protocol)
            }
            getString(R.string.module_can_demo) -> {
                val transport = DemoTransport()
                val protocol = CanProtocol(transport).withReporter(reporter)
                VehicleModule(protocol)
            }
            else -> null
        }
    }

    private fun bindViews() {
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

    companion object {
        const val REQ_BT_PERMS = 1001
    }
}
