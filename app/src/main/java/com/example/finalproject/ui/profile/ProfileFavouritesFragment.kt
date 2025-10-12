package com.example.finalproject.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.finalproject.databinding.FragmentProfileFavouritesBinding
import com.example.finalproject.model.Drama
import com.example.finalproject.util.Favoritemanager
import com.example.finalproject.util.loadDramasFromCSV
import androidx.core.view.isEmpty
import com.example.finalproject.DatabaseHelper
import com.example.finalproject.R
import com.example.finalproject.model.Tour
import com.example.finalproject.ui.ProfileMainFragment
import com.example.finalproject.util.PreferencesManager

// Suppress string/img warnings
@SuppressLint("SetTextI18n")

class ProfileFavouritesFragment : Fragment() {

    private var _binding: FragmentProfileFavouritesBinding? = null
    private val binding get() = _binding!!

    private lateinit var allDramas: List<Drama>
    private lateinit var userDramas: List<Drama>
    private lateinit var userFavoriteTours: List<Tour>
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var preferencesManager: PreferencesManager


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileFavouritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    // ============================== CONTENT ==============================
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize database helper and preferences manager
        databaseHelper = DatabaseHelper(requireContext())
        preferencesManager = PreferencesManager(requireContext())


        // Load favorite tours from database
        val favoriteTourIds = preferencesManager.getFavoriteTours()
        userFavoriteTours = databaseHelper.getAvailableTours().filter { tour ->
            tour.dramaId in favoriteTourIds
        }

        // Load favorite dramas
        allDramas = databaseHelper.getAllDramas()
        val favoriteDramaIds: List<String> = Favoritemanager.getFavorites().map { it.dramaId }
        userDramas = allDramas.filter { it.dramaId in favoriteDramaIds }


        // =========== Toggle View ===========
        val btnThreads = binding.btnThreads
        val btnDramas = binding.btnDramas

        when (arguments?.getString("toggle")) {     // If Toggle is 'Threads', show Threads container
            "Threads" -> {
                btnThreads.isChecked = true
                btnDramas.isChecked = false
                showThreads()
            }
            "Dramas" -> {                           // If Toggle is 'Dramas', show Threads container
                btnThreads.isChecked = false
                btnDramas.isChecked = true
                showDramas()
            }
        }

        // =========== Buttons ===========
        binding.btnBack.setOnClickListener {        // To ProfileMainFragment
            findNavController().navigateUp()
        }

        btnThreads.setOnClickListener {             // To show Threads container
            btnThreads.isChecked = true
            btnDramas.isChecked = false
            showThreads()
        }

