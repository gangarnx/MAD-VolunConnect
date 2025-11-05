package com.example.voluntra_mad_project

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.voluntra_mad_project.databinding.ItemOrganizerEventCardBinding
import com.example.voluntra_mad_project.models.Event
import java.text.SimpleDateFormat
import java.util.Locale

// Updated adapter to accept a Map of eventId to registration count
class OrganizerEventsAdapter(
    private var events: List<Event>,
    private var registrationCounts: Map<String, Int>, // Add map for counts
    private val onViewRegistrationsClicked: (Event) -> Unit
) : RecyclerView.Adapter<OrganizerEventsAdapter.EventViewHolder>() {

    inner class EventViewHolder(val binding: ItemOrganizerEventCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemOrganizerEventCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun getItemCount(): Int = events.size

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.binding.apply {
            eventTitle.text = event.title
            event.eventDate?.let {
                val dateFormat = SimpleDateFormat("EEE, MMM d・h:mm a", Locale.getDefault())
                eventDate.text = dateFormat.format(it)
            } ?: run {
                eventDate.text = "Date not set"
            }

            // Get the count for this event from the map
            val count = registrationCounts[event.id] ?: 0 // Default to 0 if not found
            eventRegistrationCount.text = "Registrations: $count / ${event.spotsAvailable ?: '?'}"


            buttonViewRegistrations.setOnClickListener {
                onViewRegistrationsClicked(event)
            }
        }
    }

    // Update function to accept both events and counts
    fun submitLists(newEvents: List<Event>, newCounts: Map<String, Int>) {
        events = newEvents
        registrationCounts = newCounts
        notifyDataSetChanged()
    }
}