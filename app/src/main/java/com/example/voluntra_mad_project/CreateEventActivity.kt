package com.example.voluntra_mad_project

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.voluntra_mad_project.databinding.ActivityCreateEventBinding
import com.google.firebase.firestore.GeoPoint
import java.util.Calendar

class CreateEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateEventBinding
    private var imageUri: Uri? = null
    private var eventLocation: GeoPoint? = null

    // Launcher for picking an image from the gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            binding.imageEventBanner.setImageURI(it)
        }
    }

    // Launcher for picking a location from our new map activity
    private val pickLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val latitude = data?.getDoubleExtra("latitude", 0.0) ?: 0.0
            val longitude = data?.getDoubleExtra("longitude", 0.0) ?: 0.0
            val address = data?.getStringExtra("address")

            eventLocation = GeoPoint(latitude, longitude)
            binding.editTextLocation.setText(address)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCategoryDropdown()
        setupClickListeners()
    }

    private fun setupCategoryDropdown() {
        val categories = arrayOf("Environment", "Education", "Community", "Health", "Animals")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.autoCompleteCategory.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.buttonAddImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.editTextDate.setOnClickListener {
            showDatePicker()
        }

        binding.editTextTime.setOnClickListener {
            showTimePicker()
        }

        binding.editTextLocation.setOnClickListener {
            pickLocationLauncher.launch(Intent(this, PickLocationActivity::class.java))
        }

        binding.buttonCreateEvent.setOnClickListener {
            // TODO: Implement the final save logic
            // 1. Validate all fields are filled.
            // 2. Upload the `imageUri` to Firebase Storage.
            // 3. On success, get the download URL.
            // 4. Create an `Event` object with all data, including the image URL and `eventLocation` GeoPoint.
            // 5. Save the `Event` object to Firestore.
            Toast.makeText(this, "Creating event... (Placeholder)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            binding.editTextDate.setText("$dayOfMonth/${month + 1}/$year")
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        datePickerDialog.show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
            binding.editTextTime.setText(String.format("%02d:%02d", hourOfDay, minute))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
        timePickerDialog.show()
    }
}