package com.example.finalproject.ui

import android.annotation.SuppressLint
import androidx.navigation.fragment.findNavController
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.finalproject.DatabaseHelper
import com.example.finalproject.R
import com.example.finalproject.databinding.FragmentProfileMainBinding
import com.example.finalproject.util.FavoriteManager
import com.example.finalproject.util.PreferencesManager
import com.example.finalproject.util.UserDatabaseHelper

// Suppress string/img warnings
@SuppressLint("SetTextI18n", "DiscouragedApi")
class ProfileMainFragment : Fragment() {

    private var _binding: FragmentProfileMainBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var userDatabaseHelper: UserDatabaseHelper
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    // ============================== CONTENT ==============================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialise database helpers
        dbHelper = DatabaseHelper(requireContext())
        userDatabaseHelper = UserDatabaseHelper(requireContext())
        preferencesManager = PreferencesManager(requireContext())

        FavoriteManager.init(requireContext()) // To update favourites
        loadUserUI(view) // To update data
        updateExploredProgress() // To update progress bar

        // Listen for updates from ProfileEditFragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("userUpdated")
            ?.observe(viewLifecycleOwner) { updated ->
                if (updated) {
                    loadUserUI(view)
                    findNavController().currentBackStackEntry?.savedStateHandle?.set("userUpdated", false)
                }
            }

        // Listen for interest updates from ProfileInterestsFragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<List<String>>("interestsUpdated")
            ?.observe(viewLifecycleOwner) { updatedInterests ->
                if (updatedInterests != null) {
                    loadUserUI(view)
                    findNavController().currentBackStackEntry?.savedStateHandle?.set("interestsUpdated", null)
                }
            }

        // Listen for UpdateProfileImage from ProfileEditFragment
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>("userUpdated")
            ?.observe(viewLifecycleOwner) { updated ->
                if (updated) {
                    loadUserUI(requireView())
                    findNavController().currentBackStackEntry?.savedStateHandle?.set("userUpdated", false)
                }
            }


        // =========== Buttons ===========
        binding.btnEdit.setOnClickListener { // To ProfileEditFragment
            findNavController().navigate(R.id.action_profile_to_edit)
        }

        binding.btnThreads.setOnClickListener { // To ProfileFavouritesFragment (Toggle 'Threads')
            val bundle = bundleOf("toggle" to "Threads")
            findNavController().navigate(R.id.action_profile_to_favourites, bundle)
        }

        binding.btnDramas.setOnClickListener { // To ProfileFavouritesFragment (Toggle 'Dramas')
            val bundle = bundleOf("toggle" to "Dramas")
            findNavController().navigate(R.id.action_profile_to_favourites, bundle)
        }

        binding.btnSettings.setOnClickListener { // To ProfileSettingsFragment
            findNavController().navigate(R.id.action_profile_to_settings)
        }

        binding.btnTips.setOnClickListener { // To ProfileTipsFragment
            findNavController().navigate(R.id.action_profile_to_tips)
        }

