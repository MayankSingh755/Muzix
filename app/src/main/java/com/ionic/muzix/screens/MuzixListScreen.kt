package com.ionic.muzix.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.ionic.muzix.R
import com.ionic.muzix.utils.MiniPlayer
import com.ionic.muzix.utils.searchfeatures.SearchBar
import com.ionic.muzix.utils.searchfeatures.SearchSuggestions
import com.ionic.muzix.ui.theme.MuzixTheme
import com.ionic.muzix.utils.MuzixList
import com.ionic.muzix.utils.MuzixService
import com.ionic.muzix.utils.searchfeatures.SearchManager
import com.ionic.muzix.utils.searchfeatures.SearchUtils
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

    // Music service states for background
    var muzixService by remember { mutableStateOf<MuzixService?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentMuzix by remember { mutableStateOf<Muzix?>(null) }

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

    // Service connection for background updates
    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                muzixService = (service as MuzixService.MusicBinder).getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                muzixService = null
            }
        }
    }

    // Bind to service
    LaunchedEffect(Unit) {
        val intent = Intent(context, MuzixService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    // Cleanup service connection
    DisposableEffect(Unit) {
        onDispose {
            try {
                context.unbindService(connection)
            } catch (e: Exception) {
                // Service might already be unbound
            }
        }
    }

    // Poll for music service updates
    LaunchedEffect(muzixService) {
        while (true) {
            muzixService?.let { service ->
                isPlaying = service.exoPlayer.isPlaying
                currentMuzix = service.getCurrentMuzix()
            }
            delay(500) // Less frequent updates for list screen
        }
    }

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

    // Dynamic background based on playing state
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background
        if (isPlaying && currentMuzix != null) {
            // Show album art as blurred background when music is playing
            val albumArtUri = ContentUris.withAppendedId(
                "content://media/external/audio/albumart".toUri(),
                currentMuzix!!.albumId
            )

            AsyncImage(
                model = albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.baseline_music_note_24),
                placeholder = painterResource(R.drawable.baseline_music_note_24)
            )

            // Dark overlay for better text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
        } else {
            // Black background when no music is playing
            if (currentMuzix == null){
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
            else{
                val albumArtUri = ContentUris.withAppendedId(
                    "content://media/external/audio/albumart".toUri(),
                    currentMuzix!!.albumId
                )

                AsyncImage(
                    model = albumArtUri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(20.dp),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.baseline_music_note_24),
                    placeholder = painterResource(R.drawable.baseline_music_note_24)
                )

                // Dark overlay for better text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                )
            }

        }

        // Main content
        Scaffold(
            containerColor = Color.Transparent, // Make scaffold transparent
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
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White // Ensure text is visible on dynamic background
                    )

                    Spacer(Modifier.height(16.dp))

                    // Search Bar with modified styling for better visibility
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onSearch = { performSearch(it) },
                        isExpanded = isSearchExpanded,
                        onExpandedChange = { isSearchExpanded = it },
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .background(
                                Color.Black.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    )

                    if (isSearchExpanded && (searchSuggestions.isNotEmpty() || recentSearches.isNotEmpty())) {
                        SearchSuggestions(
                            suggestions = searchSuggestions,
                            recentSearches = if (searchQuery.isBlank()) recentSearches else emptyList(),
                            onSuggestionClick = { handleSuggestionClick(it) },
                            onClearRecentSearches = { searchManager.clearRecentSearches() },
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.7f),
                                    shape = RoundedCornerShape(12.dp)
                                )
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
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Try searching with different keywords",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            } else {
                                if (searchQuery.isNotBlank()) {
                                    Text(
                                        text = "${filteredMuzixState.value.size} result${if (filteredMuzixState.value.size != 1) "s" else ""} found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.8f),
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
                                color = Color.White
                            )
                        }
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