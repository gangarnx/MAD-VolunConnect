package com.example.voluntra_mad_project

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.voluntra_mad_project.databinding.ActivityProfileBinding
// TODO: Add Firebase Auth/Firestore imports

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: Fetch current user's data from Firestore and populate fields
        // TODO: Implement edit functionality for skills/interests
        // TODO: Fetch and display attendance history
    }
}