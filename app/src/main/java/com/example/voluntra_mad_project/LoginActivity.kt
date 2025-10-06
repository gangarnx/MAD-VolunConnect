package com.example.voluntra_mad_project

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.voluntra_mad_project.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonLogin.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // TODO: Add Firebase Authentication login logic here.
                Toast.makeText(this, "Login Successful (Placeholder)", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
        }

        // The reference is now to the TextView with the ID "textSignUp"
        binding.textSignUp.setOnClickListener {
            // TODO: Navigate to a new SignUpActivity.
            Toast.makeText(this, "Sign Up Clicked", Toast.LENGTH_SHORT).show()
        }
    }
}