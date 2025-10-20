package com.example.finalproject.util

import android.content.Context
import android.content.SharedPreferences
import com.example.finalproject.model.Drama
import com.example.finalproject.ui.thread.data.LocationDramaItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.finalproject.ui.thread.data.Explored

object FavoriteManager {

    private const val PREFS_NAME = "favorites_prefs"
    private const val KEY_DRAMA_FAVORITES = "favorites_list"
    private const val KEY_THREAD_FAVORITES = "thread_favorites_list"
    private const val KEY_EXPLORED = "explored_status"

    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        if (!::sharedPreferences.isInitialized) {
            sharedPreferences = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun prefsReady(): Boolean = ::sharedPreferences.isInitialized

    // --- Drama Favorites ---
    fun getDramaFavorites(): MutableList<Drama> {
        if (!prefsReady()) return mutableListOf()
        val json = sharedPreferences.getString(KEY_DRAMA_FAVORITES, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<Drama>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun isDramaFavorite(drama: Drama): Boolean {
        if (!prefsReady()) return false
        return getDramaFavorites().any { it.dramaId == drama.dramaId }
    }

    fun addDramaFavorite(drama: Drama) {
        if (!prefsReady()) return
        val favorites = getDramaFavorites()
        if (favorites.none { it.dramaId == drama.dramaId }) {
            favorites.add(drama)
            saveDramaFavorites(favorites)
        }
    }

    fun removeDramaFavorite(drama: Drama) {
        if (!prefsReady()) return
        val favorites = getDramaFavorites()
        val changed = favorites.removeAll { it.dramaId == drama.dramaId }
        if (changed) saveDramaFavorites(favorites)
    }

    private fun saveDramaFavorites(favorites: List<Drama>) {
        if (!prefsReady()) return
        val json = gson.toJson(favorites)
        sharedPreferences.edit().putString(KEY_DRAMA_FAVORITES, json).apply()
    }

    // --- Thread (LocationDramaItem) Favorites ---
    fun getThreadFavorites(): MutableList<LocationDramaItem> {
        if (!prefsReady()) return mutableListOf()
        val json = sharedPreferences.getString(KEY_THREAD_FAVORITES, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<LocationDramaItem>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun isThreadFavorite(item: LocationDramaItem): Boolean {
        if (!prefsReady()) return false
        return getThreadFavorites().any { it.titleEn == item.titleEn }
    }

    fun addThreadFavorite(item: LocationDramaItem) {
        if (!prefsReady()) return
        val favorites = getThreadFavorites()
        if (favorites.none { it.titleEn == item.titleEn }) {
            favorites.add(item)
            saveThreadFavorites(favorites)
        }
    }

    fun removeThreadFavorite(item: LocationDramaItem) {
        if (!prefsReady()) return
        val favorites = getThreadFavorites()
        val changed = favorites.removeAll { it.titleEn == item.titleEn }
        if (changed) saveThreadFavorites(favorites)
    }

    private fun saveThreadFavorites(favorites: List<LocationDramaItem>) {
        if (!prefsReady()) return
        val json = gson.toJson(favorites)
        sharedPreferences.edit().putString(KEY_THREAD_FAVORITES, json).apply()
    }

    // =========== Explored Threads ===========
    fun getExploredList(): MutableList<Explored> {
        if (!prefsReady()) return mutableListOf()
        val json = sharedPreferences.getString(KEY_EXPLORED, null) ?: return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<Explored>>() {}.type
            gson.fromJson<MutableList<Explored>>(json, type) ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }

    fun getExploredStatus(dramaId: String): Explored? {
        return getExploredList().find { it.dramaId == dramaId }
    }

    fun setExploredStatus(dramaId: String, explored: Boolean) {
        val list = getExploredList().toMutableList()
        val existing = list.indexOfFirst { it.dramaId == dramaId }
        if (existing != -1) {
            list[existing] = Explored(dramaId, explored)
        } else {
            list.add(Explored(dramaId, explored))
        }
        sharedPreferences.edit().putString(KEY_EXPLORED, gson.toJson(list)).apply()
    }
}
