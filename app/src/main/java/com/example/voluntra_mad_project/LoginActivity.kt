package com.example.voluntra_mad_project

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.voluntra_mad_project.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // On successful login, check the user's role
                        checkUserRoleAndRedirect()
                    } else {
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.textSignUp.setOnClickListener {
            // Open the new RegisterActivity
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun checkUserRoleAndRedirect() {
        val userId = auth.currentUser?.uid ?: return
        val db = Firebase.firestore

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    if (role == "organizer") {
                        // User is an organizer, go to organizer dashboard
                        startActivity(Intent(this, OrganizerDashboardActivity::class.java))
                    } else {
                        // User is a volunteer, go to main map activity
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    finish() // Close login activity
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to get user role.", Toast.LENGTH_SHORT).show()
            }
    }
}