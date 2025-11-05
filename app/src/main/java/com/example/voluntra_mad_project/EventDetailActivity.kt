package com.example.voluntra_mad_project

import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract // Import CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.voluntra_mad_project.databinding.ActivityEventDetailBinding
import com.example.voluntra_mad_project.models.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore // Import firestore extension
import com.google.firebase.ktx.Firebase // Import Firebase
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar // Import Calendar
import java.util.Locale

class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore // Added db instance
    private var currentEvent: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val eventId = intent.getStringExtra("EVENT_ID")

        if (eventId == null) {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchEventDetails(eventId)

        binding.buttonGetDirections.setOnClickListener {
            openMapForDirections()
        }

        binding.buttonAddToCalendar.setOnClickListener {
            addEventToCalendar()
        }

        binding.buttonRegisterEvent.setOnClickListener {
            if (auth.currentUser == null) {
                Toast.makeText(this, "Please log in to register.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentEvent != null && currentEvent!!.id != null) {
                val intent = Intent(this, VolunteerRegistrationActivity::class.java)
                intent.putExtra("EVENT_ID", currentEvent!!.id)
                intent.putExtra("EVENT_TITLE", currentEvent!!.title)
                intent.putExtra("EVENT_DATE_MILLIS", currentEvent!!.eventDate?.time ?: -1L)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Event details not loaded yet.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchEventDetails(eventId: String) {
        firestore.collection("events").document(eventId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentEvent = document.toObject(Event::class.java)?.apply { id = document.id } // Assign document ID to model
                    currentEvent?.let { event ->
                        populateUI(event)
                        fetchRegistrationCount(eventId, event.spotsAvailable) // Fetch count after event loads
                    }
                } else {
                    Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("EventDetailActivity", "Error fetching event details", e)
                Toast.makeText(this, "Error fetching event details", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    // Function to fetch and display registration count
    private fun fetchRegistrationCount(eventId: String, totalSpots: Int?) {
        db.collection("registrations")
            .whereEqualTo("eventId", eventId)
            .count() // Use count aggregation
            .get(com.google.firebase.firestore.AggregateSource.SERVER) // Ensure SERVER source for accuracy
            .addOnSuccessListener { aggregateQuerySnapshot ->
                val count = aggregateQuerySnapshot.count
                // Update the TextView with the fetched count
                binding.textSpotsAvailableDetail.text = "Spots Available: $count / ${totalSpots ?: '?'}"
                Log.d("EventDetailActivity", "Registration count for $eventId: $count")
            }
            .addOnFailureListener { e ->
                Log.w("EventDetailActivity", "Error getting registration count for $eventId", e)
                // Keep the original placeholder text if count fails
                binding.textSpotsAvailableDetail.text = "Spots Available: ? / ${totalSpots ?: '?'}"
            }
    }

    private fun populateUI(event: Event) {
        binding.textEventTitleDetail.text = event.title
        binding.textOrganizerNameDetail.text = "By ${event.organizationName}"
        binding.textDescriptionDetail.text = event.description
        binding.textEventTypeDetail.text = event.eventType
        binding.textSkillsDetail.text = event.skillsNeeded?.joinToString(", ") ?: "None specified"
        binding.textWhatToBringDetail.text = event.whatToBring ?: "Nothing specific"
        // Set initial placeholder text for spots
        binding.textSpotsAvailableDetail.text = "Spots Available: ? / ${event.spotsAvailable ?: '?'}"

        event.eventDate?.let { date ->
            val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            binding.textDateTimeDetail.text = "${dateFormat.format(date)}・${timeFormat.format(date)}"
        }

        event.location?.let { geoPoint ->
            binding.textLocationDetail.text = getAddressFromGeoPoint(geoPoint.latitude, geoPoint.longitude)
        }
    }

    // Function to add event to device calendar
    private fun addEventToCalendar() {
        if (currentEvent == null || currentEvent?.eventDate == null) {
            Toast.makeText(this, "Event details not fully loaded.", Toast.LENGTH_SHORT).show()
            return
        }

        val event = currentEvent!!
        val startTimeMillis = event.eventDate!!.time

        // Estimate end time (e.g., 2 hours later - adjust duration as needed)
        val endTimeMillis = startTimeMillis + (2 * 60 * 60 * 1000)

        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
            .putExtra(CalendarContract.Events.TITLE, "Volunteering: ${event.title}") // Add context to title
            .putExtra(CalendarContract.Events.DESCRIPTION, event.description)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, getAddressFromGeoPoint(event.location!!.latitude, event.location!!.longitude))
            .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No calendar app found.", Toast.LENGTH_SHORT).show()
            Log.e("EventDetailActivity", "Failed to start calendar intent", e)
        }
    }

    // Function to open map for directions
    private fun openMapForDirections() {
        currentEvent?.location?.let { geoPoint ->
            // Create a Uri for Google Maps directions intent
            val gmmIntentUri = Uri.parse("google.navigation:q=${geoPoint.latitude},${geoPoint.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps") // Specify Google Maps app

            try {
                startActivity(mapIntent) // Try launching Google Maps
            } catch (ex: android.content.ActivityNotFoundException) {
                // Fallback if Google Maps is not installed
                try {
                    // Try opening coordinates in a web browser map
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${geoPoint.latitude},${geoPoint.longitude}"))
                    startActivity(browserIntent)
                } catch (ex2: android.content.ActivityNotFoundException) {
                    // If no browser or map app is found
                    Toast.makeText(this, "No map application found.", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: Toast.makeText(this, "Event location not available.", Toast.LENGTH_SHORT).show()
    }

    // Helper function to get address from coordinates
    private fun getAddressFromGeoPoint(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        var addressText = "Lat: $latitude, Lng: $longitude" // Default value
        try {
            // Attempt to get address (may fail if network is slow or coordinates are invalid)
            @Suppress("DEPRECATION") // Use new API if targeting SDK 33+
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                // Concatenate address lines for a more complete result
                addressText = address.getAddressLine(0) ?: "Unknown Location"
            }
        } catch (e: IOException) {
            // Handle errors like network issues or no geocoder available
            Log.e("EventDetailActivity", "Geocoder failed", e)
            // Keep the default coordinate string if geocoding fails
        } catch (e: IllegalArgumentException) {
            // Handle invalid latitude/longitude values
            Log.e("EventDetailActivity", "Invalid coordinates for Geocoder", e)
            // Keep the default coordinate string
        }
        return addressText // Ensure a String is always returned
    }
}