        binding.btnAbout.setOnClickListener { // To ProfileAboutFragment
            findNavController().navigate(R.id.action_profile_to_about)
        }

    }

    // =========== Functions ===========
    // fun loadUserUI: Update user data everytime ProfileMainFragment loads
    // fun loadGenres: Load list of genres data from database
    // fun findInterests: Update icons and texts based on the user's interests
    // fun updateExploredProgress: Update the progress bar
    private fun loadUserUI(rootView: View) {
        val userEmail = preferencesManager.getLoggedInUserEmail()

        if (userEmail.isNotEmpty()) {
            // Load user data from database
            val user = userDatabaseHelper.getUserByEmail(userEmail)

            if (user != null) {
                // =========== Update data ===========
                // Profile pic (Set to default pic if null)
                val img = binding.imgProfile
                val defaultImg = R.drawable.profile_pic_default
                val imgUri = user.profileImageUri

                if (!imgUri.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(imgUri)
                        .placeholder(R.drawable.profile_pic_default)
                        .error(R.drawable.profile_pic_default)
                        .circleCrop()
                        .into(img)
                } else {
                    img.setImageResource(defaultImg)
                }

                // Name
                val name = binding.textName
                name.text = "Hello, ${user.firstName} ${user.lastName}"

                // Email
                val email = binding.textEmail
                email.text = user.email

                // Progress bar
                findNavController().currentBackStackEntry?.savedStateHandle
                    ?.getLiveData<Boolean>("threadsUpdated")
                    ?.observe(viewLifecycleOwner) { updated ->
                        if (updated) {
                            updateExploredProgress()
                            findNavController().currentBackStackEntry?.savedStateHandle?.set("threadsUpdated", false)
                        }
                    }

                // Your favourites (Threads) - **using favorite threads**
                val userThreads = FavoriteManager.getThreadFavorites()
                val threadsGrouped = userThreads.groupBy { it.titleEn }

                val threadCount = threadsGrouped.size
                val textThreads = binding.textThreads
                when (threadCount) {
                    0 -> textThreads.text = "You have no threads saved"
                    1 -> textThreads.text = "You have 1 thread saved"
                    else -> textThreads.text = "You have $threadCount threads saved"
                }

                // Your favourites (Dramas) - **using favorite tours and dramas**
                val userDramas = FavoriteManager.getDramaFavorites()
                val textDramas = binding.textDramas
                when (userDramas.size) {
                    0 -> textDramas.text = "You have no dramas saved"
                    1 -> textDramas.text = "You have 1 drama saved"
                    else -> textDramas.text = "You have ${userDramas.size} dramas saved"
                }

                // Your interests - load from database
                val userInterests = preferencesManager.getSelectedGenres().toList()
                val genres = loadGenresFromDatabase()
                findInterests(rootView, genres, userInterests) // Call function to update icons and texts
            }
        } else {
            // Handle case where no user is logged in
            binding.textName.text = "Hello, Guest"
            binding.textEmail.text = "Please log in"
            binding.imgProfile.setImageResource(R.drawable.profile_pic_default)
        }
    }

    private fun loadGenresFromDatabase(): List<Genre> {
        val databaseGenres = dbHelper.getAllGenres()
        val genres = mutableListOf<Genre>()

        // Convert database genre strings to Genre objects with IDs
        databaseGenres.forEachIndexed { index, genreName ->
            val genre = Genre(
                id = (index + 1).toString(), // Generate ID based on position
                name = genreName
            )
            genres.add(genre)
        }

        return genres
    }

    private fun findInterests(view: View, genres: List<Genre>, userInterestNames: List<String>) {
        val likedGenres = genres.filter { it.name in userInterestNames }.take(4) // Find which genre saved in userInterestNames

        // Clear previous interests first
        for (i in 1..4) {
            val imgInterests = resources.getIdentifier("radMainIcon$i", "id", requireContext().packageName)
            val textInterests = resources.getIdentifier("radMainText$i", "id", requireContext().packageName)

            val img = view.findViewById<ImageView>(imgInterests)
            val text = view.findViewById<TextView>(textInterests)

            if (imgInterests != 0 && textInterests != 0) {
                img.setImageResource(0) // Clear image
                text.text = "" // Clear text
            }
        }

        for (i in likedGenres.indices) { // Loop through each genre saved, maximum of 4 interests
            val genre = likedGenres[i]

            // Find the components in the .xml (e.g., ranMainIcon1, radMainText1)
            val imgInterests = resources.getIdentifier("radMainIcon${i + 1}", "id", requireContext().packageName)
            val textInterests = resources.getIdentifier("radMainText${i + 1}", "id", requireContext().packageName)

            if (imgInterests != 0 && textInterests != 0) {
                val img = view.findViewById<ImageView>(imgInterests)
                val text = view.findViewById<TextView>(textInterests)

                // Load drawables with the correct name (e.g., Romance -> profile_rad2_romance)
                val drawableName = "profile_rad2_" + genre.name.lowercase().replace(" ", "_")
                val imgId = resources.getIdentifier(drawableName, "drawable", requireContext().packageName)

                if (imgId != 0) {
                    img.setImageResource(imgId)
                }

                // Change text
                text.text = genre.name
            }
        }
    }

    private fun updateExploredProgress() {
        val favoriteThreads = FavoriteManager.getThreadFavorites()
        val threadsGrouped = favoriteThreads.groupBy { it.titleEn }

        val exploredCount = threadsGrouped.count { (title, _) ->
            val drama = dbHelper.getAvailableTours().find { it.titleEn == title }
            val status = drama?.dramaId?.let { FavoriteManager.getExploredStatus(it) }
            status?.explored == true
        }

        val totalThreads = threadsGrouped.size
        val exploredPercent = if (totalThreads > 0) {
            ((exploredCount.toFloat() / totalThreads.toFloat()) * 100).toInt()
        } else {
            0
        }

        binding.barExplored.max = 100
        binding.barExplored.progress = exploredPercent
        binding.textExplored.text = "$exploredPercent%"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class Genre(
        val id: String,
        val name: String
    )
}