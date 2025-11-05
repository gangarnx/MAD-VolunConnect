package com.example.voluntra_mad_project

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
// import com.bumptech.glide.Glide // REMOVED
import com.example.voluntra_mad_project.databinding.ItemEventCardBinding
import com.example.voluntra_mad_project.models.Event
import java.text.SimpleDateFormat
import java.util.Locale

class EventsAdapter(
    private var events: List<Event>,
    private val onItemClicked: (String) -> Unit
) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

    inner class EventViewHolder(val binding: ItemEventCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun getItemCount(): Int = events.size

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.binding.apply {
            eventTitle.text = event.title
            eventOrganization.text = event.organizationName
            eventTag.text = event.category

            event.eventDate?.let {
                val dateFormat = SimpleDateFormat("EEE, MMM d・h:mm a", Locale.getDefault())
                eventDate.text = dateFormat.format(it)
            } ?: run {
                eventDate.text = "Date not set"
            }

            // Image loading REMOVED

            root.setOnClickListener {
                event.id?.let { id ->
                    onItemClicked(id)
                }
            }
        }
    }

    fun submitList(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }
}