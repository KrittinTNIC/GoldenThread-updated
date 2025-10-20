package com.example.finalproject.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalproject.DatabaseHelper
import com.example.finalproject.R
import com.example.finalproject.adapter.LocationAdapter
import com.example.finalproject.adapter.TourAdapter
import com.example.finalproject.databinding.FragmentHomeBinding
import com.example.finalproject.model.Tour
import com.example.finalproject.util.PreferencesManager
import com.example.finalproject.util.SmartGenreExpander
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var locationAdapter: LocationAdapter
    private lateinit var tourAdapter: TourAdapter
    private var loadingJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDependencies()
        setupRecyclerViews()
        setupSearch()
        setupClickListeners()
        loadData()
        showRandomFact()
    }

    private fun initDependencies() {
        dbHelper = DatabaseHelper(requireContext())
        preferencesManager = PreferencesManager(requireContext())
    }

    private fun setupRecyclerViews() {
        // Featured Locations (Horizontal)
        locationAdapter = LocationAdapter { location ->
            openThreadWithLocation(location.id)
        }
        binding.rvFeaturedLocations.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = locationAdapter
        }

        // Personalised Tours
        tourAdapter = TourAdapter(
            onTourClick = { tour ->
                openTourDetails(tour.dramaId)
            },
            onFavoriteClick = { tour, isFavorite ->
                handleFavoriteClick(tour, isFavorite)
            }
        )
        binding.rvPersonalisedTours.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = tourAdapter
        }
    }

    private fun showRandomFact() {
        animateLoadingDots()

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = "AIzaSyC6LW8mzZ6lpNWqt2tKLklNTnzASD9yRkk"
        )

        val prompt = """
            Write a one-sentence random fact about Thai dramas.
            Make it related to Thai movies or series — where they take place, or how popular they are.
            Get creative and vary the topic.
        """.trimIndent()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                loadingJob?.cancel()
                _binding?.tvFunFact?.text = response.text ?: "No fact available right now."
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error generating fun fact: ${e.message}")
                _binding?.tvFunFact?.text = "Couldn't load a fun fact."
            }
        }
    }

    private fun animateLoadingDots() {
        loadingJob?.cancel()

        loadingJob = viewLifecycleOwner.lifecycleScope.launch {
            val text = "Loading"
            while (true) {
                for (i in 0..3) {
                    _binding?.tvFunFact?.text = text + ".".repeat(i)
                    delay(500)
                    if (_binding == null) return@launch // stop if view is gone
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.tvTapForMore.setOnClickListener { showRandomFact() }
        binding.tvFeaturedLocationsHeader.setOnClickListener { openThreadFragment() }
        binding.tvPersonalisedHeader.setOnClickListener { openPersonalisedToursFragment() }
    }

    private fun setupSearch() {
        val dramas = dbHelper.getAllDramas()
        val dramaTitles = dramas.map { it.titleEn }.toTypedArray()

        val searchAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            dramaTitles
        )
        binding.etSearch.setAdapter(searchAdapter)

        binding.etSearch.setOnItemClickListener { parent, _, position, _ ->
            val selectedDramaTitle = parent.getItemAtPosition(position) as String
            val selectedDrama = dramas.find { it.titleEn == selectedDramaTitle }
            selectedDrama?.let { openTourDetails(it.dramaId) }
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else false
        }
    }

    private fun performSearch() {
        val query = binding.etSearch.text.toString().trim()
        if (query.isNotEmpty()) {
            Log.d("HomeFragment", "Search query: $query")
        }
    }

    private fun loadData() {
        val userGenres = preferencesManager.getSelectedGenres()

        val featuredLocations = dbHelper.getFeaturedLocations()
        locationAdapter.submitList(featuredLocations)

        val personalisedTours = if (userGenres.isNotEmpty()) {
            val smartGenres = SmartGenreExpander().expandUserPreferences(userGenres)
            val tours = dbHelper.getToursByGenres(smartGenres.toList())

            if (smartGenres.size > userGenres.size) {
                val extraGenres = smartGenres - userGenres
                Log.d("AI", "Expanded $userGenres to include related genres: $extraGenres")
            }

            tours
        } else {
            dbHelper.getPopularTours()
        }

        tourAdapter.submitList(personalisedTours)
    }

    private fun handleFavoriteClick(tour: Tour, isFavorite: Boolean) {
        if (isFavorite) {
            preferencesManager.addFavoriteTour(tour.dramaId)
        } else {
            preferencesManager.removeFavoriteTour(tour.dramaId)
        }
    }

    private fun openThreadFragment() {
        try {
            findNavController().navigate(R.id.nav_thread)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error navigating to thread: ${e.message}")
        }
    }

    private fun openThreadWithLocation(locationId: String) {
        try {
            val bundle = bundleOf("locationId" to locationId)
            findNavController().navigate(R.id.nav_thread, bundle)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error navigating to thread with location: ${e.message}")
        }
    }

    private fun openPersonalisedToursFragment() {
        val action = HomeFragmentDirections.actionHomeToPersonalisedTours()
        findNavController().navigate(action)
    }

    private fun openTourDetails(tourId: String) {
        val action = HomeFragmentDirections.actionHomeToTourDetails(tourId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingJob?.cancel()
        _binding = null
    }
}