        btnDramas.setOnClickListener {              // To show Dramas container
            btnThreads.isChecked = false
            btnDramas.isChecked = true
            showDramas()
        }
    }
    // =========== Functions ===========
    // fun showThreads: Show lists of favourite threads
    // fun showDrama: Show lists of favourite dramas
    private fun showThreads() {
        val threadsLayout = binding.threadsLayout
        val dramasLayout = binding.dramasLayout
        val nothingLayout = binding.nothingLayout

        dramasLayout.visibility = View.GONE             // Remove old containers
        nothingLayout.visibility = View.GONE
        threadsLayout.removeAllViews()

        if (userFavoriteTours.isNotEmpty()) {
            threadsLayout.visibility = View.VISIBLE     // Show 'threads' container
            dramasLayout.visibility = View.GONE         // Hide 'dramas' container
            nothingLayout.visibility = View.GONE        // Hide 'nothing' container

            var groupIndex = 1

            userFavoriteTours.forEach { tour ->
                val itemThread = layoutInflater.inflate(R.layout.item_thread, threadsLayout, false)
                val container = itemThread.findViewById<LinearLayout>(R.id.contentContainer)
                val textCount = itemThread.findViewById<TextView>(R.id.textCount)

                textCount.text = "No. $groupIndex"
                groupIndex++

                // Get locations for this tour
                val tourLocations = databaseHelper.getTourLocations(tour.dramaId)

                // Create sub thread container for each location in the tour
                tourLocations.forEach { location ->
                    val contentView = layoutInflater.inflate(R.layout.item_thread_content, container, false)
                    val textName = contentView.findViewById<TextView>(R.id.threadsName)
                    val textNameTH = contentView.findViewById<TextView>(R.id.threadsNameTH)

                    // Show container and write name
                    container.addView(contentView)
                    textName.text = location.name
                    textNameTH.text = location.name // You might want to add Thai names to your database

                    // Get buttons
                    val btnExplored = itemThread.findViewById<RadioButton>(R.id.btnExplored)
                    val btnOngoing = itemThread.findViewById<RadioButton>(R.id.btnOngoing)
                    val btnRemove = itemThread.findViewById<RadioButton>(R.id.btnRemove)

                    // Check if this tour is in favorites (explored state)
                    val isTourFavorite = preferencesManager.getFavoriteTours().contains(tour.dramaId)
                    if (isTourFavorite) {
                        btnExplored.isChecked = true
                        btnOngoing.isChecked = false
                        // Change the format of the buttons
                        btnExplored.setTextColor(Color.WHITE)
                        btnOngoing.setTextColor(Color.BLACK)
                    } else {
                        btnExplored.isChecked = false
                        btnOngoing.isChecked = true
                        // Change the format of the buttons
                        btnExplored.setTextColor(Color.BLACK)
                        btnOngoing.setTextColor(Color.WHITE)
                    }


                    // =========== Buttons ===========
                    btnExplored.setOnClickListener {                // To mark as 'explored'
                        preferencesManager.addFavoriteTour(tour.dramaId)
                        btnExplored.isChecked = true
                        btnOngoing.isChecked = false
                        // Change the format of the buttons
                        btnExplored.setTextColor(Color.WHITE)
                        btnOngoing.setTextColor(Color.BLACK)
                    }

                    btnOngoing.setOnClickListener {                 // To mark as 'ongoing'
                        preferencesManager.removeFavoriteTour(tour.dramaId)
                        btnExplored.isChecked = false
                        btnOngoing.isChecked = true
                        // Change the format of the buttons
                        btnExplored.setTextColor(Color.BLACK)
                        btnOngoing.setTextColor(Color.WHITE)
                    }

                    btnRemove.setOnClickListener {                  // To remove thread container
                        preferencesManager.removeFavoriteTour(tour.dramaId)
                        threadsLayout.removeView(itemThread)

                        // Re-number
                        var number = 1
                        for (i in 0 until threadsLayout.childCount) {
                            val threadItem = threadsLayout.getChildAt(i)
                            val countText = threadItem.findViewById<TextView>(R.id.textCount)
                            countText.text = "No. $number"
                            number++
                        }

                        // If the thread containers get removed until empty, show 'nothing' container
                        if (threadsLayout.isEmpty()) {
                            threadsLayout.visibility = View.GONE
                            binding.nothingLayout.visibility = View.VISIBLE
                        }
                    }
                }
                // Create sub thread container
                threadsLayout.addView(itemThread)

                // Show thread containers when the list becomes available again
                threadsLayout.visibility = View.VISIBLE
            }
        }
    }

    // Dramas
    fun showDramas() {
        val threadsLayout = binding.threadsLayout
        val dramasLayout = binding.dramasLayout
        val nothingLayout = binding.nothingLayout

        threadsLayout.visibility = View.GONE                    // Remove old containers
        nothingLayout.visibility = View.GONE
        dramasLayout.removeAllViews()

        if (userDramas.isNotEmpty()) {
            dramasLayout.visibility = View.VISIBLE              // Show 'dramas' container
            threadsLayout.visibility = View.GONE                // Hide 'threads' container
            nothingLayout.visibility = View.GONE                // Hide 'nothing' container

            // Create drama container
            userDramas.forEach { drama ->
                val itemDrama = layoutInflater.inflate(R.layout.item_drama, dramasLayout, false)
                val imgDrama = itemDrama.findViewById<ImageButton>(R.id.imgDramas)
                val textDrama = itemDrama.findViewById<TextView>(R.id.textDramas)
                val btnRemove = itemDrama.findViewById<RadioButton>(R.id.btnRemove)

                // Get image and write name
                Glide.with(this)
                    .load(drama.posterUrl)
                    .placeholder(R.drawable.profile_pic_default)
                    .error(R.drawable.profile_pic_default)
                    .into(imgDrama)
                textDrama.text = drama.titleEn

                // =========== Buttons ===========
                btnRemove.setOnClickListener {
                    Favoritemanager.removeFavorite(drama)
                    dramasLayout.removeView(itemDrama)
                    // If the drama containers get removed until empty, show 'nothing' container
                    if (Favoritemanager.getFavorites().isEmpty()) {
                        dramasLayout.visibility = View.GONE
                        nothingLayout.visibility = View.VISIBLE
                    }
                }
                // Create drama container
                dramasLayout.addView(itemDrama)

                // Show drama containers when the list becomes available again
                dramasLayout.visibility = View.VISIBLE
            }
        } else {
            threadsLayout.visibility = View.GONE
            dramasLayout.visibility = View.GONE
            nothingLayout.visibility = View.VISIBLE     // Shows 'nothing' container
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
