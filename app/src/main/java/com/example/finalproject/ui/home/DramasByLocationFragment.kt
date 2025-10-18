package com.example.finalproject.ui.home  // Adjust this to match your actual file location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.example.finalproject.DatabaseHelper
import com.example.finalproject.adapter.TourAdapter
import com.example.finalproject.databinding.FragmentDramasByLocationBinding
import com.example.finalproject.util.PreferencesManager
import com.example.finalproject.model.Tour

class DramasByLocationFragment : Fragment() {

    private var _binding: FragmentDramasByLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var tourAdapter: TourAdapter

    private val args: DramasByLocationFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDramasByLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initDependencies()
        setupRecyclerView()
        loadDramasByLocation()
        setupClickListeners()
    }

    private fun initDependencies() {
        dbHelper = DatabaseHelper(requireContext())
        preferencesManager = PreferencesManager(requireContext())
    }

    private fun setupRecyclerView() {
        tourAdapter = TourAdapter(
            onTourClick = { tour ->
                openTourDetails(tour.dramaId)
            },
            onFavoriteClick = { tour, isFavorite ->
                handleFavoriteClick(tour, isFavorite)
            }
        )

        binding.rvDramas.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = tourAdapter
        }

        // tourAdapter.setFavorites(preferencesManager.getFavoriteTours())
    }

    private fun loadDramasByLocation() {
        val locationId = args.locationId
        val locationName = args.locationName

        binding.tvLocationName.text = locationName

        // If locationName is "Unknown Location", try to get it from database
        val actualLocationName = if (locationName == "Unknown Location") {
            dbHelper.getLocationById(locationId)?.name ?: locationName
        } else {
            locationName
        }
        binding.tvLocationName.text = actualLocationName

        // Load dramas for this location
        val dramas = dbHelper.getDramasByLocation(locationId)
        binding.tvDramaCount.text = "${dramas.size} dramas filmed here"
        tourAdapter.submitList(dramas)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun openTourDetails(tourId: String) {
        val action = DramasByLocationFragmentDirections.actionDramasByLocationFragmentToTourDetailsFragment(tourId)
        findNavController().navigate(action)
    }

    private fun handleFavoriteClick(tour: Tour, isFavorite: Boolean) {
        if (isFavorite) {
            preferencesManager.addFavoriteTour(tour.dramaId)
        } else {
            preferencesManager.removeFavoriteTour(tour.dramaId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}