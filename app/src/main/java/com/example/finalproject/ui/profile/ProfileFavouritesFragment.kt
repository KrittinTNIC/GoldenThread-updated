package com.example.finalproject.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.finalproject.R
import com.example.finalproject.databinding.FragmentProfileFavouritesBinding
import com.example.finalproject.util.FavoriteManager
import androidx.core.view.isEmpty
import com.example.finalproject.DatabaseHelper

@SuppressLint("SetTextI18n")
class ProfileFavouritesFragment : Fragment() {

    private var _binding: FragmentProfileFavouritesBinding? = null
    private lateinit var dbHelper: DatabaseHelper
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileFavouritesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = DatabaseHelper(requireContext())
        FavoriteManager.init(requireContext())

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnDramas.setOnClickListener {
            showDramas()
        }

        binding.btnThreads.setOnClickListener {
            showThreads()
        }

        val toggle = arguments?.getString("toggle") ?: "Dramas"
        if (toggle == "Threads") {
            showThreads()
            binding.btnThreads.isChecked = true
            binding.btnDramas.isChecked = false
        } else {
            showDramas()
            binding.btnThreads.isChecked = false
            binding.btnDramas.isChecked = true
        }
    }

    private fun showDramas() {
        binding.dramasLayout.visibility = View.VISIBLE
        binding.threadsLayout.visibility = View.GONE
        loadDramaFavorites()
    }

    private fun showThreads() {
        binding.dramasLayout.visibility = View.GONE
        binding.threadsLayout.visibility = View.VISIBLE
        loadThreadFavorites()
    }

    private fun loadDramaFavorites() {
        val favoriteDramas = FavoriteManager.getDramaFavorites()
        val dramasLayout = binding.dramasLayout
        dramasLayout.removeAllViews()

        if (favoriteDramas.isEmpty()) {
            binding.nothingLayout.visibility = View.VISIBLE
            dramasLayout.visibility = View.GONE
        } else {
            binding.nothingLayout.visibility = View.GONE
            dramasLayout.visibility = View.VISIBLE
            favoriteDramas.forEach { drama ->
                val dramaView = layoutInflater.inflate(R.layout.item_drama, dramasLayout, false)

                val title = dramaView.findViewById<TextView>(R.id.textDramas)
                val img = dramaView.findViewById<ImageButton>(R.id.imgDramas)

                title.text = drama.titleEn
                Glide.with(this).load(drama.posterUrl).into(img)

                // =========== Buttons ===========
                val removeBtn = dramaView.findViewById<RadioButton>(R.id.btnRemove)
                removeBtn.setOnClickListener {
                    removeBtn.isChecked = false
                    AlertDialog.Builder(requireContext())
                        .setTitle("Remove Favorite")
                        .setMessage("Are you sure you want to remove '${drama.titleEn}' from favorites?")
                        .setPositiveButton("Remove") { _, _ ->
                            FavoriteManager.removeDramaFavorite(drama)
                            loadDramaFavorites() // Refresh the list
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                dramasLayout.addView(dramaView)
            }
        }
    }

    private fun loadThreadFavorites() {
        val favoriteThreads = FavoriteManager.getThreadFavorites()
        val threadsLayout = binding.threadsLayout
        threadsLayout.removeAllViews()

        if (favoriteThreads.isEmpty()) {
            binding.nothingLayout.visibility = View.VISIBLE
            threadsLayout.visibility = View.GONE
            return
        } else {
            binding.nothingLayout.visibility = View.GONE
            threadsLayout.visibility = View.VISIBLE
        }

        // Group by title
        val threadsGrouped = favoriteThreads.groupBy { it.titleEn }

        threadsGrouped.entries.forEachIndexed { index, entry ->
            val titleEn = entry.key

            // Find drama to get dramaId
            val drama = dbHelper.getAvailableTours().find { it.titleEn == titleEn }
            val locations = drama?.let { dbHelper.getTourLocations(it.dramaId) } ?: listOf()

            // Thread layout
            val threadView = layoutInflater.inflate(R.layout.item_thread, threadsLayout, false)
            val count = threadView.findViewById<TextView>(R.id.textCount)

            count?.text = "No. ${index + 1}"

            val contentContainer = threadView.findViewById<LinearLayout>(R.id.contentContainer)
            contentContainer.removeAllViews()
            // Thread content
            locations.forEach { loc ->
                val contentView = layoutInflater.inflate(R.layout.item_thread_content, contentContainer, false)
                val name = contentView.findViewById<TextView>(R.id.threadsName)
                val nameTH = contentView.findViewById<TextView>(R.id.threadsNameTH)

                name.text = loc.name
                nameTH.text = loc.address

                contentContainer.addView(contentView)
            }

            // =========== Buttons ===========
            val exploredBtn = threadView.findViewById<RadioButton>(R.id.btnExplored)
            val ongoingBtn = threadView.findViewById<RadioButton>(R.id.btnOngoing)

            val dramaId = drama?.dramaId ?: ""
            val exploredObj = FavoriteManager.getExploredStatus(drama?.dramaId ?: "")
            exploredBtn.isChecked = exploredObj?.explored == true
            ongoingBtn.isChecked = exploredObj?.explored == false

            fun textColour() {
                exploredBtn.setTextColor(
                    if (exploredBtn.isChecked)
                        Color.WHITE
                    else
                        Color.BLACK)
                ongoingBtn.setTextColor(
                    if (ongoingBtn.isChecked)
                        Color.WHITE else
                            Color.BLACK)
            }

            textColour()
            exploredBtn.setOnClickListener {
                if (dramaId.isNotEmpty()) {
                    FavoriteManager.setExploredStatus(dramaId, true)
                    exploredBtn.isChecked = true
                    ongoingBtn.isChecked = false
                    textColour()

                    findNavController().previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("threadsUpdated", true)
                }
            }

            ongoingBtn.setOnClickListener {
                if (dramaId.isNotEmpty()) {
                    FavoriteManager.setExploredStatus(dramaId, false)
                    ongoingBtn.isChecked = true
                    exploredBtn.isChecked = false
                    textColour()

                    findNavController().previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("threadsUpdated", true)
                }
            }

            val removeBtn = threadView.findViewById<ImageButton>(R.id.btnRemove)
            removeBtn.setOnClickListener {

            }
            removeBtn.setOnClickListener {
                val prevExplored = exploredBtn.isChecked
                val prevOngoing = ongoingBtn.isChecked

                AlertDialog.Builder(requireContext())
                    .setTitle("Remove Favorite")
                    .setMessage("Are you sure you want to remove No '${index + 1}' from favorites?")
                    .setPositiveButton("Remove") { _, _ ->
                        val favoriteThreads = FavoriteManager.getThreadFavorites()
                        favoriteThreads
                            .filter { it.titleEn == titleEn }
                            .forEach { FavoriteManager.removeThreadFavorite(it)
                        }
                        threadsLayout.removeView(threadView)

                        for (i in 0 until threadsLayout.childCount) {
                            val child = threadsLayout.getChildAt(i)
                            val countText = child.findViewById<TextView>(R.id.textCount)
                            countText?.text = "No. ${i + 1}"
                        }

                        if (threadsLayout.isEmpty()) {
                            binding.nothingLayout.visibility = View.VISIBLE
                            threadsLayout.visibility = View.GONE
                        }

                        val prevEntry = findNavController().previousBackStackEntry
                        prevEntry?.savedStateHandle?.set("threadsUpdated", false)
                        prevEntry?.savedStateHandle?.set("threadsUpdated", true)
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        exploredBtn.isChecked = prevExplored
                        ongoingBtn.isChecked = prevOngoing
                        textColour()
                    }
                    .show()
            }

            threadsLayout.addView(threadView)
        }
    }
}