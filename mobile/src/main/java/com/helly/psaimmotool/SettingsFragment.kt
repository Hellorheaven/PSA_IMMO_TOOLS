package com.helly.psaimmotool

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import com.helly.psaimmotool.BuildConfig
import androidx.preference.PreferenceFragmentCompat
import com.helly.psaimmotool.update.*
import com.helly.psaimmotool.utils.*
import androidx.preference.ListPreference




class SettingsFragment : PreferenceFragmentCompat() {


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.prefs, rootKey)

        findPreference<Preference>("pref_vehicle_editor")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), VehicleEditorActivity::class.java))
            true
        }
        // Sauvegarde du toggle autoscroll
        val autoScrollPref = findPreference<Preference>("pref_auto_scroll")
        autoScrollPref?.setOnPreferenceChangeListener { _, newValue ->
            UiUpdater.setAutoScrollEnabled(requireContext(), newValue as Boolean)
            true
        }
        findPreference<Preference>("edit_vehicles")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), VehicleEditorActivity::class.java))
            true
        }
        // Theme selector
        val themePref = findPreference<ListPreference>("theme_selector")
        themePref?.setOnPreferenceChangeListener { _, newValue ->
            LocaleUtils.setTheme(requireActivity(), newValue as String)
            true
        }

        // Language selector
        val langPref = findPreference<ListPreference>("language_selector")
        langPref?.setOnPreferenceChangeListener { _, newValue ->
            LocaleUtils.setLocaleAndRestart(requireActivity(), newValue as String)
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



    }
}



