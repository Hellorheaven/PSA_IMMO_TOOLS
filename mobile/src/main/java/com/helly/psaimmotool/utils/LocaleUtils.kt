@file:Suppress("DEPRECATION")

package com.helly.psaimmotool.utils

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import java.util.Locale
import androidx.appcompat.app.AppCompatDelegate

object LocaleUtils {
//    fun setLocaleAndRestart(activity: Activity, languageCode: String) {
//        val locale = Locale(languageCode)
//        Locale.setDefault(locale)
//
//        val config = Configuration()
//        config.setLocale(locale)
//
//        val resources = activity.resources
//        @Suppress("DEPRECATION")
//        resources.updateConfiguration(config, resources.displayMetrics)
//
//        // Redémarre l'activité pour appliquer la langue
//        val refresh = Intent(activity, activity::class.java)
//        refresh.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
//        activity.startActivity(refresh)
//        activity.finish()
//    }
fun setTheme(mode: String) {
    when (mode) {
        "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}
    fun setLocaleAndRestart(activity: Activity, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = activity.resources.configuration
        config.setLocale(locale)

        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(config, activity.resources.displayMetrics)

        // Redémarre sans NEW_TASK
        val intent = activity.intent
        activity.finish()
        activity.startActivity(intent)
    }
}
