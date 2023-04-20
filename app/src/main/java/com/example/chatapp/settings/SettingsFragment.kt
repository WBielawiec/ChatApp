package com.example.chatapp.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.example.chatapp.R
import com.example.chatapp.chat.GroupChatFragmentDirections
import kotlinx.android.synthetic.main.settings_toolbar.view.*

class SettingsFragment : PreferenceFragmentCompat() {


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            //Toast.makeText(context, "back pressed", Toast.LENGTH_LONG).show()
            val action = SettingsFragmentDirections.actionSettingsFragmentToNavigationFragment()
            findNavController().navigate(action)
        }
        callback.isEnabled

        val accountPreference = findPreference<Preference>("accountSettings")
        accountPreference?.setOnPreferenceClickListener {
            val action = SettingsFragmentDirections.actionSettingsFragmentToAccountSettingsFragment()
            findNavController().navigate(action)
            true
        }

        val blockedUsersPreference = findPreference<Preference>("blockedUsers")
        blockedUsersPreference?.setOnPreferenceClickListener {
            val action = SettingsFragmentDirections.actionSettingsFragmentToBlockedUsersFragment()
            findNavController().navigate(action)
            true
        }

        val themeSwitchPreference = findPreference<SwitchPreference>("darkTheme")
        themeSwitchPreference?.setOnPreferenceChangeListener { preference, newValue ->
            if(AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }

            true
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val defaultView = super.onCreateView(inflater, container, savedInstanceState)

        val newLayout = inflater.inflate(R.layout.settings_toolbar, container, false) as ViewGroup
        newLayout.addView(defaultView)
        return newLayout
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.titleTextView_settings_toolbar.text = getString(R.string.settings)
        view.backImageView_settings_toolbar.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}