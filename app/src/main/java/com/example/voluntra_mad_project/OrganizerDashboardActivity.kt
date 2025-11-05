package com.example.voluntra_mad_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu // Import Menu
import android.view.MenuItem // Import MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voluntra_mad_project.databinding.ActivityOrganizerDashboardBinding
import com.example.voluntra_mad_project.models.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OrganizerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrganizerDashboardBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private lateinit var eventsAdapter: OrganizerEventsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrganizerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
        loadOrganizerEventsAndCounts() // Call the combined function

        binding.fabCreateEvent.setOnClickListener {
            startActivity(Intent(this, CreateEventActivity::class.java))
        }
    }

    // --- Menu Methods ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.organizer_menu, menu) // Inflate the new menu
        return true // Make sure this returns true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Ensure this structure returns a Boolean in all cases
        return when (item.itemId) {
            R.id.menu_logout_organizer -> {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true // Return true because we handled the click
            }
            else -> super.onOptionsItemSelected(item) // Use the default handler otherwise
        }
    }
    // --- End Menu Methods ---

    private fun setupRecyclerView() {
        // Initialize adapter with empty map for counts initially
        eventsAdapter = OrganizerEventsAdapter(emptyList(), emptyMap()) { event ->
            val intent = Intent(this, ViewRegistrationsActivity::class.java)
            intent.putExtra("EVENT_ID", event.id)
            intent.putExtra("EVENT_TITLE", event.title)
            startActivity(intent)
        }
        binding.recyclerViewMyEvents.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewMyEvents.adapter = eventsAdapter
    }

    private fun loadOrganizerEventsAndCounts() {
        val organizerId = auth.currentUser?.uid
        if (organizerId == null) {
            Toast.makeText(this, "Error: Not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Use Kotlin Coroutines for cleaner async handling
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Fetch the organizer's events
                val eventsSnapshot = db.collection("events")
                    .whereEqualTo("organizerId", organizerId)
                    .orderBy("eventDate", Query.Direction.ASCENDING)
                    .get()
                    .await() // Wait for the query to complete

                val eventsList = eventsSnapshot.toObjects(Event::class.java)
                val eventIds = eventsList.mapNotNull { it.id } // Get list of event IDs

                // 2. Fetch registration counts for those events
                val registrationCounts = mutableMapOf<String, Int>()
                if (eventIds.isNotEmpty()) {
                    // Fetch all registrations for the organizer's events
                    val registrationsSnapshot = db.collection("registrations")
                        .whereIn("eventId", eventIds.take(30)) // Limit 'in' query size if needed
                        .get()
                        .await()

                    // Count registrations per event
                    for(doc in registrationsSnapshot.documents) {
                        val eventId = doc.getString("eventId")
                        if(eventId != null) {
                            registrationCounts[eventId] = (registrationCounts[eventId] ?: 0) + 1
                        }
                    }
                }

                // 3. Update the UI on the main thread
                withContext(Dispatchers.Main) {
                    eventsAdapter.submitLists(eventsList, registrationCounts)
                    Log.d("OrganizerDashboard", "Loaded ${eventsList.size} events and counts.")
                }

            } catch (e: Exception) {
                Log.e("OrganizerDashboard", "Error loading events/counts", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OrganizerDashboardActivity, "Failed to load data.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadOrganizerEventsAndCounts()
    }
}