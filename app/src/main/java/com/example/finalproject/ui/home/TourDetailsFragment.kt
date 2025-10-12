package com.example.finalproject.ui.home


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.finalproject.DatabaseHelper
import com.example.finalproject.R
import com.example.finalproject.adapter.LocationDetailAdapter
import com.example.finalproject.databinding.FragmentTourDetailsBinding


class TourDetailsFragment : Fragment() {


    private var _binding: FragmentTourDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var locationAdapter: LocationDetailAdapter


    private val args: TourDetailsFragmentArgs by navArgs()


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


        initDependencies()
        setupToolbar()
        setupRecyclerView()
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
        //{ location -> }


        binding.rvLocations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = locationAdapter
        }
    }


    private fun loadTourDetails() {
        val tourId = args.tourId


        // Load tour details
        val tours = dbHelper.getAvailableTours()
        val currentTour = tours.find { it.dramaId == tourId }


        currentTour?.let { tour ->
            binding.toolbar.title = tour.titleEn
            binding.tvTourTitle.text = tour.titleEn
            binding.tvTourDescription.text = tour.description ?: "No description available"
            binding.tvLocationCount.text =
                "${tour.locationCount} locations • ${tour.totalTravelTime} min travel"


            Glide.with(this)
                .load(tour.posterUrl)
                .placeholder(R.drawable.placeholder_bg) // Optional: show placeholder while loading
                .error(R.drawable.placeholder_bg)       // Optional: show placeholder if image fails to load
                .into(binding.ivPoster)


            // Load locations for this tour
            val locations = dbHelper.getTourLocations(tourId)
            locationAdapter.submitList(locations)
        } ?: run {
            // Handle case where tour is not found
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

