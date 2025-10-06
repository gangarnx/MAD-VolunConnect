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
    var location: GeoPoint? = null,
    var imageUrl: String? = null, // New field for the event image URL

    @ServerTimestamp
    var createdAt: Date? = null
)