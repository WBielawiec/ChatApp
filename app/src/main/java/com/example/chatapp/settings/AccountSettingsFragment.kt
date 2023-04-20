package com.example.chatapp.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.chatapp.R
import kotlinx.android.synthetic.main.settings_toolbar.view.*

class AccountSettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.account_settings, rootKey)

        val avatarPreference = findPreference<Preference>("avatarSettings")
        avatarPreference?.setOnPreferenceClickListener {
            val action = AccountSettingsFragmentDirections.actionAccountSettingsFragmentToAvatarChangeFragment()
            findNavController().navigate(action)
            true
        }

        val informationPreference = findPreference<Preference>("informationSettings")
        informationPreference?.setOnPreferenceClickListener {
            val action = AccountSettingsFragmentDirections.actionAccountSettingsFragmentToInformationChangeFragment()
            findNavController().navigate(action)
            true
        }

        val passwordPreference = findPreference<Preference>("passwordSettings")
        passwordPreference?.setOnPreferenceClickListener {
            val action = AccountSettingsFragmentDirections.actionAccountSettingsFragmentToPasswordChangeFragment()
            findNavController().navigate(action)
            true
        }

        val deletePreference = findPreference<Preference>("deleteAccount")
        deletePreference?.setOnPreferenceClickListener {
            val action = AccountSettingsFragmentDirections.actionAccountSettingsFragmentToDeleteUserFragment()
            findNavController().navigate(action)
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

        view.titleTextView_settings_toolbar.text = getString(R.string.account_settings)
        view.backImageView_settings_toolbar.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}