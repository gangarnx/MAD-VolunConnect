package com.example.voluntra_mad_project

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.voluntra_mad_project.databinding.ActivityRegisterBinding
import com.example.voluntra_mad_project.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.buttonRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val name = binding.editTextName.text.toString()
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString()

        val selectedRoleId = binding.radioGroupRole.checkedRadioButtonId
        val role = if (selectedRoleId == R.id.radioOrganizer) "organizer" else "volunteer"

        if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
            // 1. Create the user in Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = task.result?.user!!
                        // 2. Create a User object with the user's details
                        val user = User(
                            uid = firebaseUser.uid,
                            name = name,
                            email = email,
                            role = role
                        )
                        // 3. Save the User object to the Firestore database
                        saveUserToFirestore(user)
                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserToFirestore(user: User) {
        val db = Firebase.firestore
        // Save the user data to a "users" collection, using the UID as the document ID
        db.collection("users")
            .document(user.uid)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                // Finish this activity and go back to the login screen
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving user details: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}