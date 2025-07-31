package com.helly.psaimmotool

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import com.helly.psaimmotool.modules.PsaKeyCalculator
import com.helly.psaimmotool.utils.VehicleCapabilitiesEditor
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class VehicleEditorActivity : Activity() {

    private lateinit var brandField: EditText
    private lateinit var modelField: EditText
    private lateinit var yearField: EditText
    private lateinit var appKeyField: EditText

    private lateinit var checkCan: CheckBox
    private lateinit var checkObd2: CheckBox
    private lateinit var checkKLine: CheckBox
    private lateinit var moduleSpinner: Spinner
    private lateinit var addModuleButton: Button
    private lateinit var moduleListText: TextView

    private lateinit var saveButton: Button
    private lateinit var statusText: TextView

    private val selectedModules = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vehicle_editor)

        brandField = findViewById(R.id.editBrand)
        modelField = findViewById(R.id.editModel)
        yearField = findViewById(R.id.editYear)
        appKeyField = findViewById(R.id.editAppKey)

        checkCan = findViewById(R.id.checkCan)
        checkObd2 = findViewById(R.id.checkObd2)
        checkKLine = findViewById(R.id.checkKLine)

        moduleSpinner = findViewById(R.id.moduleSpinner)
        addModuleButton = findViewById(R.id.addModuleButton)
        moduleListText = findViewById(R.id.moduleListText)

        saveButton = findViewById(R.id.saveButton)
        statusText = findViewById(R.id.statusText)

        // Remplir la liste des modules depuis le XML
        val modules = resources.getStringArray(R.array.module_list)
        moduleSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modules)

        addModuleButton.setOnClickListener {
            val module = moduleSpinner.selectedItem.toString()
            selectedModules.add(module)
            moduleListText.text = selectedModules.joinToString(", ")
        }

        saveButton.setOnClickListener {
            val brand = brandField.text.toString().trim()
            val model = modelField.text.toString().trim()
            val year = yearField.text.toString().toIntOrNull()
            val appKey = appKeyField.text.toString().uppercase().trim()

            if (brand.isBlank() || model.isBlank() || year == null || appKey.length != 4) {
                statusText.text = getString(R.string.invalid_vehicle_data)
                return@setOnClickListener
            }

            val vehicle = Triple(brand, model, year)

            // Ajout AppKey
            PsaKeyCalculator.addVehicleKey(vehicle, appKey)

            // Ajout Capacités
            VehicleCapabilitiesEditor.registerVehicle(
                    brand = brand,
                    model = model,
                    supportsCan = checkCan.isChecked,
                    supportsObd2 = checkObd2.isChecked,
                    supportsKLine = checkKLine.isChecked,
                    compatibleModules = selectedModules.toList()
                )


            // Sauvegarde persistante locale
            saveVehicleToJson(vehicle, appKey)

            statusText.text = getString(R.string.vehicle_saved, brand, model, year)
        }
    }

    private fun saveVehicleToJson(vehicle: Triple<String, String, Int>, appKey: String) {
        val dir = File(filesDir, "psa_data")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "vehicles.json")

        val array = if (file.exists()) JSONArray(file.readText()) else JSONArray()

        val obj = JSONObject().apply {
            put("brand", vehicle.first)
            put("model", vehicle.second)
            put("year", vehicle.third)
            put("appKey", appKey)
            put("modules", JSONArray(selectedModules))
            put("can", checkCan.isChecked)
            put("obd2", checkObd2.isChecked)
            put("kline", checkKLine.isChecked)
        }

        array.put(obj)
        file.writeText(array.toString(2))
    }
}
