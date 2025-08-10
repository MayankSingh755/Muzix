package com.ionic.muzix.screens

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ionic.muzix.data.Muzix
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ionic.muzix.R
import com.ionic.muzix.utils.MiniPlayer
import com.ionic.muzix.utils.SearchBar
import com.ionic.muzix.utils.SearchSuggestions
import com.ionic.muzix.ui.theme.MuzixTheme
import com.ionic.muzix.utils.MuzixList
import com.ionic.muzix.utils.SearchManager
import com.ionic.muzix.utils.SearchUtils
import com.ionic.muzix.utils.getMuzix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


@SuppressLint("PermissionLaunchedDuringComposition")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MuzixListScreen(
    onMuzixClick: (music: List<Muzix>, position: Int) -> Unit,
    onMiniPlayerExpand: () -> Unit = {}
) {
    val context = LocalContext.current
    val searchManager = remember { SearchManager.getInstance(context) }

    // Music data states
    val muzixState = remember { mutableStateOf<List<Muzix>>(emptyList()) }
    val filteredMuzixState = remember { mutableStateOf<List<Muzix>>(emptyList()) }

    // Search states
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val recentSearches by searchManager.recentSearches.collectAsState()

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val storagePermissionState = rememberPermissionState(storagePermission)

    val notificationPermissionState =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        } else null

    // Request permissions once on first composition
    LaunchedEffect(Unit) {
        storagePermissionState.launchPermissionRequest()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionState?.launchPermissionRequest()
        }
    }

    // Load music when permission granted
    LaunchedEffect(storagePermissionState.status) {
        if (storagePermissionState.status.isGranted) {
            val loadedMuzix = withContext(Dispatchers.IO) { getMuzix(context) }
            muzixState.value = loadedMuzix
            filteredMuzixState.value = loadedMuzix
        }
    }

    // Handle search query changes
    LaunchedEffect(searchQuery) {
        delay(300)

        if (searchQuery.isBlank()) {
            filteredMuzixState.value = muzixState.value
            searchSuggestions = emptyList()
        } else {
            // Filter muzix list
            filteredMuzixState.value = SearchUtils.filterMuzix(muzixState.value, searchQuery)

            // Get suggestions
            searchSuggestions = SearchUtils.getSearchSuggestions(muzixState.value, searchQuery, 5)
        }
    }

    // Search functions
    fun performSearch(query: String) {
        if (query.isNotBlank()) {
            searchManager.addRecentSearch(query)
            searchQuery = query
            isSearchExpanded = false
        }
    }

    fun handleSuggestionClick(suggestion: String) {
        performSearch(suggestion)
    }

    Scaffold(
        bottomBar = {
            MiniPlayer(
                onExpandClick = onMiniPlayerExpand,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }
    ) { innerPadding ->
        MuzixTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.MuzixListScreenWelcome),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.displaySmall
                )

                Spacer(Modifier.height(16.dp))

                // Search Bar
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { performSearch(it) },
                    isExpanded = isSearchExpanded,
                    onExpandedChange = { isSearchExpanded = it },
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (isSearchExpanded && (searchSuggestions.isNotEmpty() || recentSearches.isNotEmpty())) {
                    SearchSuggestions(
                        suggestions = searchSuggestions,
                        recentSearches = if (searchQuery.isBlank()) recentSearches else emptyList(),
                        onSuggestionClick = { handleSuggestionClick(it) },
                        onClearRecentSearches = { searchManager.clearRecentSearches() },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                when {
                    storagePermissionState.status.isGranted -> {
                        if (searchQuery.isNotBlank() && filteredMuzixState.value.isEmpty()) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
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
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        } else {
                            if (searchQuery.isNotBlank()) {
                                Text(
                                    text = "${filteredMuzixState.value.size} result${if (filteredMuzixState.value.size != 1) "s" else ""} found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            MuzixList(
                                muzix = filteredMuzixState.value,
                                onMuzixClick = { _, position ->
                                    val selectedMuzix = filteredMuzixState.value[position]
                                    val originalPosition = muzixState.value.indexOfFirst {
                                        it.data == selectedMuzix.data
                                    }
                                    onMuzixClick(muzixState.value, if (originalPosition >= 0) originalPosition else position)
                                },
                                modifier = Modifier.weight(1f),
                                searchQuery = searchQuery
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.storage_permission_required_to_load_your_music),
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun MuzixListScreenPreview() {
    MuzixTheme {
        MuzixListScreen(
            onMuzixClick = { _, _ -> },
            onMiniPlayerExpand = {}
        )
    }
}