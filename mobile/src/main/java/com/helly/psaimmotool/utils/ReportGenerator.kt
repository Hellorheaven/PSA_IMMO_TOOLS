package com.helly.psaimmotool.utils

import android.content.Context
import com.helly.psaimmotool.R
import com.helly.psaimmotool.modules.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportGenerator {

    fun generate(context: Context, moduleName: String, logs: String): String {
        val (brand, model, year) = VehicleManager.selectedVehicle
        val capabilities = VehicleCapabilities.getCapabilities(brand, model)
        val algoAvailable = PsaKeyCalculator.hasKeyAlgoFor(VehicleManager.selectedVehicle)

        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val safeDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val report = buildString {
            appendLine(context.getString(R.string.report_header))
            appendLine("${context.getString(R.string.report_date)} $date")
            appendLine("${context.getString(R.string.report_vehicle)} $brand $model $year")
            appendLine("${context.getString(R.string.report_module)} $moduleName")
            appendLine(context.getString(R.string.report_capabilities))
            appendLine("CAN: ${capabilities?.supportsCan}, OBD2: ${capabilities?.supportsObd2}, K-Line: ${capabilities?.supportsKLine}")
            appendLine("${context.getString(R.string.report_modules)} ${capabilities?.compatibleModules?.joinToString(", ")}")
            if (algoAvailable) {
                val last = PsaKeyCalculator.lastCalculation
                if (last != null) {
                    appendLine(context.getString(R.string.report_seed_received, last.first))
                    appendLine(context.getString(R.string.report_key_calculated, last.second))
                }
            }
            appendLine()
            appendLine(context.getString(R.string.report_logs_section))
            appendLine(logs)
        }

        return try {
            val dir = File(context.getExternalFilesDir(null), "PSAImmoTool")
            if (!dir.exists()) dir.mkdirs()
            val fileName = "rapport_${safeDate}_${brand}_${model}.txt"
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(report.toByteArray()) }

            context.getString(R.string.report_saved, file.absolutePath)
        } catch (e: Exception) {
            context.getString(R.string.report_error, e.message ?: "")
        }
    }
}
