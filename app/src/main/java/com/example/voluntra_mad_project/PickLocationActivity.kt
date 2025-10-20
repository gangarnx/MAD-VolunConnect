package com.example.voluntra_mad_project

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.voluntra_mad_project.databinding.ActivityPickLocationBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.IOException
import java.util.Locale

class PickLocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPickLocationBinding
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPickLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // OSMDroid Configuration
        Configuration.getInstance().load(applicationContext, getPreferences(MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = applicationContext.packageName

        mapView = binding.mapViewPicker
        mapView.setMultiTouchControls(true)

        // Set initial map center to Mumbai
        val mapController = mapView.controller
        mapController.setZoom(12.5)
        val startPoint = GeoPoint(19.0760, 72.8777)
        mapController.setCenter(startPoint)

        binding.buttonConfirmLocation.setOnClickListener {
            // Get the coordinates from the center of the map
            val centerPoint = mapView.mapCenter
            val latitude = centerPoint.latitude
            val longitude = centerPoint.longitude
            val address = getAddressFromGeoPoint(latitude, longitude)

            // Create an intent to send the data back
            val resultIntent = Intent()
            resultIntent.putExtra("latitude", latitude)
            resultIntent.putExtra("longitude", longitude)
            resultIntent.putExtra("address", address)

            // Set the result and finish the activity
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun getAddressFromGeoPoint(latitude: Double, longitude: Double): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        var addressText = "Address not found"
        try {
            // This is "reverse geocoding" - getting an address from coordinates
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                // Build a simple address string
                addressText = address.getAddressLine(0) ?: "Unknown Location"
            }
        } catch (e: IOException) {
            // Handle case where geocoder service is not available
        }
        return addressText
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }
}