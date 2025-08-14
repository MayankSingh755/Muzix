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
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.ionic.muzix.R
import com.ionic.muzix.utils.MiniPlayer
import com.ionic.muzix.utils.searchfeatures.SearchBar
import com.ionic.muzix.utils.searchfeatures.SearchSuggestions
import com.ionic.muzix.ui.theme.MuzixTheme
import com.ionic.muzix.utils.MuzixService
import com.ionic.muzix.utils.searchfeatures.SearchManager
import com.ionic.muzix.utils.searchfeatures.SearchUtils
import com.ionic.muzix.utils.getMuzix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

// Sealed class to represent different types of list items
sealed class ListItem {
    data class HeaderItem(val letter: String) : ListItem()
    data class MuzixItem(val muzix: Muzix, val originalIndex: Int) : ListItem()
}

@Composable
private fun MuzixItem(
    muzix: Muzix,
    onClick: () -> Unit,
    searchQuery: String = "",
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0x00FFFFFF)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            val albumArtUri = ContentUris.withAppendedId(
                "content://media/external/audio/albumart".toUri(),
                muzix.albumId
            )

            AsyncImage(
                model = albumArtUri,
                contentDescription = "Album Art",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.baseline_music_note_24),
                placeholder = painterResource(R.drawable.baseline_music_note_24)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = getHighlightedText(muzix.title ?: "Unknown", searchQuery),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = getHighlightedText(muzix.artist, searchQuery),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

@Composable
private fun getHighlightedText(
    text: String,
    searchQuery: String
): AnnotatedString {
    if (searchQuery.isBlank() || text.isBlank()) {
        return AnnotatedString(text)
    }

    val lowercaseText = text.lowercase()
    val lowercaseQuery = searchQuery.lowercase().trim()
    val startIndex = lowercaseText.indexOf(lowercaseQuery)

    return if (startIndex >= 0) {
        buildAnnotatedString {
            append(text.substring(0, startIndex))

            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append(text.substring(startIndex, startIndex + lowercaseQuery.length))
            }

            append(text.substring(startIndex + lowercaseQuery.length))
        }
    } else {
        AnnotatedString(text)
    }
}

// Enum for different sort options
enum class SortOption(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ARTIST_ASC("Artist (A-Z)"),
    ARTIST_DESC("Artist (Z-A)"),
    ALPHABETICAL_GROUPED("Alphabetical (Grouped)"),
    DEFAULT("Default")
}

// Function to get the first letter for grouping
fun getGroupingLetter(title: String): String {
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) return "#"

    // Find the first alphabetical character
    val firstAlphaChar = cleanTitle.firstOrNull { it.isLetter() }

    return if (firstAlphaChar != null) {
        firstAlphaChar.uppercaseChar().toString()
    } else {
        "#" // For numbers and special characters
    }
}

// Function to create grouped list items
fun createGroupedListItems(muzixList: List<Muzix>, sortOption: SortOption): List<ListItem> {
    if (muzixList.isEmpty()) return emptyList()

    // Determine grouping function
    val groupByFunc: (Muzix) -> String = when (sortOption) {
        SortOption.ARTIST_ASC, SortOption.ARTIST_DESC -> {
            { it.artist }
        }

        else -> {
            { it.title ?: "" }
        }
    }

    // Group by letter
    val groups = muzixList.groupBy { getGroupingLetter(groupByFunc(it)) }

    // Determine if descending order for groups
    val isDesc = sortOption in listOf(SortOption.TITLE_DESC, SortOption.ARTIST_DESC)

    // Sort group keys
    val sortedGroupKeys = if (isDesc) {
        groups.keys.sortedDescending()
    } else {
        groups.keys.sorted()
    }

    // Determine comparator for items within groups
    val itemComparator: Comparator<Muzix> = when (sortOption) {
        SortOption.TITLE_ASC, SortOption.ALPHABETICAL_GROUPED -> Comparator { a, b ->
            (a.title?.lowercase() ?: "").compareTo(b.title?.lowercase() ?: "")
        }

        SortOption.TITLE_DESC -> Comparator { a, b ->
            (b.title?.lowercase() ?: "").compareTo(a.title?.lowercase() ?: "")
        }

        SortOption.ARTIST_ASC -> Comparator { a, b ->
            a.artist.lowercase().compareTo(b.artist.lowercase())
        }

        SortOption.ARTIST_DESC -> Comparator { a, b ->
            b.artist.lowercase().compareTo(a.artist.lowercase())
        }

        SortOption.DEFAULT -> Comparator { a, b ->
            muzixList.indexOf(a) - muzixList.indexOf(b)
        }
    }

    val groupedItems = mutableListOf<ListItem>()

    sortedGroupKeys.forEach { letter ->
        val groupItems = groups[letter] ?: emptyList()
        val sortedGroupItems = groupItems.sortedWith(itemComparator)

        if (sortedGroupItems.isNotEmpty()) {
            groupedItems.add(ListItem.HeaderItem(letter))
            sortedGroupItems.forEach { muzix ->
                val originalIndex = muzixList.indexOf(muzix)
                groupedItems.add(ListItem.MuzixItem(muzix, originalIndex))
            }
        }
    }

    return groupedItems
}

