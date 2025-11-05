package com.example.voluntra_mad_project.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Event(
    @DocumentId
    var id: String? = null,

    var title: String? = null,
    var description: String? = null,
    var organizationName: String? = null,
    var organizerId: String? = null,
    var location: GeoPoint? = null,
    // Removed imageUrl
    var category: String? = null,
    var eventDate: Date? = null,
    var eventType: String? = null,
    var skillsNeeded: List<String>? = null,
    var whatToBring: String? = null,
    var spotsAvailable: Int? = null,

    // Add lowercase fields for searching
    var title_lowercase: String? = null,
    var description_lowercase: String? = null,
    var organizationName_lowercase: String? = null,

    @ServerTimestamp
    var createdAt: Date? = null
) {
    // Add a secondary constructor or modify the primary if needed,
    // or populate lowercase fields before saving.
    // Easiest is to populate them just before saving in CreateEventActivity.
}