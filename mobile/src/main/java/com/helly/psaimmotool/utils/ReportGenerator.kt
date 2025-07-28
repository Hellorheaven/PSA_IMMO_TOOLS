package com.helly.psaimmotool.utils

import android.content.Context
import com.helly.psaimmotool.R

import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportGenerator {

    fun generate(context: Context, moduleName: String, logs: String) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val safeDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val (brand, model, year) = VehicleManager.selectedVehicle
        val vehicle = "$brand $model $year"
        val module = moduleName.ifBlank { context.getString(R.string.module_unknown) }

        val connectionStatus =
            context.getString(R.string.connection_success) // ou autre selon ton état

        val capabilities = VehicleCapabilities.getCapabilities(brand, model)
        val supportsCan = capabilities?.supportsCan?.toString() ?: "N/A"
        val supportsObd2 = capabilities?.supportsObd2?.toString() ?: "N/A"
        val supportsKLine = capabilities?.supportsKLine?.toString() ?: "N/A"
        val compatibleModules = capabilities?.compatibleModules?.joinToString(", ") ?: "N/A"

        val report = StringBuilder()
        report.appendLine(context.getString(R.string.report_header))
        report.appendLine("${context.getString(R.string.report_date)} $date")
        report.appendLine("${context.getString(R.string.report_vehicle)} $vehicle")
        report.appendLine("${context.getString(R.string.report_module)} $module")
        report.appendLine("${context.getString(R.string.report_connection)} $connectionStatus")
        report.appendLine(context.getString(R.string.report_capabilities))
        report.appendLine("CAN: $supportsCan, OBD2: $supportsObd2, K-Line: $supportsKLine")
        report.appendLine("${context.getString(R.string.report_modules)} $compatibleModules")

        val lastSeedAndKey = PsaKeyCalculator.lastCalculation
        if (lastSeedAndKey != null) {
            report.appendLine(context.getString(R.string.report_seed_received, lastSeedAndKey.first))
            report.appendLine(context.getString(R.string.report_key_calculated, lastSeedAndKey.second))
        }

        report.appendLine()
        report.appendLine(context.getString(R.string.report_pid_section))
        report.appendLine(DiagnosticRecorder.getDecodedSummary())
        report.appendLine()
        report.appendLine(context.getString(R.string.report_dtc_section))
        report.appendLine(DiagnosticRecorder.getDtcSummary())
        report.appendLine()
        report.appendLine(context.getString(R.string.report_logs_section))
        report.appendLine(logs)

        try {
            val dir = File(context.getExternalFilesDir(null), "PSAImmoTool")
            if (!dir.exists()) dir.mkdirs()
            val fileName = "rapport_${safeDate}_${brand}_${model}.txt"
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(report.toString().toByteArray()) }
            UiUpdater.appendLog(null, context.getString(R.string.report_saved, file.absolutePath))
        } catch (e: Exception) {
            UiUpdater.appendLog(null, context.getString(R.string.report_error, e.message ?: ""))
        }
    }
}
