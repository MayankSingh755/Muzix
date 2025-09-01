package com.ionic.muzix.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ionic.muzix.R
import com.ionic.muzix.utils.searchfeatures.SearchBar
import com.ionic.muzix.utils.searchfeatures.SearchSuggestions
import com.ionic.muzix.data.model.Muzix
import com.ionic.muzix.ui.theme.MuzixTheme
import com.ionic.muzix.utils.MuzixList
import com.ionic.muzix.utils.searchfeatures.SearchManager
import com.ionic.muzix.utils.searchfeatures.SearchUtils
import com.ionic.muzix.data.model.SharedMuzixData
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
class SearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val muzixList = intent.getParcelableArrayListExtra<Muzix>("muzixList") ?: emptyList()

        setContent {
            MuzixTheme {
                SearchScreen(
                    muzixList = muzixList,
                    onBackClick = { finish() },
                    onMuzixClick = { music, position ->
                        // Store data and launch player
                        SharedMuzixData.setData(music, position)

                        val intent = Intent(this@SearchActivity, MuzixPlayerActivity::class.java)
                        intent.putParcelableArrayListExtra("muzixList", ArrayList(music))
                        intent.putExtra("initialIndex", position)
                        intent.putExtra("shouldStartPlayback", true)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreen(
    muzixList: List<Muzix>,
    onBackClick: () -> Unit,
    onMuzixClick: (music: List<Muzix>, position: Int) -> Unit
) {
    val context = LocalContext.current
    val searchManager = remember { SearchManager.getInstance(context) }

    var searchQuery by remember { mutableStateOf("") }
    var filteredMuzix by remember { mutableStateOf<List<Muzix>>(emptyList()) }
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSearchFocused by remember { mutableStateOf(true) }

    val recentSearches by searchManager.recentSearches.collectAsState()

    // Handle search
    LaunchedEffect(searchQuery) {
        delay(300)

        if (searchQuery.isBlank()) {
            filteredMuzix = emptyList()
            searchSuggestions = emptyList()
        } else {
            filteredMuzix = SearchUtils.filterMuzix(muzixList, searchQuery)
            searchSuggestions = SearchUtils.getSearchSuggestions(muzixList, searchQuery, 5)
        }
    }

    fun performSearch(query: String) {
        if (query.isNotBlank()) {
            searchManager.addRecentSearch(query)
            searchQuery = query
            isSearchFocused = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_music)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { performSearch(it) },
                isExpanded = isSearchFocused,
                onExpandedChange = { isSearchFocused = it },
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Search Suggestions
            if (isSearchFocused && (searchSuggestions.isNotEmpty() || recentSearches.isNotEmpty())) {
                SearchSuggestions(
                    suggestions = searchSuggestions,
                    recentSearches = if (searchQuery.isBlank()) recentSearches else emptyList(),
                    onSuggestionClick = { performSearch(it) },
                    onClearRecentSearches = { searchManager.clearRecentSearches() },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Results
            when {
                searchQuery.isBlank() -> {
                    // Show recent searches or empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start typing to search your music",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                filteredMuzix.isEmpty() -> {
                    // No results
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No results found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try searching with different keywords",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    // Show results
                    Text(
                        text = "${filteredMuzix.size} result${if (filteredMuzix.size != 1) "s" else ""} found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    MuzixList(
                        muzix = filteredMuzix,
                        onMuzixClick = { _, position ->
                            val selectedMuzix = filteredMuzix[position]
                            val originalPosition = muzixList.indexOfFirst {
                                it.data == selectedMuzix.data
                            }
                            onMuzixClick(muzixList, if (originalPosition >= 0) originalPosition else position)
                        },
                        searchQuery = searchQuery,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}