package com.example.voluntra_mad_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voluntra_mad_project.databinding.ActivityMainBinding
import com.example.voluntra_mad_project.models.Event
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var eventsAdapter: EventsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = applicationContext.packageName

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap()
        setupRecyclerView()
        loadEventsFromFirestore()
    }

    private fun setupMap() {
        val mapController = binding.mapView.controller
        mapController.setZoom(12.5)
        val startPoint = GeoPoint(19.0760, 72.8777) // Mumbai
        mapController.setCenter(startPoint)
    }

    private fun setupRecyclerView() {
        // The RecyclerView is inside the included layout, so we access it via binding.bottomSheet
        binding.bottomSheet.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            eventsAdapter = EventsAdapter(emptyList())
            adapter = eventsAdapter
        }
    }

    private fun loadEventsFromFirestore() {
        val db = Firebase.firestore
        db.collection("events")
            .get()
            .addOnSuccessListener { result ->
                val eventsList = result.toObjects(Event::class.java)
                eventsAdapter.submitList(eventsList)
                Log.d("MainActivity", "Successfully fetched ${eventsList.size} events.")
            }
            .addOnFailureListener { exception ->
                Log.w("MainActivity", "Error getting documents.", exception)
                Toast.makeText(this, "Failed to load events.", Toast.LENGTH_SHORT).show()
            }
    }

    // --- Menu Handling ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // --- Map Lifecycle ---
    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}