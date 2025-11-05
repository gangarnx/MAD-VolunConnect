package com.example.voluntra_mad_project

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voluntra_mad_project.databinding.ActivityMainBinding
import com.example.voluntra_mad_project.models.Event
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    private val TAG = "MainActivity"

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                fetchCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                fetchCurrentLocation()
            }
            else -> {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = applicationContext.packageName

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMap()
        setupRecyclerView()
        setupFilterChips()
        loadEventsFromFirestore()
        setupMapLegend() // Setup the map legend

        // --- Click Listeners ---
        binding.searchCard.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        // Click listener for 'profileButton' REMOVED

        binding.filterButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.fabMyLocation.setOnClickListener {
            checkAndRequestLocationPermission()
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
            fetchCurrentLocation()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "fetchCurrentLocation called without permissions.")
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userGeoPoint = GeoPoint(location.latitude, location.longitude)
                    animateToLocation(userGeoPoint)
                    Toast.makeText(this, "Centering on your location", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Could not get current location. Is GPS on?", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to get location", e)
                Toast.makeText(this, "Error getting location", Toast.LENGTH_SHORT).show()
            }
    }

    private fun animateToLocation(geoPoint: GeoPoint) {
        val mapController = binding.mapView.controller
        mapController.setZoom(16.0) // Zoom in closer
        mapController.animateTo(geoPoint)
    }

    private fun setupMap() {
        val mapController = binding.mapView.controller
        mapController.setZoom(12.5)
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

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}