package com.example.finalproject.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.finalproject.DatabaseHelper
import com.example.finalproject.adapter.TourAdapter
import com.example.finalproject.databinding.FragmentPersonalisedToursBinding
import com.example.finalproject.model.Tour
import com.example.finalproject.util.PreferencesManager
import com.example.finalproject.util.SmartGenreExpander

class PersonalisedToursFragment : Fragment() {

    private var _binding: FragmentPersonalisedToursBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var tourAdapter: TourAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPersonalisedToursBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        preferencesManager = PreferencesManager(requireContext())

        setupToolbar()
        setupRecyclerView()
        loadPersonalisedTours()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Your Tours"
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        tourAdapter = TourAdapter(
            onTourClick = { tour ->
                openTourDetails(tour.dramaId)
            },
            onFavoriteClick = { tour, isFavorite ->
                if (isFavorite) {
                    preferencesManager.addFavoriteTour(tour.dramaId)
                } else {
                    preferencesManager.removeFavoriteTour(tour.dramaId)
                }
            }
        )

        binding.rvPersonalisedTours.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvPersonalisedTours.adapter = tourAdapter

        val initialFavorites = preferencesManager.getFavoriteTours()
        tourAdapter.setFavorites(initialFavorites)
    }

    private fun loadPersonalisedTours() {
        val userGenres = preferencesManager.getSelectedGenres()

        val personalisedTours = if (userGenres.isNotEmpty()) {
            // 🎯 AI ENHANCEMENT: Use smart genre expansion
            val smartGenres = SmartGenreExpander().expandUserPreferences(userGenres)

            dbHelper.getToursByGenres(smartGenres.toList())
        } else {
            dbHelper.getPopularTours()
        }

        tourAdapter.submitList(personalisedTours)

        // Update UI to show AI is working
        if (userGenres.isNotEmpty()) {
            val smartGenres = SmartGenreExpander().expandUserPreferences(userGenres)
            val extraCount = smartGenres.size - userGenres.size

            binding.tvTitle.text = "Personalised For You"
            if (extraCount > 0) {
                binding.tvSubtitle.text = "Based on your preferences + $extraCount related genres"
            } else {
                binding.tvSubtitle.text = "Based on your genre preferences"
            }
        } else {
            binding.tvTitle.text = "Popular Tours"
            binding.tvSubtitle.text = "Discover trending dramas"
        }

        // Show empty state if no tours
        if (personalisedTours.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvPersonalisedTours.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvPersonalisedTours.visibility = View.VISIBLE
        }
    }

    private fun openTourDetails(tourId: String) {
        // Fix: Use the correct action name from your navigation graph
        val action = PersonalisedToursFragmentDirections.actionPersonalisedToursToTourDetails(tourId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}