// Function to filter grouped list items
fun filterGroupedItems(groupedItems: List<ListItem>, searchQuery: String): List<ListItem> {
    if (searchQuery.isBlank()) return groupedItems

    val filteredItems = mutableListOf<ListItem>()
    var currentHeader: ListItem.HeaderItem? = null

    groupedItems.forEach { item ->
        when (item) {
            is ListItem.HeaderItem -> {
                currentHeader = item
            }

            is ListItem.MuzixItem -> {
                // Check if this muzix item matches the search query
                val matchesSearch =
                    item.muzix.title?.contains(searchQuery, ignoreCase = true) == true ||
                            item.muzix.artist.contains(searchQuery, ignoreCase = true)

                if (matchesSearch) {
                    // Add header if not already added
                    currentHeader?.let { header ->
                        if (filteredItems.lastOrNull() !is ListItem.HeaderItem ||
                            (filteredItems.lastOrNull() as ListItem.HeaderItem).letter != header.letter
                        ) {
                            filteredItems.add(header)
                        }
                    }
                    filteredItems.add(item)
                }
            }
        }
    }

    return filteredItems
}

@Composable
fun SectionHeader(letter: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Composable
fun DraggableScrollbar(
    listState: LazyListState,
    config: ScrollbarConfig,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    // Calculate scrollbar dimensions and position
    val firstVisibleIndex = listState.firstVisibleItemIndex
    val firstVisibleOffset = listState.firstVisibleItemScrollOffset
    val totalItems = listState.layoutInfo.totalItemsCount
    val visibleItems = listState.layoutInfo.visibleItemsInfo.size

    if (totalItems <= visibleItems) return // No scrollbar needed

    val scrollProgress = if (totalItems > 0) {
        (firstVisibleIndex + firstVisibleOffset.toFloat() /
                (listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1)) /
                (totalItems - visibleItems).coerceAtLeast(1)
    } else 0f

    val thumbHeight = max(
        (visibleItems.toFloat() / totalItems) * config.trackHeight.value,
        config.minThumbHeight.value
    )
    val thumbPosition = scrollProgress * (config.trackHeight.value - thumbHeight)

    val alpha by animateFloatAsState(
        targetValue = if (isDragging) 1f else if (listState.isScrollInProgress) 0.8f else 0.4f,
        label = "scrollbar_alpha"
    )

    Box(
        modifier = modifier
            .width(config.trackWidth)
            .height(config.trackHeight)
            .padding(end = 4.dp)
            .alpha(alpha)
    ) {
        // Track
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(config.trackWidth / 2))
                .background(config.trackColor)
        )

        // Thumb
        Box(
            modifier = Modifier
                .width(config.thumbWidth)
                .height(thumbHeight.dp)
                .offset(
                    x = (config.trackWidth - config.thumbWidth) / 2,
                    y = thumbPosition.dp
                )
                .clip(RoundedCornerShape(config.thumbWidth / 2))
                .background(if (isDragging) config.thumbColorPressed else config.thumbColor)
                .draggableScrollbar(
                    listState = listState,
                    totalItems = totalItems,
                    visibleItems = visibleItems,
                    trackHeight = config.trackHeight.value,
                    thumbHeight = thumbHeight,
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false }
                )
        )
    }
}

private fun Modifier.draggableScrollbar(
    listState: LazyListState,
    totalItems: Int,
    visibleItems: Int,
    trackHeight: Float,
    thumbHeight: Float,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { onDragStart() },
            onDragEnd = { onDragEnd() },
            onDrag = { change, dragAmount ->
                change.consume()
                coroutineScope.launch {
                    val dragDelta = with(density) { dragAmount.y.toDp().value }
                    val maxScroll = trackHeight - thumbHeight
                    val scrollDelta = dragDelta / maxScroll
                    val targetIndex = (
                            (listState.firstVisibleItemIndex +
                                    listState.firstVisibleItemScrollOffset.toFloat() /
                                    (listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size
                                        ?: 1)) +
                                    scrollDelta * (totalItems - visibleItems)
                            ).coerceIn(0f, (totalItems - visibleItems).toFloat())

                    listState.scrollToItem(targetIndex.toInt())
                }
            }
        )
    }
}

