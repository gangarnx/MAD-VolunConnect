package com.example.voluntra_mad_project

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var auth: FirebaseAuth // Declare FirebaseAuth

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        auth = FirebaseAuth.getInstance() // Initialize FirebaseAuth

        // Find the logout preference
        val logoutPreference: Preference? = findPreference("logout_preference")

        // Set a click listener
        logoutPreference?.setOnPreferenceClickListener {
            // Sign the user out
            auth.signOut()

            // Redirect to LoginActivity
            val intent = Intent(activity, LoginActivity::class.java)
            // Clear the activity stack so the user can't go back
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish() // Finish the SettingsActivity

            true // Indicate the click was handled
        }
    }
}