package com.example.finalproject.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.finalproject.adapter.FavoriteThreadAdapter
import com.example.finalproject.databinding.FragmentProfileBinding
import com.example.finalproject.util.FavoriteManager

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var favoriteThreadAdapter: FavoriteThreadAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadFavoriteThreads()
    }

    override fun onResume() {
        super.onResume()
        // Reload favorites every time the fragment is resumed
        loadFavoriteThreads()
    }

    private fun setupRecyclerView() {
        // The adapter no longer needs an initial list
        favoriteThreadAdapter = FavoriteThreadAdapter()
        binding.rvFavoriteThreads.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = favoriteThreadAdapter
        }
    }

    private fun loadFavoriteThreads() {
        val favoriteThreads = FavoriteManager.getThreadFavorites()
        favoriteThreadAdapter.submitList(favoriteThreads)

        // Show a message if the list is empty
        if (favoriteThreads.isEmpty()) {
            binding.tvEmptyFavorites.visibility = View.VISIBLE
            binding.rvFavoriteThreads.visibility = View.GONE
        } else {
            binding.tvEmptyFavorites.visibility = View.GONE
            binding.rvFavoriteThreads.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
