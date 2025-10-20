package com.example.voluntra_mad_project

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.voluntra_mad_project.databinding.ActivityOrganizerDashboardBinding

class OrganizerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrganizerDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrganizerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the toolbar
        setSupportActionBar(binding.toolbar)

        // Set a click listener for the Floating Action Button
        binding.fabCreateEvent.setOnClickListener {
            startActivity(Intent(this, CreateEventActivity::class.java))
        }

        // TODO: Later, we will add code here to fetch and display
        // the events created by this specific organizer in the RecyclerView.
    }
}