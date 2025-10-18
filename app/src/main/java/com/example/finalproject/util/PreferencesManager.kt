package com.example.finalproject.util

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

class PreferencesManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

    // User Info
    fun saveLoggedInUserEmail(email: String) {
        prefs.edit().putString("user_email", email).apply()
    }

    fun getLoggedInUserEmail(): String {
        return prefs.getString("user_email", "") ?: ""
    }

    fun saveProfilePictureUri(uri: String) {
        prefs.edit().putString("profile_pic_uri", uri).apply()
    }

    fun getProfilePictureUri(): String? {
        return prefs.getString("profile_pic_uri", null)
    }

    fun clearUserSession() {
        prefs.edit().clear().apply()
    }

    // Genre Preference
    fun saveSelectedGenres(genres: Set<String>) {
        prefs.edit().putStringSet("selected_genres", genres).apply()
    }

    fun getSelectedGenres(): Set<String> {
        return prefs.getStringSet("selected_genres", emptySet()) ?: emptySet()
    }

    // Favourite Tours
    fun saveFavoriteTours(tourIds: Set<String>) {
        prefs.edit().putStringSet("favorite_tours", tourIds).apply()
    }

    fun getFavoriteTours(): Set<String> {
        return prefs.getStringSet("favorite_tours", emptySet()) ?: emptySet()
    }

    fun addFavoriteTour(tourId: String) {
        val favorites = getFavoriteTours().toMutableSet()
        favorites.add(tourId)
        saveFavoriteTours(favorites)
    }

    fun removeFavoriteTour(tourId: String) {
        val favorites = getFavoriteTours().toMutableSet()
        favorites.remove(tourId)
        saveFavoriteTours(favorites)
    }

    // Use a separate flag to track if preferences are completed
    fun setPreferencesCompleted(completed: Boolean) {
        prefs.edit().putBoolean("preferences_completed", completed).apply()
    }

    fun hasCompletedPreferences(): Boolean {
        return prefs.getBoolean("preferences_completed", false)
    }

    // Optional: You can keep this method if you want to check both
    fun hasSelectedGenres(): Boolean {
        return getSelectedGenres().isNotEmpty()
    }

    fun clearPreferences() {
        prefs.edit().remove("selected_genres").apply()
        setPreferencesCompleted(false)
    }
}