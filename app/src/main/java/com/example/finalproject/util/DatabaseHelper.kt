package com.example.finalproject

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.finalproject.model.Drama
import com.example.finalproject.model.FeaturedLocation
import com.example.finalproject.model.LocationDetail
import com.example.finalproject.model.Tour
//import com.example.finalproject.model.FeaturedLocation
//import com.example.finalproject.model.LocationDetail
//import com.example.finalproject.model.Tour
import java.io.FileOutputStream

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val TAG = "DatabaseHelper"
        private const val DATABASE_NAME = "GoldenThread.db"
        private const val DATABASE_VERSION = 1
    }

    init {
        copyDatabaseIfNeeded(context)
    }

    private fun copyDatabaseIfNeeded(context: Context) {
        val dbPath = context.getDatabasePath(DATABASE_NAME)

        if (!dbPath.exists()) {
            try {
                dbPath.parentFile?.mkdirs()
                context.assets.open(DATABASE_NAME).use { input ->
                    FileOutputStream(dbPath).use { output ->
                        input.copyTo(output)
                        Log.d(TAG, "Database copied successfully from assets")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error copying database", e)
                throw RuntimeException("Failed to copy database", e)
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Database is pre-populated from assets
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades when needed
    }

    // Genre Methods
    fun getAllGenres(): List<String> {
        val genres = mutableListOf<String>()
        val db = readableDatabase

        try {
            val cursor = db.rawQuery(
                "SELECT DISTINCT genre FROM genres ORDER BY genre",
                null
            )

            while (cursor.moveToNext()) {
                cursor.getString(0)?.let { genre ->
                    genres.add(genre)
                }
            }
            cursor.close()

            Log.d(TAG, "Loaded ${genres.size} genres from database")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading genres", e)
        } finally {
            db.close()
        }

        return genres
    }

    // Drama Methods
    fun getAllDramas(): List<Drama> {
        return queryDramas(
            """
            SELECT d.drama_id, d.title_en, d.title_th, d.release_year, 
                   d.duration_min, d.summary, d.poster_url, 
                   GROUP_CONCAT(g.genre) as genres
            FROM dramas d
            LEFT JOIN drama_genres dg ON d.drama_id = dg.drama_id
            LEFT JOIN genres g ON dg.genre_id = g.genre_id
            GROUP BY d.drama_id
            ORDER BY d.release_year DESC
            """
        )
    }

    fun getDramasByGenres(genres: Set<String>): List<Drama> {
        if (genres.isEmpty()) return getAllDramas()

        val placeholders = genres.joinToString(",") { "?" }
        val selectionArgs = genres.toTypedArray()

        return queryDramas(
            """
            SELECT DISTINCT d.drama_id, d.title_en, d.title_th, d.release_year, 
                   d.duration_min, d.summary, d.poster_url, 
                   GROUP_CONCAT(g.genre) as genres
            FROM dramas d
            INNER JOIN drama_genres dg ON d.drama_id = dg.drama_id
            INNER JOIN genres g ON dg.genre_id = g.genre_id
            WHERE g.genre IN ($placeholders)
            GROUP BY d.drama_id
            ORDER BY d.release_year DESC
            """,
            selectionArgs
        )
    }

    private fun queryDramas(query: String, selectionArgs: Array<String>? = null): List<Drama> {
        val dramas = mutableListOf<Drama>()
        val db = readableDatabase

        try {
            val cursor = db.rawQuery(query, selectionArgs)

            while (cursor.moveToNext()) {
                dramas.add(
                    Drama(
                        dramaId = cursor.getString(0),
                        titleEn = cursor.getString(1) ?: "",
                        titleTh = cursor.getString(2) ?: "",
                        releaseYear = cursor.getInt(3),
                        duration = cursor.getString(4) ?: "",
                        summary = cursor.getString(5) ?: "",
                        posterUrl = cursor.getString(6) ?: "",
                        genre = cursor.getString(7) ?: "Unknown"
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error querying dramas", e)
        } finally {
            db.close()
        }

        return dramas
    }

    // Tour Methods
    fun getAvailableTours(): List<Tour> {
        return queryTours(
            """
            SELECT d.drama_id, d.title_en, d.title_th, d.poster_url,
                   COUNT(DISTINCT dl.location_id) as location_count,
                   SUM(dl.car_travel_min) as total_travel_time,
                   d.summary
            FROM dramas d
            INNER JOIN drama_locations dl ON d.drama_id = dl.drama_id
            GROUP BY d.drama_id
            HAVING location_count > 0
            ORDER BY location_count DESC
            """
        )
    }

    fun getPopularTours(): List<Tour> {
        return queryTours(
            """
            SELECT d.drama_id, d.title_en, d.title_th, d.poster_url,
                   COUNT(DISTINCT dl.location_id) as location_count,
                   SUM(dl.car_travel_min) as total_travel_time,
                   d.summary
            FROM dramas d
            INNER JOIN drama_locations dl ON d.drama_id = dl.drama_id
            GROUP BY d.drama_id
            ORDER BY location_count DESC
            LIMIT 12
            """
        )
    }

    fun getToursByGenres(genres: List<String>): List<Tour> {
        if (genres.isEmpty()) return getPopularTours()

        val placeholders = genres.joinToString(",") { "?" }

        return queryTours(
            """
            SELECT DISTINCT d.drama_id, d.title_en, d.title_th, d.poster_url,
                   COUNT(DISTINCT dl.location_id) as location_count,
                   SUM(dl.car_travel_min) as total_travel_time,
                   d.summary
            FROM dramas d
            INNER JOIN drama_locations dl ON d.drama_id = dl.drama_id
            INNER JOIN drama_genres dg ON d.drama_id = dg.drama_id
            INNER JOIN genres g ON dg.genre_id = g.genre_id
            WHERE g.genre IN ($placeholders)
            GROUP BY d.drama_id
            ORDER BY location_count DESC
            """,
            genres.toTypedArray()
        )
    }

    private fun queryTours(query: String, selectionArgs: Array<String>? = null): List<Tour> {
        val tours = mutableListOf<Tour>()
        val db = readableDatabase

        try {
            val cursor = db.rawQuery(query, selectionArgs)

            while (cursor.moveToNext()) {
                tours.add(
                    Tour(
                        dramaId = cursor.getString(0),
                        titleEn = cursor.getString(1) ?: "",
                        titleTh = cursor.getString(2) ?: "",
                        posterUrl = cursor.getString(3) ?: "",
                        locationCount = cursor.getInt(4),
                        totalTravelTime = cursor.getInt(5),
                        description = cursor.getString(6) ?: ""
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error querying tours", e)
        } finally {
            db.close()
        }

        return tours
    }

    fun getDramaById(dramaId: String): Drama? {
        val db = this.readableDatabase
        var drama: Drama? = null
        val cursor = db.query(
            "dramas",                // The table to query
            null,                    // The columns to return (null returns all)
            "drama_id = ?",          // The columns for the WHERE clause
            arrayOf(dramaId),        // The values for the WHERE clause
            null,                    // don't group the rows
            null,                    // don't filter by row groups
            null                     // The sort order
        )

        if (cursor.moveToFirst()) {
            drama = Drama(
                dramaId = cursor.getString(0),
                titleEn = cursor.getString(1) ?: "",
                titleTh = cursor.getString(2) ?: "",
                posterUrl = cursor.getString(3) ?: "",
                releaseYear = cursor.getInt(4),
                duration = cursor.getString(5) ?: "",
                summary = cursor.getString(6) ?: "",
                genre = cursor.getString(7) ?: "Unknown"
            )
        }

        cursor.close()
        db.close()
        return drama
    }


    fun getDramasByLocation(locationId: String): List<Tour> {
        val dramas = mutableListOf<Tour>()
        val db = readableDatabase

        try {
            val cursor = db.rawQuery(
                """
            SELECT DISTINCT d.drama_id, d.title_en, d.title_th, d.poster_url,
                   COUNT(DISTINCT dl2.location_id) as location_count,
                   SUM(dl2.car_travel_min) as total_travel_time,
                   d.summary
            FROM dramas d
            INNER JOIN drama_locations dl ON d.drama_id = dl.drama_id
            INNER JOIN drama_locations dl2 ON d.drama_id = dl2.drama_id
            WHERE dl.location_id = ?
            GROUP BY d.drama_id
            ORDER BY location_count DESC
            """,
                arrayOf(locationId)
            )

            while (cursor.moveToNext()) {
                dramas.add(
                    Tour(
                        dramaId = cursor.getString(0),
                        titleEn = cursor.getString(1) ?: "",
                        titleTh = cursor.getString(2) ?: "",
                        posterUrl = cursor.getString(3) ?: "",
                        locationCount = cursor.getInt(4),
                        totalTravelTime = cursor.getInt(5),
                        description = cursor.getString(6) ?: ""
                    )
                )
            }
            cursor.close()

            Log.d(TAG, "Loaded ${dramas.size} dramas for location $locationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dramas by location $locationId", e)
        } finally {
            db.close()
        }

        return dramas
    }
    fun getLocationById(locationId: String): FeaturedLocation? {
        val db = readableDatabase

        try {
            val cursor = db.rawQuery(
                """
            SELECT l.location_id, l.name_en, l.address,
                   COUNT(DISTINCT dl.drama_id) as drama_count
            FROM locations l
            LEFT JOIN drama_locations dl ON l.location_id = dl.location_id
            WHERE l.location_id = ?
            GROUP BY l.location_id
            """,
                arrayOf(locationId)
            )

            return if (cursor.moveToFirst()) {
                FeaturedLocation(
                    id = cursor.getString(0),
                    name = cursor.getString(1) ?: "Unknown Location",
                    imageUrl = "placeholder_poster.png",
                    city = extractCityFromAddress(cursor.getString(2)),
                    dramaCount = cursor.getInt(3)
                )
            } else {
                null
            }.also {
                cursor.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading location $locationId", e)
            return null
        } finally {
            db.close()
        }
    }

    // Location Methods
    fun getFeaturedLocations(): List<FeaturedLocation> {
        val locations = mutableListOf<FeaturedLocation>()
        val db = readableDatabase

        try {
            val cursor = db.rawQuery(
                """
                SELECT l.location_id, l.name_en, l.address,
                       COUNT(DISTINCT dl.drama_id) as drama_count
                FROM locations l
                LEFT JOIN drama_locations dl ON l.location_id = dl.location_id
                GROUP BY l.location_id
                ORDER BY drama_count DESC
                LIMIT 10
                """, null
            )

            while (cursor.moveToNext()) {
                locations.add(
                    FeaturedLocation(
                        id = cursor.getString(0),
                        name = cursor.getString(1) ?: "Unknown Location",
                        imageUrl = "placeholder_poster.png",
                        city = extractCityFromAddress(cursor.getString(2)),
                        dramaCount = cursor.getInt(3)
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading featured locations", e)
        } finally {
            db.close()
        }

        return locations
    }

    fun getTourLocations(tourId: String): List<LocationDetail> {
        val locations = mutableListOf<LocationDetail>()
        val db = readableDatabase

        try {
            val cursor = db.rawQuery(
                """
                SELECT l.location_id, l.name_en, l.address,
                       dl.scene_notes, dl.car_travel_min, dl.order_in_trip
                FROM locations l
                INNER JOIN drama_locations dl ON l.location_id = dl.location_id
                WHERE dl.drama_id = ?
                ORDER BY dl.order_in_trip
                """,
                arrayOf(tourId)
            )

            while (cursor.moveToNext()) {
                locations.add(
                    LocationDetail(
                        id = cursor.getString(0),
                        name = cursor.getString(1),
                        address = cursor.getString(2),
                        imageUrl = "",
                        description = cursor.getString(3),
                        travelTimeFromPrevious = cursor.getInt(4)
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading tour locations for $tourId", e)
        } finally {
            db.close()
        }

        return locations
    }

    // Utility Methods
    private fun extractCityFromAddress(address: String?): String {
        return address?.split(",")?.lastOrNull()?.trim()?.split(" ")?.firstOrNull() ?: "Thailand"
    }
}