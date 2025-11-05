package com.example.voluntra_mad_project.models

data class User(
    val uid: String = "",
    val name: String = "", // This will now be the organizer's personal name
    val email: String = "",
    val role: String = "",
    val organizationName: String? = null // NEW: Field for the org's name
)