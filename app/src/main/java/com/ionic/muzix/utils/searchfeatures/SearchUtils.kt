package com.ionic.muzix.utils.searchfeatures

import com.ionic.muzix.data.Muzix

/**
 * Utility object for search functionality
 */
object SearchUtils {

    /**
     * Filters the muzix list based on search query
     * Searches in title, artist, album, and genre
     */
    fun filterMuzix(
        muzixList: List<Muzix>,
        query: String
    ): List<Muzix> {
        if (query.isBlank()) return muzixList

        val searchQuery = query.lowercase().trim()

        return muzixList.filter { muzix ->
            // Search in title
            muzix.title?.lowercase()?.contains(searchQuery) == true ||
                    // Search in artist
                    muzix.artist.lowercase().contains(searchQuery) ||
                    // Search in album (if available)
                    muzix.title?.lowercase()?.contains(searchQuery) == true
            // Add more fields if needed
            // muzix.genre?.lowercase()?.contains(searchQuery) == true
        }
    }

    /**
     * Get search suggestions based on partial query
     */
    fun getSearchSuggestions(
        muzixList: List<Muzix>,
        query: String,
        maxSuggestions: Int = 5
    ): List<String> {
        if (query.isBlank()) return emptyList()

        val searchQuery = query.lowercase().trim()
        val suggestions = mutableSetOf<String>()

        muzixList.forEach { muzix ->
            // Add matching titles
            muzix.title?.let { title ->
                if (title.lowercase().contains(searchQuery)) {
                    suggestions.add(title)
                }
            }

            // Add matching artists
            if (muzix.artist.lowercase().contains(searchQuery)) {
                suggestions.add(muzix.artist)
            }

            // Add matching albums
            muzix.title?.let { album ->
                if (album.lowercase().contains(searchQuery)) {
                    suggestions.add(album)
                }
            }
        }

        return suggestions.take(maxSuggestions)
    }


}