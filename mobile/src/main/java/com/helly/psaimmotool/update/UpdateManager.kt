package com.helly.psaimmotool.update

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.helly.psaimmotool.BuildConfig
import com.helly.psaimmotool.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateManager {
    private const val VERSION_URL = "https://raw.githubusercontent.com/Hellorheaven/PSA_IMMO_TOOLS/main/mobile/src/version.txt"
    private const val APK_URL = "https://github.com/Hellorheaven/PSA_IMMO_TOOLS/raw/main/mobile/release/mobile-release.apk"

    fun checkForUpdate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val connection = URL(VERSION_URL).openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val remoteVersion = connection.inputStream.bufferedReader().readText().trim()
                    val localVersion = BuildConfig.VERSION_NAME

                    if (isNewerVersionAvailable(localVersion, remoteVersion)) {
                        (context as? Activity)?.runOnUiThread {
                            promptUpdate(context, remoteVersion)
                        }
                    } else {
                        (context as? Activity)?.runOnUiThread {
                            Toast.makeText(
                                context,
                                context.getString(R.string.update_latest_version, localVersion),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    showError(context, context.getString(R.string.update_error_check))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError(context, context.getString(R.string.update_error_check))
            }
        }
    }

    private fun isNewerVersionAvailable(local: String, remote: String): Boolean {
        val localParts = local.split(".").mapNotNull { it.toIntOrNull() }
        val remoteParts = remote.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0..2) {
            val localVal = localParts.getOrElse(i) { 0 }
            val remoteVal = remoteParts.getOrElse(i) { 0 }
            if (remoteVal > localVal) return true
            if (remoteVal < localVal) return false
        }
        return false
    }

    private fun promptUpdate(context: Context, remoteVersion: String) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.update_title))
            .setMessage(context.getString(R.string.update_message, remoteVersion))
            .setPositiveButton(android.R.string.ok) { _, _ -> downloadAndInstallApk(context) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun downloadAndInstallApk(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apkUrl = URL(APK_URL)
                val connection = apkUrl.openConnection() as HttpURLConnection
                connection.connect()

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    showError(context, context.getString(R.string.update_download_failed))
                    return@launch
                }

                val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
                connection.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                    showError(context, context.getString(R.string.update_open_browser_failed))
                }

            } catch (e: Exception) {
                e.printStackTrace()
                showError(context, context.getString(R.string.update_download_failed))
            }
        }
    }

    private fun showError(context: Context, message: String) {
        (context as? Activity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
