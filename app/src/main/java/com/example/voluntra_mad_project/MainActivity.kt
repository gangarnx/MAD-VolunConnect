package com.example.voluntra_mad_project

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout // Import LinearLayout for the legend
import android.widget.TextView // Import TextView for legend items
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voluntra_mad_project.databinding.ActivityMainBinding
import com.example.voluntra_mad_project.models.Event
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.util.Locale

// Alias Firestore GeoPoint to avoid name collision
typealias FirestoreGeoPoint = com.google.firebase.firestore.GeoPoint

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var eventsAdapter: EventsAdapter
    private val mapMarkers = mutableListOf<Marker>() // Keep track of markers

    // Define a TAG for logging
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMDroid Configuration
        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = applicationContext.packageName

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap()
        setupRecyclerView()
        setupFilterChips()
        loadEventsFromFirestore() // Load initial unfiltered list
        setupMapLegend() // Setup the map legend

        // --- Click Listeners ---
        // Make the entire search card clickable to open SearchActivity
        binding.searchCard.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        binding.profileButton.setOnClickListener {
            // Navigate to ProfileActivity
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.filterButton.setOnClickListener {
            // Open Settings for now, could be a dedicated filter later
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.fabMyLocation.setOnClickListener {
            // TODO: Implement logic to get current location and center map
            Toast.makeText(this, "My Location Button Clicked (Placeholder)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMap() {
        val mapController = binding.mapView.controller
        mapController.setZoom(12.5)
        val startPoint = GeoPoint(19.0760, 72.8777) // Mumbai (Using OSMDroid GeoPoint)
        mapController.setCenter(startPoint)
        binding.mapView.setMultiTouchControls(true) // Enable zoom gestures
    }

    private fun setupRecyclerView() {
        // Ensure the RecyclerView in bottom_sheet_persistent.xml has the ID recycler_view_events
        binding.bottomSheet.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            // Pass the click listener lambda to the adapter
            eventsAdapter = EventsAdapter(emptyList()) { eventId ->
                val intent = Intent(this@MainActivity, EventDetailActivity::class.java)
                intent.putExtra("EVENT_ID", eventId)
                startActivity(intent)
            }
            adapter = eventsAdapter
        }
    }

    private fun setupFilterChips() {
        // Make sure the ChipGroup in bottom_sheet_persistent.xml has the ID chipGroupFilters
        binding.bottomSheet.chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            // checkedIds is a List<Int>, usually contains 0 or 1 ID for singleSelection mode
            val selectedCategory = when (checkedIds.firstOrNull()) {
                R.id.chip_environment -> "Environment"
                R.id.chip_education -> "Education"
                R.id.chip_community -> "Community"
                R.id.chip_health -> "Health"
                R.id.chip_animals -> "Animals"
                else -> null // For "All" chip (R.id.chip_all) or if none selected
            }
            Log.d(TAG, "Filter selected: $selectedCategory") // Log which category was selected
            loadEventsFromFirestore(category = selectedCategory) // Reload events, passing null if "All" was selected
        }
    }

    // Modified function to accept filters/sorting
    private fun loadEventsFromFirestore(
        category: String? = null,
        sortBy: String = "createdAt", // Default sort by creation time
        sortDirection: Query.Direction = Query.Direction.DESCENDING // Show newest first
    ) {
        val db = Firebase.firestore
        var query: Query = db.collection("events")

        // Apply category filter if one is selected
        if (category != null) {
            Log.d(TAG, "Applying filter: category == $category") // Log filter being applied
            query = query.whereEqualTo("category", category)
        } else {
            Log.d(TAG, "Loading all categories (no filter)")
        }

        // Apply sorting
        query = query.orderBy(sortBy, sortDirection)

        query.get()
            .addOnSuccessListener { result ->
                val eventsList = result.toObjects(Event::class.java)
                eventsAdapter.submitList(eventsList)
                updateMapMarkers(eventsList) // Update markers based on fetched events
                Log.d(TAG, "Successfully fetched ${eventsList.size} events.")
            }
            .addOnFailureListener { exception ->
                // Log the detailed error
                Log.e(TAG, "Error getting documents with filter '$category': ", exception)
                Toast.makeText(this, "Failed to load events.", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to handle map markers
    private fun updateMapMarkers(events: List<Event>) {
        // 1. Clear existing markers from the map overlay and our tracking list
        binding.mapView.overlays.removeAll(mapMarkers.toSet()) // Use toSet() for efficient removal if overlays changed elsewhere
        mapMarkers.clear()

        // Get the default marker icon once if needed for fallback
        val defaultMarkerIcon: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_marker_default)?.mutate()

        // 2. Add new markers for events that have a location
        for (event in events) {
            // Use the aliased FirestoreGeoPoint type here
            val firestoreGeoPoint: FirestoreGeoPoint? = event.location
            firestoreGeoPoint?.let { fbGeoPoint -> // Only proceed if location is not null
                val eventMarker = Marker(binding.mapView)

                // Convert Firestore GeoPoint to OSMDroid GeoPoint
                val osmGeoPoint = GeoPoint(fbGeoPoint.latitude, fbGeoPoint.longitude)
                eventMarker.position = osmGeoPoint // Set marker position

                eventMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                eventMarker.title = event.title // Text shown on tap

                // Get and set the colored marker icon based on event category
                eventMarker.icon = getMarkerIconForCategory(event.category) ?: defaultMarkerIcon

                // Store event ID in the marker's related object for retrieval on click
                eventMarker.relatedObject = event.id

                // Set click listener for the marker
                eventMarker.setOnMarkerClickListener { marker, mapView ->
                    val clickedEventId = marker.relatedObject as? String // Retrieve the stored ID
                    if (clickedEventId != null) {
                        // Open EventDetailActivity with the ID
                        val intent = Intent(this@MainActivity, EventDetailActivity::class.java)
                        intent.putExtra("EVENT_ID", clickedEventId)
                        startActivity(intent)
                    } else {
                        Log.e(TAG, "Marker click failed: Event ID was null in relatedObject")
                        Toast.makeText(mapView.context, "Error: Could not load event details", Toast.LENGTH_SHORT).show()
                    }
                    true // Indicate that the click event is handled
                }

                // Add the configured marker to the map's overlays and our tracking list
                binding.mapView.overlays.add(eventMarker)
                mapMarkers.add(eventMarker)
            }
        }

        // 3. Refresh the map view to make the new markers visible
        binding.mapView.invalidate()
    }

    // Helper function to get the correct colored marker icon based on category
    private fun getMarkerIconForCategory(category: String?): Drawable? {
        val drawableResId = when (category?.lowercase(Locale.getDefault())) { // Use lowercase for case-insensitive matching
            "environment" -> R.drawable.ic_marker_environment
            "education" -> R.drawable.ic_marker_education
            "community" -> R.drawable.ic_marker_community
            "health" -> R.drawable.ic_marker_health
            "animals" -> R.drawable.ic_marker_animals
            else -> R.drawable.ic_marker_default // Fallback for null or unknown categories
        }
        // Use mutate() to ensure each marker gets its own drawable state if needed (e.g., for selection)
        return ContextCompat.getDrawable(this, drawableResId)?.mutate()
    }

    // Setup function for the map legend
    private fun setupMapLegend() {
        // Define the categories and their corresponding marker drawables
        val categories = mapOf(
            "Environment" to R.drawable.ic_marker_environment,
            "Education" to R.drawable.ic_marker_education,
            "Community" to R.drawable.ic_marker_community,
            "Health" to R.drawable.ic_marker_health,
            "Animals" to R.drawable.ic_marker_animals,
            "Other" to R.drawable.ic_marker_default // For default/unspecified
        )

        binding.mapLegend.removeAllViews() // Clear previous items if legend is rebuilt

        for ((categoryName, drawableId) in categories) {
            // Inflate the legend_item layout for each category
            val legendItemView = layoutInflater.inflate(R.layout.legend_item, binding.mapLegend, false)
            val markerIconDrawable = ContextCompat.getDrawable(this, drawableId)
            val categoryTextView = legendItemView.findViewById<TextView>(R.id.legend_category_text)

            // Optional: Scale the icon down for the legend display
            val iconSize = resources.getDimensionPixelSize(R.dimen.legend_icon_size) // Define legend_icon_size in dimens.xml (e.g., 16dp)
            markerIconDrawable?.setBounds(0, 0, iconSize, iconSize)

            // Set the icon to the left of the text
            categoryTextView.setCompoundDrawablesRelative(markerIconDrawable, null, null, null) // Use Relative for LTR/RTL support
            categoryTextView.compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.legend_drawable_padding) // Define padding in dimens.xml

            categoryTextView.text = categoryName // Set the category name text
            binding.mapLegend.addView(legendItemView) // Add the item view to the legend container
        }
    }

    // --- Map Lifecycle Methods ---
    override fun onResume() {
        super.onResume()
        binding.mapView.onResume() // Required for osmdroid
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause() // Required for osmdroid
    }
}