package com.example.voluntra_mad_project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.voluntra_mad_project.databinding.ItemRegistrationBinding // Use the correct binding class

// Simple data class to hold registration info needed for display
data class RegistrationInfo(
    val volunteerName: String? = null,
    val volunteerEmail: String? = null,
    val volunteerPhone: String? = null
)

class RegistrationAdapter(private var registrations: List<RegistrationInfo>) :
    RecyclerView.Adapter<RegistrationAdapter.RegistrationViewHolder>() {

    // ViewHolder holds references to views in item_registration.xml
    inner class RegistrationViewHolder(val binding: ItemRegistrationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegistrationViewHolder {
        // Inflate the layout for each registration item
        val binding = ItemRegistrationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RegistrationViewHolder(binding)
    }

    override fun getItemCount(): Int = registrations.size // Return total number of registrations

    override fun onBindViewHolder(holder: RegistrationViewHolder, position: Int) {
        val registration = registrations[position] // Get data for this position
        holder.binding.textVolunteerName.text = registration.volunteerName ?: "N/A"
        holder.binding.textVolunteerEmail.text = registration.volunteerEmail ?: "N/A"

        // Show phone number only if it's available
        if (!registration.volunteerPhone.isNullOrEmpty()) {
            holder.binding.textVolunteerPhone.text = "Phone: ${registration.volunteerPhone}"
            holder.binding.textVolunteerPhone.visibility = View.VISIBLE
        } else {
            holder.binding.textVolunteerPhone.visibility = View.GONE
        }
    }

    // Function to update the list of registrations shown
    fun submitList(newRegistrations: List<RegistrationInfo>) {
        registrations = newRegistrations
        notifyDataSetChanged() // Refresh the RecyclerView
    }
}