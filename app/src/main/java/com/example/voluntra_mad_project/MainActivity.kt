package com.example.voluntra_mad_project

import android.Manifest // Import Manifest
import android.content.Intent
import android.content.pm.PackageManager // Import PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts // Import ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voluntra_mad_project.databinding.ActivityMainBinding
import com.example.voluntra_mad_project.models.Event
import com.google.android.gms.location.FusedLocationProviderClient // Import FusedLocationProviderClient
import com.google.android.gms.location.LocationServices // Import LocationServices
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
    private val mapMarkers = mutableListOf<Marker>()
    private var userLocationMarker: Marker? = null // Marker for the user's location
    private val TAG = "MainActivity"

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
                fetchCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted.
                fetchCurrentLocation()
            }
            else -> {
                // No location access granted.
                Toast.makeText(this, "Location permission denied. Showing default location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // OSMDroid Configuration
        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = applicationContext.packageName

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMap()
        setupRecyclerView()
        setupFilterChips()
        loadEventsFromFirestore()
        setupMapLegend()

        // Ask for location permission as soon as the app starts.
        checkAndRequestLocationPermission()

        // --- Click Listeners ---
        binding.searchCard.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        binding.filterButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.fabMyLocation.setOnClickListener {
            checkAndRequestLocationPermission() // Re-center on user
        }
    }

    private fun checkAndRequestLocationPermission() {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {
            // Permission is already granted
            fetchCurrentLocation()
        } else {
            // Permission is not granted, request it
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchCurrentLocation() {
        // Check permissions again (required by Android framework)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "fetchCurrentLocation called without permissions.")
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    // Location found
                    val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                    animateToLocation(userGeoPoint, 14.5) // Zoom to city-level
                    updateUserLocationMarker(userGeoPoint) // Add/update blue marker
                    Toast.makeText(this, "Showing events near you", Toast.LENGTH_SHORT).show()
                } else {
                    // Location is null (e.g., GPS turned off, or new emulator)
                    Toast.makeText(this, "Could not get current location. Is GPS on?", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                // Handle failure
                Log.e(TAG, "Failed to get location", e)
                Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to add/move user's location marker
    private fun updateUserLocationMarker(geoPoint: GeoPoint) {
        // Remove the old marker if it exists
        userLocationMarker?.let { binding.mapView.overlays.remove(it) }

        // Create a new marker
        userLocationMarker = Marker(binding.mapView)
        userLocationMarker?.position = geoPoint
        userLocationMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        userLocationMarker?.title = "My Location"
        // Set the custom blue icon
        userLocationMarker?.icon = ContextCompat.getDrawable(this, R.drawable.ic_my_location_marker)

        // Add the new marker to the map
        binding.mapView.overlays.add(userLocationMarker)
        binding.mapView.invalidate() // Refresh the map
    }

    // Updated to accept a zoom level
    private fun animateToLocation(geoPoint: GeoPoint, zoomLevel: Double) {
        val mapController = binding.mapView.controller
        mapController.setZoom(zoomLevel) // Use the specified zoom level
        mapController.animateTo(geoPoint)
    }

    private fun setupMap() {
        val mapController = binding.mapView.controller
        mapController.setZoom(12.5)
        // This is the default location if permission is denied
        val startPoint = GeoPoint(19.0760, 72.8777)
        mapController.setCenter(startPoint)
        binding.mapView.setMultiTouchControls(true)
    }

    private fun setupRecyclerView() {
        binding.bottomSheet.recyclerViewEvents.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            eventsAdapter = EventsAdapter(emptyList()) { eventId ->
                val intent = Intent(this@MainActivity, EventDetailActivity::class.java)
                intent.putExtra("EVENT_ID", eventId)
                startActivity(intent)
            }
            adapter = eventsAdapter
        }
    }

    private fun setupFilterChips() {
        binding.bottomSheet.chipGroupFilters.setOnCheckedStateChangeListener { group, checkedIds ->
            val selectedCategory = when (checkedIds.firstOrNull()) {
                R.id.chip_environment -> "Environment"
                R.id.chip_education -> "Education"
                R.id.chip_community -> "Community"
                R.id.chip_health -> "Health"
                R.id.chip_animals -> "Animals"
                else -> null
            }
            Log.d(TAG, "Filter selected: $selectedCategory")
            loadEventsFromFirestore(category = selectedCategory)
        }
    }

    private fun loadEventsFromFirestore(
        category: String? = null,
        sortBy: String = "createdAt",
        sortDirection: Query.Direction = Query.Direction.DESCENDING
    ) {
        val db = Firebase.firestore
        var query: Query = db.collection("events")

        if (category != null) {
            Log.d(TAG, "Applying filter: category == $category")
            query = query.whereEqualTo("category", category)
        } else {
            Log.d(TAG, "Loading all categories (no filter)")
        }

        query = query.orderBy(sortBy, sortDirection)

        query.get()
            .addOnSuccessListener { result ->
                val eventsList = result.toObjects(Event::class.java)
                eventsAdapter.submitList(eventsList)
                updateMapMarkers(eventsList)
                Log.d(TAG, "Successfully fetched ${eventsList.size} events.")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting documents with filter '$category': ", exception)
                Toast.makeText(this, "Failed to load events.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateMapMarkers(events: List<Event>) {
        binding.mapView.overlays.removeAll(mapMarkers.toSet())
        mapMarkers.clear()

        val defaultMarkerIcon: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_marker_default)?.mutate()

        for (event in events) {
            val firestoreGeoPoint: FirestoreGeoPoint? = event.location
            firestoreGeoPoint?.let { fbGeoPoint ->
                val eventMarker = Marker(binding.mapView)
                val osmGeoPoint = GeoPoint(fbGeoPoint.latitude, fbGeoPoint.longitude)
                eventMarker.position = osmGeoPoint
                eventMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                eventMarker.title = event.title
                eventMarker.icon = getMarkerIconForCategory(event.category) ?: defaultMarkerIcon
                eventMarker.relatedObject = event.id

                eventMarker.setOnMarkerClickListener { marker, mapView ->
                    val clickedEventId = marker.relatedObject as? String
                    if (clickedEventId != null) {
                        val intent = Intent(this@MainActivity, EventDetailActivity::class.java)
                        intent.putExtra("EVENT_ID", clickedEventId)
                        startActivity(intent)
                    } else {
                        Log.e(TAG, "Marker click failed: Event ID was null in relatedObject")
                        Toast.makeText(mapView.context, "Error: Could not load event details", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                binding.mapView.overlays.add(eventMarker)
                mapMarkers.add(eventMarker)
            }
        }
        binding.mapView.invalidate()
    }

    private fun getMarkerIconForCategory(category: String?): Drawable? {
        val drawableResId = when (category?.lowercase(Locale.getDefault())) {
            "environment" -> R.drawable.ic_marker_environment
            "education" -> R.drawable.ic_marker_education
            "community" -> R.drawable.ic_marker_community
            "health" -> R.drawable.ic_marker_health
            "animals" -> R.drawable.ic_marker_animals
            else -> R.drawable.ic_marker_default
        }
        return ContextCompat.getDrawable(this, drawableResId)?.mutate()
    }

    private fun setupMapLegend() {
        val categories = mapOf(
            "Environment" to R.drawable.ic_marker_environment,
            "Education" to R.drawable.ic_marker_education,
            "Community" to R.drawable.ic_marker_community,
            "Health" to R.drawable.ic_marker_health,
            "Animals" to R.drawable.ic_marker_animals
        )

        binding.mapLegend.removeAllViews()

        for ((categoryName, drawableId) in categories) {
            val legendItemView = layoutInflater.inflate(R.layout.legend_item, binding.mapLegend, false)
            val markerIconDrawable = ContextCompat.getDrawable(this, drawableId)
            val categoryTextView = legendItemView.findViewById<TextView>(R.id.legend_category_text)

            val iconSize = try { resources.getDimensionPixelSize(R.dimen.legend_icon_size) } catch (e: Exception) { 32 }
            val padding = try { resources.getDimensionPixelSize(R.dimen.legend_drawable_padding) } catch (e: Exception) { 8 }

            markerIconDrawable?.setBounds(0, 0, iconSize, iconSize)
            categoryTextView.setCompoundDrawablesRelative(markerIconDrawable, null, null, null)
            categoryTextView.compoundDrawablePadding = padding
            categoryTextView.text = categoryName
            binding.mapLegend.addView(legendItemView)
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