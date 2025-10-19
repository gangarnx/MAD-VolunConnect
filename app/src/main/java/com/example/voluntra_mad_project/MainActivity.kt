package com.example.voluntra_mad_project

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voluntra_mad_project.databinding.ActivityMainBinding
import com.example.voluntra_mad_project.models.Event
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
        loadDummyData()

        // This listener for the left button already exists
        binding.profileButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // --- THIS IS THE NEW CODE ---
        // Add a click listener to the right button to also open SettingsActivity
        binding.filterButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun setupMap() {
        val mapController = binding.mapView.controller
        mapController.setZoom(12.5)
        val startPoint = GeoPoint(19.0760, 72.8777) // Mumbai
        mapController.setCenter(startPoint)
    }

    private fun setupRecyclerView() {
        binding.bottomSheet.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            eventsAdapter = EventsAdapter(emptyList())
            adapter = eventsAdapter
        }
    }

    private fun loadDummyData() {
        val dummyEvents = listOf(
            Event(id = "1", title = "Art Workshop at Gateway", organizationName = "Mumbai Arts Foundation", imageUrl = "https://i.imgur.com/L72E3qA.jpeg"),
            Event(id = "2", title = "Heritage Walk in Fort", organizationName = "History Buffs Club", imageUrl = "https://i.imgur.com/bXn3pZv.jpeg"),
            Event(id = "3", title = "Mangrove Cleanup Drive", organizationName = "Coastal Conservation Crew", imageUrl = "https://i.imgur.com/L72E3qA.jpeg"),
            Event(id = "4", title = "Food Distribution for the Needy", organizationName = "Community Kitchen", imageUrl = "https://i.imgur.com/bXn3pZv.jpeg")
        )
        eventsAdapter.submitList(dummyEvents)
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