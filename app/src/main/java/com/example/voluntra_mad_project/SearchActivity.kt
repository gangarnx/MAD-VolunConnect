package com.example.voluntra_mad_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.voluntra_mad_project.databinding.ActivitySearchBinding
import com.example.voluntra_mad_project.models.Event
// Removed Filter import as it's not needed for this approach
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.Locale // Import Locale

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var searchAdapter: EventsAdapter
    private val db = Firebase.firestore
    private var searchJob: Job? = null

    private val TAG = "SearchActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarSearch)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupRecyclerView()
        setupSearchInput()
    }

    private fun setupRecyclerView() {
        searchAdapter = EventsAdapter(emptyList()) { eventId ->
            val intent = Intent(this, EventDetailActivity::class.java)
            intent.putExtra("EVENT_ID", eventId)
            startActivity(intent)
        }
        binding.recyclerViewSearchResults.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSearchResults.adapter = searchAdapter
    }

    private fun setupSearchInput() {
        binding.editTextSearchQuery.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(textView.text.toString().trim())
                // Optionally hide keyboard here
                true
            } else {
                false
            }
        }

        binding.editTextSearchQuery.addTextChangedListener { editable ->
            searchJob?.cancel()
            searchJob = CoroutineScope(Dispatchers.Main).launch {
                delay(500) // Debounce delay
                performSearch(editable.toString().trim())
            }
        }
    }

    private fun performSearch(rawQuery: String) {
        searchJob?.cancel()

        if (rawQuery.isEmpty()) {
            searchAdapter.submitList(emptyList())
            return
        }

        // Convert query to lowercase for case-insensitive search
        val query = rawQuery.lowercase(Locale.getDefault())
        Log.d(TAG, "Performing search for (lowercase): $query")

        searchJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Firestore prefix match range end character
                val queryEnd = query + '\uf8ff'

                // Query lowercase fields
                val titleQuery = db.collection("events")
                    .whereGreaterThanOrEqualTo("title_lowercase", query)
                    .whereLessThan("title_lowercase", queryEnd)
                    .get()
                    .await()

                val descQuery = db.collection("events")
                    .whereGreaterThanOrEqualTo("description_lowercase", query)
                    .whereLessThan("description_lowercase", queryEnd)
                    .get()
                    .await()

                val orgQuery = db.collection("events")
                    .whereGreaterThanOrEqualTo("organizationName_lowercase", query)
                    .whereLessThan("organizationName_lowercase", queryEnd)
                    .get()
                    .await()

                // Combine results using a Set to handle duplicates based on Event's data class equals()
                val resultsSet = mutableSetOf<Event>()
                resultsSet.addAll(titleQuery.toObjects(Event::class.java))
                resultsSet.addAll(descQuery.toObjects(Event::class.java))
                resultsSet.addAll(orgQuery.toObjects(Event::class.java))

                Log.d(TAG, "Search found ${resultsSet.size} unique results.")

                // Update UI on the main thread
                withContext(Dispatchers.Main) {
                    // Sort results if needed, e.g., by date
                    val sortedResults = resultsSet.sortedByDescending { it.createdAt }
                    searchAdapter.submitList(sortedResults)
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    Log.d(TAG, "Search job cancelled")
                } else {
                    Log.e(TAG, "Error performing search", e)
                    withContext(Dispatchers.Main) {
                        // Check for index errors specifically
                        if (e.message?.contains("index") == true) {
                            Toast.makeText(this@SearchActivity, "Search setup needed. Please check logs.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@SearchActivity, "Search failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
    }
}