package com.ionic.muzix.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for handling search functionality and recent searches
 */
class SearchManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _recentSearches = MutableStateFlow<List<String>>(loadRecentSearches())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    companion object {
        private const val PREFS_NAME = "muzix_search_prefs"
        private const val KEY_RECENT_SEARCHES = "recent_searches"
        private const val MAX_RECENT_SEARCHES = 10

        @Volatile
        private var INSTANCE: SearchManager? = null

        fun getInstance(context: Context): SearchManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SearchManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Add a search query to recent searches
     */
    fun addRecentSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return

        val currentSearches = _recentSearches.value.toMutableList()

        // Remove if already exists to avoid duplicates
        currentSearches.remove(trimmedQuery)

        // Add to the beginning
        currentSearches.add(0, trimmedQuery)

        // Keep only the most recent searches
        val updatedSearches = currentSearches.take(MAX_RECENT_SEARCHES)

        // Update state and save to preferences
        _recentSearches.value = updatedSearches
        saveRecentSearches(updatedSearches)
    }

    /**
     * Clear all recent searches
     */
    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        prefs.edit().remove(KEY_RECENT_SEARCHES).apply()
    }

    /**
     * Remove a specific recent search
     */
    fun removeRecentSearch(query: String) {
        val currentSearches = _recentSearches.value.toMutableList()
        currentSearches.remove(query)
        _recentSearches.value = currentSearches
        saveRecentSearches(currentSearches)
    }

    private fun loadRecentSearches(): List<String> {
        val searchesString = prefs.getString(KEY_RECENT_SEARCHES, "") ?: ""
        return if (searchesString.isNotBlank()) {
            searchesString.split(",").filter { it.isNotBlank() }
        } else {
            emptyList()
        }
    }

    private fun saveRecentSearches(searches: List<String>) {
        val searchesString = searches.joinToString(",")
        prefs.edit().putString(KEY_RECENT_SEARCHES, searchesString).apply()
    }
}