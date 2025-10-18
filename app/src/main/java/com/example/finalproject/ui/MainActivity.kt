package com.example.finalproject

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.finalproject.databinding.ActivityMainBinding
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.finalproject.util.FavoriteManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize FavoriteManager
        FavoriteManager.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Nav component set up
        setSupportActionBar(binding.toolbar)
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        setupActionBarWithNavController(navController, AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_explore,
                R.id.nav_thread,
                R.id.nav_profile
            )
        ))

        // Replace the setupWithNavController with custom navigation handling
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    if (navController.currentDestination?.id != R.id.nav_home) {
                        navController.navigate(R.id.nav_home)
                    }
                    true
                }

                R.id.nav_explore -> {
                    if (navController.currentDestination?.id != R.id.nav_explore) {
                        navController.navigate(R.id.nav_explore)
                    }
                    true
                }

                R.id.nav_thread -> {
                    if (navController.currentDestination?.id != R.id.nav_thread) {
                        navController.navigate(R.id.nav_thread)
                    }
                    true
                }

                R.id.nav_profile -> {
                    if (navController.currentDestination?.id != R.id.nav_profile) {
                        navController.navigate(R.id.nav_profile)
                    }
                    true
                }
                else -> false
            }
        }

        // To obsrve nav changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_home -> {
                    binding.bottomNav.selectedItemId = R.id.nav_home
                    supportActionBar?.title = "Home"
                }

                R.id.nav_explore -> {
                    binding.bottomNav.selectedItemId = R.id.nav_explore
                    supportActionBar?.title = "Explore"
                }

                R.id.nav_thread -> {
                    binding.bottomNav.selectedItemId = R.id.nav_thread
                    supportActionBar?.title = ""
                }

                R.id.nav_profile -> {
                    binding.bottomNav.selectedItemId = R.id.nav_profile
                    supportActionBar?.title = ""
                }


                R.id.tourDetailsFragment -> supportActionBar?.title = ""
                R.id.dramasByLocationFragment -> supportActionBar?.title = ""
                R.id.personalisedToursFragment -> supportActionBar?.title = ""
                R.id.profileEditFragment -> supportActionBar?.title = ""
                R.id.profileFavouritesFragment -> supportActionBar?.title = ""
                R.id.profileSettingsFragment -> supportActionBar?.title = ""
                R.id.profileTipsFragment -> supportActionBar?.title = ""
                R.id.profileAboutFragment -> supportActionBar?.title = ""
                R.id.profileInterestsFragment -> supportActionBar?.title = ""
            }
        }

    }

    fun setBottomNavVisibility(isVisible: Boolean) {
        binding.bottomNav.visibility = if (isVisible) View.VISIBLE else View.GONE
    }
}