data class ScrollbarConfig(
    val trackWidth: Dp = 12.dp,
    val trackHeight: Dp = 300.dp,
    val thumbWidth: Dp = 8.dp,
    val minThumbHeight: Dp = 48.dp,
    val trackColor: Color = Color.Gray.copy(alpha = 0.3f),
    val thumbColor: Color = Color.Gray.copy(alpha = 0.7f),
    val thumbColorPressed: Color = Color.Gray.copy(alpha = 0.9f)
)

@Composable
fun GroupedMuzixList(
    groupedItems: List<ListItem>,
    onMuzixClick: (muzix: Muzix, originalIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {
    val scrollState = rememberLazyListState()

    Box(modifier = Modifier) {
        LazyColumn(
            modifier = modifier,
            state = scrollState,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(groupedItems) { item ->
                when (item) {
                    is ListItem.HeaderItem -> SectionHeader(letter = item.letter)
                    is ListItem.MuzixItem -> MuzixItem(
                        muzix = item.muzix,
                        onClick = { onMuzixClick(item.muzix, item.originalIndex) },
                        searchQuery = searchQuery
                    )
                }
            }
        }

        DraggableScrollbar(
            listState = scrollState,
            config = ScrollbarConfig(),
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}


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
    val groupedItemsState = remember { mutableStateOf<List<ListItem>>(emptyList()) }
    val filteredItemsState = remember { mutableStateOf<List<ListItem>>(emptyList()) }

    // Search states
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val recentSearches by searchManager.recentSearches.collectAsState()

    // Sort states
    var currentSortOption by remember { mutableStateOf(SortOption.ALPHABETICAL_GROUPED) }
    var showSortMenu by remember { mutableStateOf(false) }

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
            } catch (_: Exception) {
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

            // Create grouped items
            groupedItemsState.value = createGroupedListItems(loadedMuzix, currentSortOption)
            filteredItemsState.value = groupedItemsState.value
        }
    }

    // Handle sort option changes
    LaunchedEffect(currentSortOption) {
        if (muzixState.value.isNotEmpty()) {
            groupedItemsState.value = createGroupedListItems(muzixState.value, currentSortOption)

            // Re-apply search filter
            filteredItemsState.value = if (searchQuery.isBlank()) {
                groupedItemsState.value
            } else {
                filterGroupedItems(groupedItemsState.value, searchQuery)
            }
        }
    }

    // Handle search query changes
    LaunchedEffect(searchQuery) {
        delay(300)

        if (searchQuery.isBlank()) {
            filteredItemsState.value = groupedItemsState.value
            searchSuggestions = emptyList()
        } else {
            // Filter the grouped items
            filteredItemsState.value = filterGroupedItems(groupedItemsState.value, searchQuery)

            // Get suggestions using the original list
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

    // Sort functions
    fun handleSortOptionSelect(sortOption: SortOption) {
        currentSortOption = sortOption
        showSortMenu = false
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
            if (currentMuzix == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            } else {
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

                    // Search Bar and Sort Button Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Bar with modified styling for better visibility
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { performSearch(it) },
                            isExpanded = isSearchExpanded,
                            onExpandedChange = { isSearchExpanded = it },
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    Color.Black.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        )

                        // Sort Button
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier
                                    .background(
                                        Color.Black.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .size(56.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_sort_24),
                                    contentDescription = "Sort",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(
                                    Color.Black.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            ) {
                                SortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.displayName,
                                                color = if (currentSortOption == option)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    Color.White
                                            )
                                        },
                                        onClick = { handleSortOptionSelect(option) }
                                    )
                                }
                            }
                        }
                    }

                    // Current sort indicator
                    if (currentSortOption != SortOption.DEFAULT) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sorted by: ${currentSortOption.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

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
                            if (searchQuery.isNotBlank() && filteredItemsState.value.isEmpty()) {
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
                                    val resultCount =
                                        filteredItemsState.value.count { it is ListItem.MuzixItem }
                                    Text(
                                        text = "$resultCount result${if (resultCount != 1) "s" else ""} found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                GroupedMuzixList(
                                    groupedItems = filteredItemsState.value,
                                    onMuzixClick = { muzix, originalIndex ->
                                        onMuzixClick(muzixState.value, originalIndex)
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