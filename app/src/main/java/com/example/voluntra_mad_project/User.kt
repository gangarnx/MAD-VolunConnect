package com.example.voluntra_mad_project.models

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val skills: List<String>? = null, // List of volunteer skills
    val interests: List<String>? = null // List of volunteer interests
)