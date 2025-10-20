package com.example.finalproject.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.finalproject.DatabaseHelper
import com.example.finalproject.R
import com.example.finalproject.adapter.LocationDetailAdapter
import com.example.finalproject.databinding.FragmentTourDetailsBinding
import com.example.finalproject.model.Drama
import com.example.finalproject.model.Tour
import com.example.finalproject.util.FavoriteManager

class TourDetailsFragment : Fragment() {

    private var _binding: FragmentTourDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var locationAdapter: LocationDetailAdapter

    private val args: TourDetailsFragmentArgs by navArgs()
    private var currentTour: Tour? = null
    private var isFavorite = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTourDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensure FavoriteManager is initialized with context
        FavoriteManager.init(requireContext())

        initDependencies()
        setupToolbar()
        setupRecyclerView()
        setupFavoriteButton()
        loadTourDetails()
    }

    private fun initDependencies() {
        dbHelper = DatabaseHelper(requireContext())
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        locationAdapter = LocationDetailAdapter()
        binding.rvLocations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = locationAdapter
        }
    }

    private fun setupFavoriteButton() {
        binding.btnFavoriteDrama.setOnClickListener {
            toggleFavorite()
        }
    }

    private fun createDramaFromTour(tour: Tour): Drama {
        return Drama(
            dramaId = tour.dramaId,
            titleEn = tour.titleEn,
            titleTh = tour.titleTh ?: "",
            releaseYear = 0,
            duration = "Unknown",
            summary = tour.description ?: "No description available",
            posterUrl = tour.posterUrl ?: "",
            bgUrl = ""
        )
    }

    private fun toggleFavorite() {
        currentTour?.let { tour ->
            val drama = createDramaFromTour(tour)

            if (isFavorite) {
                // Remove from favorites
                FavoriteManager.removeDramaFavorite(drama)
                isFavorite = false
                binding.btnFavoriteDrama.setImageResource(R.drawable.ic_favorite_border)
                Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show()
            } else {
                // Add to favorites
                FavoriteManager.addDramaFavorite(drama)
                isFavorite = true
                binding.btnFavoriteDrama.setImageResource(R.drawable.ic_favorite_filled)
                Toast.makeText(requireContext(), "Added to favorites!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkIfFavorite(tour: Tour) {
        val drama = createDramaFromTour(tour)
        isFavorite = FavoriteManager.isDramaFavorite(drama)

        // Set initial heart icon
        if (isFavorite) {
            binding.btnFavoriteDrama.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            binding.btnFavoriteDrama.setImageResource(R.drawable.ic_favorite_border)
        }
    }

    private fun loadTourDetails() {
        val tourId = args.tourId
        val tours = dbHelper.getAvailableTours()

        this.currentTour = tours.find { it.dramaId == tourId }

        currentTour?.let { tour ->
            checkIfFavorite(tour)

            binding.tvTourTitle.text = tour.titleEn
            binding.tvTourDescription.text = tour.description ?: "No description available"
            binding.tvLocationCount.text =
                "${tour.locationCount} locations • ${tour.totalTravelTime} min travel"

            Glide.with(this)
                .load(tour.bgUrl)
                .placeholder(R.drawable.placeholder_bg)
                .error(R.drawable.placeholder_bg)
                .into(binding.ivPoster)

            val locations = dbHelper.getTourLocations(tourId)
            locationAdapter.submitList(locations)
        } ?: run {
            binding.toolbar.title = "Tour Not Found"
            binding.tvTourTitle.text = "Tour Not Found"
            binding.tvTourDescription.text = "The requested tour could not be loaded."
            binding.tvLocationCount.text = "0 locations • 0 min travel"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
