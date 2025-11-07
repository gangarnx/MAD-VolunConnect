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

    // --- THIS IS THE CORRECTED LINE ---
    private lateinit var binding: ActivityViewRegistrationsBinding
    // --- END CORRECTION ---

    private lateinit var adapter: RegistrationAdapter
    private val db = Firebase.firestore
    private var eventId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewRegistrationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarRegistrations)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        eventId = intent.getStringExtra("EVENT_ID")
        val eventTitle = intent.getStringExtra("EVENT_TITLE")

        if (eventId == null) {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.textEventTitleRegs.text = eventTitle ?: "Registrations"
        setupRecyclerView()
        loadRegistrations()
    }

    private fun setupRecyclerView() {
        adapter = RegistrationAdapter(emptyList())
        binding.recyclerViewRegistrations.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRegistrations.adapter = adapter
    }

    private fun loadRegistrations() {
        db.collection("registrations")
            .whereEqualTo("eventId", eventId)
            .orderBy("registrationTime")
            .get()
            .addOnSuccessListener { documents ->
                // Map the Firestore documents to our RegistrationInfo data class
                val registrationList = documents.mapNotNull { doc ->
                    RegistrationInfo(
                        volunteerName = doc.getString("volunteerName"),
                        volunteerEmail = doc.getString("volunteerEmail"),
                        volunteerPhone = doc.getString("volunteerPhone"),
                        emergencyContactName = doc.getString("emergencyContactName"),
                        emergencyContactPhone = doc.getString("emergencyContactPhone")
                    )
                }
                adapter.submitList(registrationList)
                Log.d("ViewRegistrations", "Loaded ${registrationList.size} registrations")
            }
            .addOnFailureListener { e ->
                Log.e("ViewRegistrations", "Error loading registrations", e)
                Toast.makeText(this, "Failed to load registrations.", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}