package com.example.voluntra_mad_project

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.voluntra_mad_project.databinding.ActivityVolunteerRegistrationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class VolunteerRegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVolunteerRegistrationBinding
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private var eventId: String? = null
    private var eventTitle: String? = null
    private var eventDate: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVolunteerRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        eventId = intent.getStringExtra("EVENT_ID")
        eventTitle = intent.getStringExtra("EVENT_TITLE")
        val eventDateMillis = intent.getLongExtra("EVENT_DATE_MILLIS", -1)
        if (eventDateMillis != -1L) {
            eventDate = Date(eventDateMillis)
        }

        if (eventId == null) {
            Toast.makeText(this, "Error: Event ID missing.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.textEventTitleRegistration.text = "Register for $eventTitle"
        eventDate?.let {
            val dateFormat = SimpleDateFormat("EEE, MMM d・h:mm a", Locale.getDefault())
            binding.textEventDateRegistration.text = dateFormat.format(it)
        }

        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { doc ->
                    binding.editTextVolunteerName.setText(doc.getString("name") ?: "")
                    binding.editTextVolunteerEmail.setText(currentUser.email ?: "")
                }
            binding.editTextVolunteerEmail.setText(currentUser.email ?: "")
        }

        binding.buttonConfirmRegistration.setOnClickListener {
            confirmRegistration()
        }
    }

    private fun confirmRegistration() {
        val volunteerName = binding.editTextVolunteerName.text.toString().trim()
        val volunteerEmail = binding.editTextVolunteerEmail.text.toString().trim()
        val volunteerPhone = binding.editTextVolunteerPhone.text.toString().trim()
        val volunteerUid = auth.currentUser?.uid

        val emergencyName = binding.editTextEmergencyName.text.toString().trim()
        val emergencyPhone = binding.editTextEmergencyPhone.text.toString().trim()
        val agreementChecked = binding.checkboxAgreement.isChecked

        // Validation
        if (volunteerName.isEmpty() || volunteerEmail.isEmpty()) {
            Toast.makeText(this, "Please enter your name and email.", Toast.LENGTH_SHORT).show()
            return
        }
        if (emergencyName.isEmpty() || emergencyPhone.isEmpty()) {
            Toast.makeText(this, "Please enter emergency contact details.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!agreementChecked) {
            Toast.makeText(this, "You must agree to the terms.", Toast.LENGTH_SHORT).show()
            return
        }
        if (volunteerUid == null) {
            Toast.makeText(this, "Error: You must be logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create registration data map (T-shirt size removed)
        val registrationData = hashMapOf(
            "eventId" to eventId,
            "eventTitle" to eventTitle,
            "volunteerUid" to volunteerUid,
            "volunteerName" to volunteerName,
            "volunteerEmail" to volunteerEmail,
            "volunteerPhone" to volunteerPhone,
            "emergencyContactName" to emergencyName,
            "emergencyContactPhone" to emergencyPhone,
            "agreedToTerms" to agreementChecked,
            "registrationTime" to FieldValue.serverTimestamp()
        )

        // Save to Firestore
        db.collection("registrations")
            .add(registrationData)
            .addOnSuccessListener {
                Log.d("Registration", "Volunteer registered successfully: ${it.id}")
                Toast.makeText(this, "Registration Confirmed!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("Registration", "Error saving registration", e)
                Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}