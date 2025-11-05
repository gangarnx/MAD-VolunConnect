package com.example.voluntra_mad_project

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voluntra_mad_project.databinding.ActivityViewRegistrationsBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ViewRegistrationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewRegistrationsBinding
    private lateinit var adapter: RegistrationAdapter
    private val db = Firebase.firestore
    private var eventId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewRegistrationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarRegistrations)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back button in toolbar

        // Get event details passed from OrganizerDashboardActivity
        eventId = intent.getStringExtra("EVENT_ID")
        val eventTitle = intent.getStringExtra("EVENT_TITLE")

        if (eventId == null) {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show()
            finish() // Close activity if no event ID is provided
            return
        }

        // Set the title in the UI
        binding.textEventTitleRegs.text = eventTitle ?: "Registrations"
        setupRecyclerView()
        loadRegistrations()
    }

    private fun setupRecyclerView() {
        adapter = RegistrationAdapter(emptyList()) // Initialize with an empty list
        binding.recyclerViewRegistrations.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRegistrations.adapter = adapter
    }

    private fun loadRegistrations() {
        db.collection("registrations")
            .whereEqualTo("eventId", eventId) // Filter registrations for this specific event
            .orderBy("registrationTime") // Show registrations in chronological order
            .get()
            .addOnSuccessListener { documents ->
                // Map the Firestore documents to our simple RegistrationInfo data class
                val registrationList = documents.mapNotNull { doc ->
                    RegistrationInfo(
                        volunteerName = doc.getString("volunteerName"),
                        volunteerEmail = doc.getString("volunteerEmail"),
                        volunteerPhone = doc.getString("volunteerPhone")
                    )
                }
                adapter.submitList(registrationList) // Update the RecyclerView adapter
                Log.d("ViewRegistrations", "Loaded ${registrationList.size} registrations for event $eventId")
            }
            .addOnFailureListener { e ->
                Log.e("ViewRegistrations", "Error loading registrations", e)
                Toast.makeText(this, "Failed to load registrations.", Toast.LENGTH_SHORT).show()
            }
    }

    // Handle the back button press in the toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Use onBackPressedDispatcher for modern back navigation
        return true
    }
}