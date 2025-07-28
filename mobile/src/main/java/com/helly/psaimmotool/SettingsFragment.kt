package com.helly.psaimmotool

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import com.helly.psaimmotool.BuildConfig
import androidx.preference.PreferenceFragmentCompat
import com.helly.psaimmotool.update.UpdateManager
import com.helly.psaimmotool.utils.UiUpdater
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.helly.psaimmotool.utils.LocaleUtils
import com.helly.psaimmotool.utils.VehicleManager

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs, rootKey)

        findPreference<Preference>("pref_vehicle_editor")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), VehicleEditorActivity::class.java))
            true
        }

        findPreference<androidx.preference.Preference>("pref_theme_light")?.setOnPreferenceClickListener {
            LocaleUtils.setTheme(requireActivity(), false)
            true
        }

        findPreference<androidx.preference.Preference>("pref_theme_dark")?.setOnPreferenceClickListener {
            LocaleUtils.setTheme(requireActivity(), true)
            true
        }

        findPreference<androidx.preference.Preference>("pref_language_fr")?.setOnPreferenceClickListener {
            LocaleUtils.setLocaleAndRestart(requireActivity(), "fr")
            true
        }

        findPreference<androidx.preference.Preference>("pref_language_en")?.setOnPreferenceClickListener {
            LocaleUtils.setLocaleAndRestart(requireActivity(), "en")
            true
        }
        findPreference<Preference>("pref_update_app")?.setOnPreferenceClickListener {
            UpdateManager.checkForUpdate(requireContext())
            true
        }
        // Mise à jour bouton
        findPreference<Preference>("pref_check_update")?.setOnPreferenceClickListener {
            UpdateManager.checkForUpdate(requireContext())
            true
        }

        // Affichage version actuelle (texte seul)

        findPreference<Preference>("pref_version")?.summary =
            "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"

        // Sauvegarde du toggle autoscroll
        val autoScrollPref = findPreference<Preference>("pref_auto_scroll")
        autoScrollPref?.setOnPreferenceChangeListener { _, newValue ->
            UiUpdater.setAutoScrollEnabled(requireContext(), newValue as Boolean)
            true
        }
    }
}



