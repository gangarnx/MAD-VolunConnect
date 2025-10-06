package com.example.voluntra_mad_project

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // THIS LINE WAS MISSING
import com.example.voluntra_mad_project.databinding.ItemEventCardBinding
import com.example.voluntra_mad_project.models.Event

class EventsAdapter(private var events: List<Event>) : RecyclerView.Adapter<EventsAdapter.EventViewHolder>() {

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

            Glide.with(holder.itemView.context)
                .load(event.imageUrl)
                .centerCrop()
                .into(eventImage)
        }
    }

    fun submitList(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }
}