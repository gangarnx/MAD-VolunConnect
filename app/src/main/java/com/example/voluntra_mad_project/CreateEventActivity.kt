package com.example.voluntra_mad_project

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.voluntra_mad_project.databinding.ActivityCreateEventBinding
import com.example.voluntra_mad_project.models.Event
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

class CreateEventActivity : AppCompatActivity() {

    // --- THIS IS THE CORRECTED LINE ---
    private lateinit var binding: ActivityCreateEventBinding
    // --- END CORRECTION ---

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var eventLocation: GeoPoint? = null
    private var selectedDate: Calendar = Calendar.getInstance()

    // Location picker launcher
    private val pickLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            val address = data?.getStringExtra("address")

            eventLocation = GeoPoint(latitude, longitude)
            binding.editTextLocation.setText(address ?: "Lat/Lng: $latitude, $longitude")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupCategoryDropdown()
        setupClickListeners()
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf("Environment", "Education", "Community", "Health", "Animals")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.autoCompleteCategory.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.editTextDate.setOnClickListener { showDatePicker() }
        binding.editTextTime.setOnClickListener { showTimePicker() }
        binding.editTextLocation.setOnClickListener {
            pickLocationLauncher.launch(Intent(this, PickLocationActivity::class.java))
        }
        binding.buttonCreateEvent.setOnClickListener { validateAndCreateEvent() }
    }

    private fun validateAndCreateEvent() {
        val title = binding.editTextEventTitle.text.toString().trim()
        val category = binding.autoCompleteCategory.text.toString().trim()
        val dateStr = binding.editTextDate.text.toString().trim()
        val timeStr = binding.editTextTime.text.toString().trim()
        val description = binding.editTextEventDescription.text.toString().trim()
        val eventType = binding.editTextEventType.text.toString().trim()
        val spotsAvailableStr = binding.editTextSpotsAvailable.text.toString().trim()
        val skillsNeededStr = binding.editTextSkillsNeeded.text.toString().trim()
        val whatToBringStr = binding.editTextWhatToBring.text.toString().trim() // Get new field
        val organizerId = auth.currentUser?.uid

        if (title.isEmpty() || category.isEmpty() || dateStr.isEmpty() || timeStr.isEmpty() ||
            eventLocation == null || description.isEmpty() || eventType.isEmpty() ||
            spotsAvailableStr.isEmpty() || organizerId == null) {
            Toast.makeText(this, "Please fill all required fields and pick a location.", Toast.LENGTH_LONG).show()
            return
        }

        val spotsAvailable = try {
            spotsAvailableStr.toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Please enter a valid number for spots.", Toast.LENGTH_SHORT).show()
            return
        }

        if (spotsAvailable <= 0) {
            Toast.makeText(this, "Spots available must be greater than zero.", Toast.LENGTH_SHORT).show()
            return
        }

        val eventDateTime = selectedDate.time
        val eventTimestamp = Timestamp(eventDateTime)
        val skillsList = convertSkillsStringToList(skillsNeededStr)

        saveEventToFirestore(
            title, category, eventTimestamp, eventLocation!!, description,
            eventType, organizerId, spotsAvailable, skillsList, whatToBringStr
        )
    }

    private fun saveEventToFirestore(
        title: String, category: String, eventTimestamp: Timestamp, location: GeoPoint,
        description: String, eventType: String, organizerId: String, spotsAvailable: Int,
        skillsList: List<String>?, whatToBring: String
    ) {
        firestore.collection("users").document(organizerId).get()
            .addOnSuccessListener { userDocument ->
                val organizationName = userDocument?.getString("organizationName") ?: "Unknown Organizer"

                val newEvent = Event(
                    title = title,
                    category = category,
                    eventDate = eventTimestamp.toDate(),
                    location = location,
                    description = description,
                    eventType = eventType,
                    organizerId = organizerId,
                    organizationName = organizationName,
                    skillsNeeded = skillsList,
                    whatToBring = if (whatToBring.isBlank()) null else whatToBring, // Save as null if empty
                    spotsAvailable = spotsAvailable,
                    title_lowercase = title.lowercase(Locale.getDefault()),
                    description_lowercase = description.lowercase(Locale.getDefault()),
                    organizationName_lowercase = organizationName.lowercase(Locale.getDefault())
                )

                firestore.collection("events")
                    .add(newEvent)
                    .addOnSuccessListener { documentReference ->
                        Log.d("CreateEventActivity", "Event added with ID: ${documentReference.id}")
                        Toast.makeText(this, "Event created successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Log.e("CreateEventActivity", "Error adding event", e)
                        Toast.makeText(this, "Error creating event: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("CreateEventActivity", "Error fetching organizer name", e)
                Toast.makeText(this, "Error getting organizer details: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Helper Function to convert skills string
    private fun convertSkillsStringToList(skills: String): List<String>? {
        if (skills.isBlank()) {
            return null
        }
        return skills.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    // --- Date & Time Picker Logic ---
    private fun showDatePicker() {
        val currentCalendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            selectedDate.set(Calendar.YEAR, year)
            selectedDate.set(Calendar.MONTH, month)
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            binding.editTextDate.setText(displayFormat.format(selectedDate.time))
        },
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH),
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
            show()
        }
    }

    private fun showTimePicker() {
        val currentCalendar = Calendar.getInstance()
        TimePickerDialog(this, { _, hourOfDay, minute ->
            selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
            selectedDate.set(Calendar.MINUTE, minute)
            selectedDate.set(Calendar.SECOND, 0)
            val displayFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.editTextTime.setText(displayFormat.format(selectedDate.time))
        },
            currentCalendar.get(Calendar.HOUR_OF_DAY),
            currentCalendar.get(Calendar.MINUTE),
            true // 24-hour format
        ).show()
    }
}