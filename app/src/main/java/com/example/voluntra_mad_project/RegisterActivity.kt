package com.example.voluntra_mad_project

import android.os.Bundle
import android.util.Log
import android.view.View // Import View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.voluntra_mad_project.databinding.ActivityRegisterBinding
import com.example.voluntra_mad_project.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // --- NEW: RadioGroup listener ---
        binding.radioGroupRole.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.radioOrganizer) {
                binding.textFieldOrganizationName.visibility = View.VISIBLE
            } else {
                binding.textFieldOrganizationName.visibility = View.GONE
            }
        }
        // --- END NEW ---

        binding.buttonRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name = binding.editTextName.text.toString().trim()
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString()

        val selectedRoleId = binding.radioGroupRole.checkedRadioButtonId
        val role = if (selectedRoleId == R.id.radioOrganizer) "organizer" else "volunteer"

        // Get organization name (will be empty if field is not visible)
        val organizationName = binding.editTextOrganizationName.text.toString().trim()

        // 1. Check for empty fields
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Check if organizer field is empty (if it's supposed to be filled)
        if (role == "organizer" && organizationName.isEmpty()) {
            Toast.makeText(this, "Please enter your organization's name.", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Domain validation
        val allowedDomains = setOf("@gmail.com", "@outlook.com", "@yahoo.com", "@hotmail.com", "@live.com")
        val atIndex = email.lastIndexOf('@')
        if (atIndex == -1) {
            Toast.makeText(this, "Invalid email format.", Toast.LENGTH_SHORT).show()
            return
        }
        val emailDomain = email.substring(atIndex).lowercase(Locale.getDefault())

        if (!allowedDomains.contains(emailDomain)) {
            Toast.makeText(this, "Email provider not supported. Please use Gmail, Outlook, or Yahoo.", Toast.LENGTH_LONG).show()
            return
        }

        // 4. Proceed with Firebase registration
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user!!

                    firebaseUser.sendEmailVerification()
                        .addOnSuccessListener { Log.d("RegisterActivity", "Verification email sent.") }
                        .addOnFailureListener { e -> Log.e("RegisterActivity", "Failed to send verification email.", e) }

                    // Create User object with the new organizationName field
                    val user = User(
                        uid = firebaseUser.uid,
                        name = name, // Personal name
                        email = email,
                        role = role,
                        organizationName = if (role == "organizer") organizationName else null // Only save if organizer
                    )

                    saveUserToFirestore(user)
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserToFirestore(user: User) {
        val db = Firebase.firestore
        db.collection("users")
            .document(user.uid)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration Successful! Please verify your email.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving user details: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}