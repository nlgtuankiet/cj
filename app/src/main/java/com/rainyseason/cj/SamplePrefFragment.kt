package com.rainyseason.cj

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SamplePrefFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val screen = preferenceManager.createPreferenceScreen(requireContext())

        val samplePref = Preference(context).apply {
            key = "b"
            title = "Title"
            summary = "Summary"
        }

        screen.addPreference(samplePref)

        val listPref = ListPreference(context).apply {
            key = "a"
            title = "number of decimal"
            entries = (1..100).map { "entry $it" }.toTypedArray()
            entryValues = (1..100).map { "value $it" }.toTypedArray()
            dialogTitle = "number of decimal"
        }

        screen.addPreference(listPref)

        preferenceScreen = screen


    }
}