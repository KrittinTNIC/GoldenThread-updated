package com.example.finalproject.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.example.finalproject.DatabaseHelper
import com.example.finalproject.R
import com.example.finalproject.databinding.FragmentProfileInterestsBinding
import com.example.finalproject.util.PreferencesManager

// Suppress string/img warnings
@SuppressLint("SetTextI18n", "DiscouragedApi")
class ProfileInterestsFragment : Fragment() {

    private var _binding: FragmentProfileInterestsBinding? = null
    private val binding get() = _binding!!

    private var isFirstTimeSetup = false
    private lateinit var userInterestIds: MutableList<String>
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var preferencesManager: PreferencesManager

    // Data class for Genre (since we're not using ProfileMainFragment.Genre anymore)
    data class Genre(val id: String, val name: String)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileInterestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize database and preferences manager
        databaseHelper = DatabaseHelper(requireContext())
        preferencesManager = PreferencesManager(requireContext())

        // Check if this is first-time setup
        isFirstTimeSetup = arguments?.getBoolean("isFirstTimeSetup", false) ?: false

        // Load user interests from database (PreferencesManager)
        userInterestIds = preferencesManager.getSelectedGenres().toMutableList()

        updateUIForFlow()

        // **Load genres from database instead of CSV**
        val genres = loadGenresFromDatabase()
        displayInterests(view, genres, userInterestIds)

        // =========== Buttons ===========
        binding.btnBack.setOnClickListener {
            if (isFirstTimeSetup) {
                // For first-time setup, don't allow going back without selection
                if (userInterestIds.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select at least 1 interest to continue", Toast.LENGTH_SHORT).show()
                } else {
                    saveAndNavigate()
                }
            } else {
                findNavController().navigateUp()
            }
        }
        binding.btnSubmit.setOnClickListener {
            saveAndNavigate()
        }
    }

    private fun updateUIForFlow() {
        if (isFirstTimeSetup) {
            val headerText = view?.findViewById<TextView>(R.id.textView)
            headerText?.text = "Select Your Preferences"

            // Change subtitle text
            val subtitle = view?.findViewById<TextView>(R.id.textView2)
            subtitle?.text = "Choose your favorite genres to personalize your experience"
        }
    }

    private fun saveAndNavigate() {
        // Validate for first-time setup
        if (isFirstTimeSetup && userInterestIds.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least 1 interest to continue", Toast.LENGTH_SHORT).show()
            return
        }

        // Save to database (PreferencesManager) instead of dummy data
        preferencesManager.saveSelectedGenres(userInterestIds.toSet())
        preferencesManager.setPreferencesCompleted(true)

        // Show confirmation
        Toast.makeText(requireContext(), "Interests saved!", Toast.LENGTH_SHORT).show()

        // Navigate based on flow
        if (isFirstTimeSetup) {
            // Navigate to home for first-time setup
            findNavController().navigate(R.id.nav_home)
        } else {
            // Navigate back to profile for editing
            findNavController().navigateUp()
        }

        // Notify listeners that interests were updated
        findNavController().previousBackStackEntry?.savedStateHandle?.set("interestsUpdated", userInterestIds)
    }

    // =========== Functions ===========
    // Load genres from database instead of CSV
    private fun loadGenresFromDatabase(): List<Genre> {
        val databaseGenres = databaseHelper.getAllGenres()
        val genres = mutableListOf<Genre>()

        // Convert database genre strings to Genre objects with IDs
        databaseGenres.forEachIndexed { index, genreName ->
            // Use index as ID, or you could modify your database to include genre IDs
            val genre = Genre(
                id = (index + 1).toString(), // Generate ID based on position
                name = genreName
            )
            genres.add(genre)
        }

        return genres
    }

    private fun displayInterests(view: View, allGenres: List<Genre>, selectedIds: List<String>) {
        // Loop through all genres
        for (i in allGenres.indices) {
            val genre = allGenres[i]

            // Find the components in the .xml (e.g., radIconBg1, radIcon1, radText)
            val bgId = requireContext().resources.getIdentifier("radIconBg${i + 1}", "id", requireContext().packageName)
            val iconId = requireContext().resources.getIdentifier("radIcon${i + 1}", "id", requireContext().packageName)
            val textId = requireContext().resources.getIdentifier("radText${i + 1}", "id", requireContext().packageName)


            val bg = view.findViewById<FrameLayout>(bgId)
            val icon = view.findViewById<ImageButton>(iconId)
            val text = view.findViewById<TextView>(textId)

            // Load drawables with the correct name (e.g., Romance -> profile_rad_romance)
            val drawableName = "profile_rad_" + genre.name.lowercase().replace(" ", "_")
            val drawableId = requireContext().resources.getIdentifier(drawableName, "drawable", requireContext().packageName)

            // Set default icon if specific icon doesn't exist
            if (drawableId != 0) {
                icon.setImageResource(drawableId)
            }

            // Set the background and name
            bg.setBackgroundResource(R.drawable.profile_rounded_white)
            text.text = genre.name

            // When the genre is selected (in the user's interests list), replace the icon and background
            if (genre.name in userInterestIds) {
                val highlightDrawableName = drawableName.replace("profile_rad_", "profile_rad2_")
                val highlightDrawableId = requireContext().resources.getIdentifier(
                    highlightDrawableName,
                    "drawable",
                    requireContext().packageName
                )
                if (highlightDrawableId != 0) {
                    icon.setImageResource(highlightDrawableId)
                }
                bg.setBackgroundResource(R.drawable.profile_rounded_red)

                if ((i + 1) % 2 == 0) {
                    bg.setBackgroundResource(R.drawable.profile_rounded_yellow)
                } else {
                    bg.setBackgroundResource(R.drawable.profile_rounded_red)
                }
            }

            // =========== Buttons ===========
            bg.setOnClickListener {
                // If already selected, deselect it
                if (genre.name in userInterestIds) {
                    userInterestIds.remove(genre.name)              // Remove from user's list
                    if (drawableId != 0) {
                        icon.setImageResource(drawableId)           // Change icon back to default
                    }
                    bg.setBackgroundResource(R.drawable.profile_rounded_white)
                } else {
                    // If more than 4 genres are selected, show toast and return listener
                    if (userInterestIds.size >= 4) {
                        Toast.makeText(requireContext(), "Please select up to 4 interests", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    // If not, select it
                    userInterestIds.add(genre.name)                 // Add to user's list
                    val highlightDrawableName = drawableName.replace("profile_rad_", "profile_rad2_")
                    val highlightDrawableId = requireContext().resources.getIdentifier(highlightDrawableName, "drawable", requireContext().packageName)
                    if (highlightDrawableId != 0) {
                        icon.setImageResource(highlightDrawableId)  // Change icon to highlighted version
                    }
                    if ((i + 1) % 2 == 0) {                         // Change background (red for odd, yellow for even)
                        bg.setBackgroundResource(R.drawable.profile_rounded_yellow)
                    } else {
                        bg.setBackgroundResource(R.drawable.profile_rounded_red)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}