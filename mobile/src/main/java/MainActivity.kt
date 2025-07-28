// MainActivity.kt (corrigé)
package com.helly.psaimmotool

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.helly.psaimmotool.modules.*
import com.helly.psaimmotool.utils.*
import java.util.*

@Suppress("DEPRECATION")
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

    @SuppressLint("ServiceCast")
    val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()


    private lateinit var btNamesAdapter: ArrayAdapter<String>
    private var btDialog: AlertDialog? = null

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
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        initToolbar()
        forceHideSpinners()
        setupButtons()

        UiUpdater.init(statusText, outputText)
        ContextProvider.init(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                ), REQ_BT_PERMS)
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
        }
        registerReceiver(bluetoothReceiver, filter)
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
        return when (item.itemId) {
            R.id.menu_select_vehicle -> {
                showVehicleSelectionDialog(); true
            }
            R.id.menu_select_module -> {
                showModuleSelectionDialog(); true
            }
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java)); true
            }
            R.id.menu_quit -> {
                finish(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showModuleSelectionDialog() {
        val modules = VehicleCapabilities.getCompatibleModules()
        if (modules.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle(R.string.select_module)
            .setItems(modules.toTypedArray()) @androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_SCAN) { _, which ->
                val selected = modules[which]
                currentModuleName = selected

                if (selected == getString(R.string.module_obd2_bluetooth)) {
                    currentModule = null
                    updateUiVisibilityForModule()
                    openBluetoothLivePicker()
                } else {
                    buildModuleForName(selected)
                    updateUiVisibilityForModule()
                }
            }
            .show()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun openBluetoothLivePicker() {
        bluetoothDevices.clear()
        val listView = ListView(this)
        btNamesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listView.adapter = btNamesAdapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val device = bluetoothDevices[position]
            btDialog?.dismiss()
            currentModule = Obd2BluetoothModule(this, device)
            updateUiVisibilityForModule()
        }

        btDialog = AlertDialog.Builder(this)
            .setTitle(R.string.bluetooth_devices)
            .setView(listView)
            .setNegativeButton(android.R.string.cancel, null)
            .show()

        bluetoothAdapter?.startDiscovery()
    }

    private fun showVehicleSelectionDialog() {
        val vehicules = listOf(
            Triple("Peugeot", "207", 2008),
            Triple("MG", "4", 2023),
            Triple("Ford", "Mustang Mach-E", 2021)
        )
        val labels = vehicules.map { "${it.first} ${it.second} ${it.third}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.select_vehicle)
            .setItems(labels) { _, which ->
                val v = vehicules[which]
                VehicleManager.setVehicle(v.first, v.second, v.third)
                updateUiVisibilityForModule()
            }
            .show()
    }

    private fun updateUiVisibilityForModule() {
        val isCanModule = currentModuleName.contains("CANBUS", true)
        requestVinButton.isVisible = currentModule != null || isCanModule
        requestPinButton.isVisible = isCanModule
        startCanListenButton.isVisible = isCanModule
    }

    private fun setupButtons() {
        connectButton.setOnClickListener { currentModule?.connect() }
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
            ReportGenerator.generate(this, currentModuleName, outputText.text.toString())
        }
    }
    private fun buildModuleForName(name: String) {
        currentModule = when (name) {
            getString(R.string.module_obd2_usb) -> Obd2UsbModule(this)
            getString(R.string.module_obd2_bluetooth) -> null // handled after BT selection
            getString(R.string.module_kline_usb) -> KLineUsbModule(this)
            getString(R.string.module_canbus) -> CanBusModule(this)
            getString(R.string.module_canbus_uart) -> CanBusUartModule(this)
            getString(R.string.module_can_demo) -> GenericCanDemoModule(this)
            else -> null
        }
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
        const val REQ_BT_PERMS = 1001
    }
}
