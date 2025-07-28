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


    fun setTheme(activity: Activity, themeValue: String) {
    when (themeValue) {
        "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}
    /**
     * Change the app locale and restart the activity to apply the new locale.
     *
     * @param activity The activity to restart.
     * @param langCode The language code to set (e.g., "en", "fr").
     */

    fun setLocaleAndRestart(activity: Activity, langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration()
        config.setLocale(locale)
        activity.baseContext.resources.updateConfiguration(config, activity.baseContext.resources.displayMetrics)

        val intent = Intent(activity, activity.javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
        activity.finish()
    }


}
