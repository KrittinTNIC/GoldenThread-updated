package com.example.finalproject.util

class SmartGenreExpander {

    // Define genre relationships
    private val genreRelationships = mapOf(
        "Romance" to setOf("Romantic Comedy", "Drama"),
        "Comedy" to setOf("Romantic Comedy", "Sitcom"),
        "Drama" to setOf("Melodrama", "Family", "Romance"),
        "Action" to setOf("Thriller", "Adventure"),
        "Thriller" to setOf("Mystery", "Crime"),
        "Fantasy" to setOf("Adventure", "Supernatural"),
        "Horror" to setOf("Thriller", "Mystery")
    )

    fun expandUserPreferences(userGenres: Set<String>): Set<String> {
        val expandedGenres = mutableSetOf<String>()

        // Add the original user preferences
        expandedGenres.addAll(userGenres)

        // Add related genres
        userGenres.forEach { genre ->
            val relatedGenres = genreRelationships[genre] ?: emptySet()
            expandedGenres.addAll(relatedGenres)
        }

        return expandedGenres
    }
}