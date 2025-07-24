package com.helly.psaimmotool.utils

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.helly.psaimmotool.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object LogExporter {

    fun exportLogs(context: Context, content: String) {
        try {
            val dir = File(context.getExternalFilesDir(null), "PSAImmoTool")
            if (!dir.exists()) dir.mkdirs()

            val date = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "log_$date.txt"
            val file = File(dir, fileName)

            FileOutputStream(file).use { it.write(content.toByteArray()) }

            Toast.makeText(
                context,
                context.getString(R.string.log_saved_success, file.absolutePath),
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.log_saved_error, e.message ?: "unknown"),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
