package com.example.finalproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalproject.adapter.DramaDropdownAdapter
import com.example.finalproject.adapter.GridAdapter
import com.example.finalproject.adapter.PosterAdapter
import com.example.finalproject.databinding.FragmentExploreBinding
import com.example.finalproject.model.Drama
import com.google.android.material.tabs.TabLayout

class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var allDramas: List<Drama>
    private var currentDramas: List<Drama> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())

        // Load all dramas from database instead of CSV
        allDramas = dbHelper.getAllDramas()

        setupCarousel()
        setupCategories()
        setupSearch()
    }

    private fun setupCarousel() {
        // Get popular dramas for carousel (first 4 from popular tours)
        val popularTours = dbHelper.getPopularTours()
        val carouselDramas = allDramas.filter { drama ->
            popularTours.take(4).any { it.dramaId == drama.dramaId }
        }.take(4)

        binding.rvPosters.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvPosters.adapter = PosterAdapter(requireContext(), carouselDramas) { drama ->
            openDramaDetails(drama)
        }
    }

    private fun setupCategories() {
        // Get dramas by categories from database
        val popularDramas = allDramas.take(12) // Show first 12 as popular
        val dramaDramas = dbHelper.getDramasByGenres(setOf("Drama")).take(12)
        val actionDramas = dbHelper.getDramasByGenres(setOf("Action")).take(12)
        val romanceDramas = dbHelper.getDramasByGenres(setOf("Romance")).take(12)
        val comedyDramas = dbHelper.getDramasByGenres(setOf("Comedy")).take(12)

        val categoryMap = mapOf(
            "Popular" to popularDramas,
            "Drama" to dramaDramas,
            "Action" to actionDramas,
            "Romance" to romanceDramas,
            "Comedy" to comedyDramas
        )

        // Default Grid setup
        currentDramas = popularDramas
        binding.rvGrid.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvGrid.adapter = GridAdapter(requireContext(), currentDramas) { drama ->
            openDramaDetails(drama)
        }

        // Setup tabs
        val categories = listOf("Popular", "Drama", "Action", "Romance", "Comedy")
        for (cat in categories) {
            binding.tabCategories.addTab(binding.tabCategories.newTab().setText(cat))
        }

        binding.tabCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val selectedDramas = categoryMap[tab.text] ?: emptyList()
                currentDramas = selectedDramas
                binding.rvGrid.adapter = GridAdapter(requireContext(), currentDramas) { drama ->
                    openDramaDetails(drama)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupSearch() {
        val searchBox = binding.searchBox as AutoCompleteTextView
        val dropdownAdapter = DramaDropdownAdapter(requireContext(), allDramas)
        searchBox.setAdapter(dropdownAdapter)

        // Show dropdown when typing
        searchBox.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && searchBox.text.isNotEmpty()) {
                searchBox.showDropDown()
            }
        }

        searchBox.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    searchBox.showDropDown()
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // When user picks from dropdown
        searchBox.setOnItemClickListener { _, _, position, _ ->
            val drama = dropdownAdapter.getItem(position)
            if (drama != null) {
                openDramaDetails(drama)
                searchBox.text.clear()
            }
        }

        // When user presses Enter/Search
        searchBox.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = searchBox.text.toString().trim()
                if (query.isNotEmpty()) {
                    val drama = allDramas.find {
                        it.titleEn.contains(query, ignoreCase = true) ||
                                it.titleTh.contains(query, ignoreCase = true)
                    }
                    if (drama != null) {
                        openDramaDetails(drama)
                        searchBox.text.clear()
                    }
                }
                true
            } else {
                false
            }
        }
    }

    private fun openDramaDetails(drama: Drama) {
        // Navigate to TourDetailsFragment using the drama ID
        val action = ExploreFragmentDirections.actionExploreToTourDetails(drama.dramaId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}