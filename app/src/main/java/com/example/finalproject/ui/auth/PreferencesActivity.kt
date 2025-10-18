package com.example.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.finalproject.DatabaseHelper
import com.example.finalproject.MainActivity
import com.example.finalproject.R
import com.example.finalproject.databinding.ActivityPreferencesBinding
import com.example.finalproject.util.PreferencesManager

class PreferencesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreferencesBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var preferencesManager: PreferencesManager

    private val selectedGenres = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        preferencesManager = PreferencesManager(this)

        // Load previously selected genres
        selectedGenres.addAll(preferencesManager.getSelectedGenres())

        setupToolbar()
        setupFlexboxGenres()
        setupClickListeners()
        updateSaveButton()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupFlexboxGenres() {
        val allGenres = dbHelper.getAllGenres()
        binding.flexboxGenres.removeAllViews()

        allGenres.forEach { genre ->
            val chipView = layoutInflater.inflate(R.layout.item_genre_chip, binding.flexboxGenres, false) as TextView

            chipView.text = genre
            chipView.isSelected = selectedGenres.contains(genre)

            // Apply theme to chip based on selection state
            updateChipAppearance(chipView)

            chipView.setOnClickListener {
                chipView.isSelected = !chipView.isSelected

                if (chipView.isSelected) {
                    selectedGenres.add(genre)
                } else {
                    selectedGenres.remove(genre)
                }

                updateChipAppearance(chipView)
                updateSaveButton()
            }

            binding.flexboxGenres.addView(chipView)
        }
    }

    private fun updateChipAppearance(chipView: TextView) {
        if (chipView.isSelected) {
            // Selected chip - use your theme's primary colors
            chipView.setBackgroundResource(R.drawable.genre_chip_selected)
            chipView.setTextColor(ContextCompat.getColor(this, R.color.black)) // colorOnPrimary
            chipView.background.setTint(ContextCompat.getColor(this, R.color.main)) // colorPrimary
        } else {
            // Unselected chip - use surface colors
            chipView.setBackgroundResource(R.drawable.genre_chip_default)
            chipView.setTextColor(ContextCompat.getColor(this, R.color.dark_shades)) // colorOnSurface
            chipView.background.setTint(ContextCompat.getColor(this, R.color.white)) // like your button background
        }

        // Add stroke for unselected state to match your button style
        if (!chipView.isSelected) {
            chipView.setBackgroundResource(R.drawable.genre_chip_default)
        }
    }

    private fun setupClickListeners() {
        binding.btnSavePreferences.setOnClickListener {
            savePreferences()
            navigateToMain()
        }

        binding.btnSkip.setOnClickListener {
            navigateToMain()
        }
    }

    private fun updateSaveButton() {
        val hasSelections = selectedGenres.isNotEmpty()
        binding.btnSavePreferences.isEnabled = hasSelections

        // Update button appearance based on state (matching your theme)
        if (hasSelections) {
            // Enabled state - use your primary color
            binding.btnSavePreferences.setBackgroundColor(
                ContextCompat.getColor(this, R.color.main) // colorPrimary
            )
            binding.btnSavePreferences.setTextColor(
                ContextCompat.getColor(this, R.color.black) // colorOnPrimary
            )
        } else {
            // Disabled state - use muted version
            binding.btnSavePreferences.setBackgroundColor(
                ContextCompat.getColor(this, R.color.light_accent) // lighter version
            )
            binding.btnSavePreferences.setTextColor(
                ContextCompat.getColor(this, R.color.dark_shades) // muted text
            )
        }

        binding.tvSelectedCount.text =
            if (hasSelections) "${selectedGenres.size} genres selected"
            else "Select at least 1 genre"

        binding.tvSelectedCount.setTextColor(
            ContextCompat.getColor(this, R.color.dark_shades) // colorOnSurface
        )
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun savePreferences() {
        preferencesManager.saveSelectedGenres(selectedGenres)
        preferencesManager.setPreferencesCompleted(true)

        // Show confirmation
        android.widget.Toast.makeText(this, "Preferences saved!", android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        dbHelper.close()
    }
}