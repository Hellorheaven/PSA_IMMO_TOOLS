package com.helly.psaimmotool.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.helly.psaimmotool.BuildConfig
import com.helly.psaimmotool.R
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object UpdateManager {

    private const val VERSION_URL =
        "https://raw.githubusercontent.com/Hellorheaven/PSA_IMMO_TOOLS/master/mobile/src/version.txt"
    private const val APK_URL =
        "https://raw.githubusercontent.com/Hellorheaven/PSA_IMMO_TOOLS/master/mobile/release/mobile-release.apk"
    private const val APK_FILE_NAME = "mobile-release.apk"
    private const val MIN_APK_SIZE_BYTES = 1_000_000

    private val io = Executors.newSingleThreadExecutor()

    fun showUpdateDialog(activity: Activity) {
        val current = BuildConfig.VERSION_NAME
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.update_title))
            .setMessage(activity.getString(R.string.update_current_version, current))
            .setPositiveButton(R.string.update_check) { _, _ -> checkForUpdate(activity) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun checkForUpdate(activity: Activity) {
        io.execute {
            val remote = try { fetchRemoteVersion() } catch (_: Exception) { null }
            activity.runOnUiThread {
                if (remote == null) {
                    showError(activity, R.string.update_error_fetch)
                } else if (isNewer(remote, BuildConfig.VERSION_NAME)) {
                    promptDownload(activity, remote)
                } else {
                    AlertDialog.Builder(activity)
                        .setTitle(R.string.update_latest_title)
                        .setMessage(activity.getString(R.string.update_latest, BuildConfig.VERSION_NAME))
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }
    }

    private fun fetchRemoteVersion(): String? {
        val conn = URL(VERSION_URL).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        return conn.inputStream.bufferedReader().use { it.readLine()?.trim() }
    }

    private fun isNewer(remote: String, local: String): Boolean {
        fun parse(v: String) = v.split(".", "-").mapNotNull { it.toIntOrNull() }
        val r = parse(remote)
        val l = parse(local)
        val max = maxOf(r.size, l.size)
        for (i in 0 until max) {
            val rv = r.getOrNull(i) ?: 0
            val lv = l.getOrNull(i) ?: 0
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }

    private fun promptDownload(activity: Activity, remoteVersion: String) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.update_available_title)
            .setMessage(activity.getString(R.string.update_available, remoteVersion))
            .setPositiveButton(R.string.update_download) { _, _ -> manualDownloadApk(activity) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun manualDownloadApk(activity: Activity) {
        io.execute {
            try {
                val apkFile = File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILE_NAME)
                val conn = URL(APK_URL).openConnection() as HttpURLConnection
                conn.connect()
                conn.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output -> input.copyTo(output) }
                }
                if (apkFile.length() < MIN_APK_SIZE_BYTES) {
                    activity.runOnUiThread {
                        showError(activity, R.string.update_parse_error_fallback)
                    }
                } else {
                    activity.runOnUiThread {
                        installApk(activity, apkFile)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity.runOnUiThread {
                    showError(activity, R.string.update_error_fetch)
                }
            }
        }
    }

    private fun installApk(activity: Activity, apkFile: File) {
        val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        activity.startActivity(intent)
    }

    private fun showError(activity: Activity, msgRes: Int) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.update_error_title)
            .setMessage(activity.getString(msgRes))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
