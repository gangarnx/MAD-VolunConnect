package com.example.voluntra_mad_project

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.voluntra_mad_project.databinding.ItemRegistrationBinding

// ** 1. Update data class **
data class RegistrationInfo(
    val volunteerName: String? = null,
    val volunteerEmail: String? = null,
    val volunteerPhone: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null
)

class RegistrationAdapter(private var registrations: List<RegistrationInfo>) :
    RecyclerView.Adapter<RegistrationAdapter.RegistrationViewHolder>() {

    inner class RegistrationViewHolder(val binding: ItemRegistrationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegistrationViewHolder {
        val binding = ItemRegistrationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RegistrationViewHolder(binding)
    }

    override fun getItemCount(): Int = registrations.size

    override fun onBindViewHolder(holder: RegistrationViewHolder, position: Int) {
        val registration = registrations[position]
        holder.binding.textVolunteerName.text = registration.volunteerName ?: "N/A"
        holder.binding.textVolunteerEmail.text = registration.volunteerEmail ?: "N/A"

        // Show phone number only if it's available
        if (!registration.volunteerPhone.isNullOrEmpty()) {
            holder.binding.textVolunteerPhone.text = "Phone: ${registration.volunteerPhone}"
            holder.binding.textVolunteerPhone.visibility = View.VISIBLE
        } else {
            holder.binding.textVolunteerPhone.visibility = View.GONE
        }

        // ** 2. Add logic for emergency contacts **
        if (!registration.emergencyContactName.isNullOrEmpty() && !registration.emergencyContactPhone.isNullOrEmpty()) {
            holder.binding.textEmergencyHeader.visibility = View.VISIBLE
            holder.binding.textEmergencyContact.text = "${registration.emergencyContactName} (${registration.emergencyContactPhone})"
            holder.binding.textEmergencyContact.visibility = View.VISIBLE
        } else {
            holder.binding.textEmergencyHeader.visibility = View.GONE
            holder.binding.textEmergencyContact.visibility = View.GONE
        }
    }

    fun submitList(newRegistrations: List<RegistrationInfo>) {
        registrations = newRegistrations
        notifyDataSetChanged()
    }
}