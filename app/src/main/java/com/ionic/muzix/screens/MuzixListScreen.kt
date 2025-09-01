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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.ionic.muzix.activity.PlaylistDetailActivity
import com.ionic.muzix.data.MyApplication
import com.ionic.muzix.utils.PlaylistAddDialog
import com.ionic.muzix.data.Playlist

sealed class ListItem {
    data class HeaderItem(val letter: String) : ListItem()
    data class MuzixItem(val muzix: Muzix, val originalIndex: Int) : ListItem()
}

sealed class AlbumItem {
    data class HeaderItem(val letter: String) : AlbumItem()
    data class AlbumGroupItem(
        val albumName: String,
        val artist: String,
        val albumId: Long,
        val tracks: List<Muzix>
    ) : AlbumItem()
}

sealed class ArtistItem {
    data class HeaderItem(val letter: String) : ArtistItem()
    data class ArtistGroupItem(val artistName: String, val tracks: List<Muzix>) : ArtistItem()
}

@Composable
private fun MuzixItem(
    muzix: Muzix,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    searchQuery: String = "",
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            AsyncImage(
                model = ContentUris.withAppendedId(
                    stringResource(R.string.album_art_uri).toUri(),
                    muzix.albumId
                ),
                contentDescription = stringResource(R.string.albumArt),
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.baseline_music_note_24),
                placeholder = painterResource(R.drawable.baseline_music_note_24)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
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
private fun ArtistItem(
    artistName: String,
    tracks: List<Muzix>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Use first track's album art as artist image
            AsyncImage(
                model = ContentUris.withAppendedId(
                    stringResource(R.string.album_art_uri).toUri(),
                    tracks.firstOrNull()?.albumId ?: 0L
                ),
                contentDescription = "Artist Image",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.Gray),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.baseline_music_note_24),
                placeholder = painterResource(R.drawable.baseline_music_note_24)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${tracks.size} ${if (tracks.size == 1) "track" else "tracks"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun getHighlightedText(text: String, searchQuery: String): AnnotatedString {
    if (searchQuery.isBlank() || text.isBlank()) return AnnotatedString(text)

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

// Enum for sort options
enum class SortOption(val displayName: String) {
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)"),
    ARTIST_ASC("Artist (A-Z)"),
    ARTIST_DESC("Artist (Z-A)"),
    ALPHABETICAL_GROUPED("Alphabetical (Grouped)"),
    DEFAULT("Default")
}

fun getGroupingLetter(title: String): String {
    val cleanTitle = title.trim()
    if (cleanTitle.isEmpty()) return "#"
    val firstAlphaChar = cleanTitle.firstOrNull { it.isLetter() }
    return firstAlphaChar?.uppercaseChar()?.toString() ?: "#"
}

fun createGroupedListItems(muzixList: List<Muzix>, sortOption: SortOption): List<ListItem> {
    if (muzixList.isEmpty()) return emptyList()

    val groupByFunc: (Muzix) -> String = when (sortOption) {
        SortOption.ARTIST_ASC, SortOption.ARTIST_DESC -> {
            { it.artist }
        }

        else -> {
            { it.title ?: "" }
        }
    }

    val groups = muzixList.groupBy { getGroupingLetter(groupByFunc(it)) }
    val isDesc = sortOption in listOf(SortOption.TITLE_DESC, SortOption.ARTIST_DESC)
    val sortedGroupKeys = if (isDesc) groups.keys.sortedDescending() else groups.keys.sorted()

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
                groupedItems.add(ListItem.MuzixItem(muzix, muzixList.indexOf(muzix)))
            }
        }
    }
    return groupedItems
}

fun createGroupedAlbumItems(muzixList: List<Muzix>): List<AlbumItem> {
    if (muzixList.isEmpty()) return emptyList()

    val albumGroups = muzixList.groupBy { "${it.albumId}_${it.artist}" }
    val sortedGroups = albumGroups.toList().sortedBy { it.first.split("_")[0] }

    val groupedByLetter = sortedGroups.groupBy {
        getGroupingLetter(it.first.split("_")[0])
    }

    val groupedItems = mutableListOf<AlbumItem>()
    groupedByLetter.keys.sorted().forEach { letter ->
        val albumsInGroup = groupedByLetter[letter] ?: emptyList()
        if (albumsInGroup.isNotEmpty()) {
            groupedItems.add(AlbumItem.HeaderItem(letter))
            albumsInGroup.forEach { (albumKey, tracks) ->
                val albumName = albumKey.split("_")[0]
                val artist = albumKey.split("_").getOrNull(1) ?: "Unknown Artist"
                val albumId = tracks.firstOrNull()?.albumId ?: 0L
                groupedItems.add(AlbumItem.AlbumGroupItem(albumName, artist, albumId, tracks))
            }
        }
    }
    return groupedItems
}

fun createGroupedArtistItems(muzixList: List<Muzix>): List<ArtistItem> {
    if (muzixList.isEmpty()) return emptyList()

    val artistGroups = muzixList.groupBy { it.artist }
    val sortedGroups = artistGroups.toList().sortedBy { it.first }

    val groupedByLetter = sortedGroups.groupBy {
        getGroupingLetter(it.first)
    }

    val groupedItems = mutableListOf<ArtistItem>()
    groupedByLetter.keys.sorted().forEach { letter ->
        val artistsInGroup = groupedByLetter[letter] ?: emptyList()
        if (artistsInGroup.isNotEmpty()) {
            groupedItems.add(ArtistItem.HeaderItem(letter))
            artistsInGroup.forEach { (artistName, tracks) ->
                groupedItems.add(ArtistItem.ArtistGroupItem(artistName, tracks))
            }
        }
    }
    return groupedItems
}

fun filterGroupedItems(groupedItems: List<ListItem>, searchQuery: String): List<ListItem> {
    if (searchQuery.isBlank()) return groupedItems

    val filteredItems = mutableListOf<ListItem>()
    var currentHeader: ListItem.HeaderItem? = null

    groupedItems.forEach { item ->
        when (item) {
            is ListItem.HeaderItem -> currentHeader = item
            is ListItem.MuzixItem -> {
                val matchesSearch =
                    item.muzix.title?.contains(searchQuery, ignoreCase = true) == true ||
                            item.muzix.artist.contains(searchQuery, ignoreCase = true)
                if (matchesSearch) {
                    currentHeader?.let { header ->
                        if (filteredItems.lastOrNull() !is ListItem.HeaderItem ||
                            (filteredItems.lastOrNull() as? ListItem.HeaderItem)?.letter != header.letter
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
            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = letter,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
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
fun DraggableScrollbar(
    listState: LazyListState,
    config: ScrollbarConfig,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    val totalItems = listState.layoutInfo.totalItemsCount
    val visibleItems = listState.layoutInfo.visibleItemsInfo.size
    if (totalItems <= visibleItems) return

    val scrollProgress = if (totalItems > 0) {
        (listState.firstVisibleItemIndex + listState.firstVisibleItemScrollOffset.toFloat() /
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(config.trackWidth / 2))
                .background(config.trackColor)
        )
        Box(
            modifier = Modifier
                .width(config.thumbWidth)
                .height(thumbHeight.dp)
                .offset(x = (config.trackWidth - config.thumbWidth) / 2, y = thumbPosition.dp)
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

@Composable
fun GroupedMuzixList(
    groupedItems: List<ListItem>,
    onMuzixClick: (Muzix, Int) -> Unit,
    onMuzixLongPress: (Muzix) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    scrollState: LazyListState = rememberLazyListState()
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                        onLongPress = { onMuzixLongPress(item.muzix) },
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

@Composable
fun GroupedArtistList(
    groupedItems: List<ArtistItem>,
    onArtistClick: (List<Muzix>) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: LazyListState = rememberLazyListState()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = modifier,
            state = scrollState,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(groupedItems) { item ->
                when (item) {
                    is ArtistItem.HeaderItem -> SectionHeader(letter = item.letter)
                    is ArtistItem.ArtistGroupItem -> ArtistItem(
                        artistName = item.artistName,
                        tracks = item.tracks,
                        onClick = { onArtistClick(item.tracks) }
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
@Composable
fun rememberScrollDirection(scrollState: LazyListState): State<Boolean> {
    val isScrollingUp = remember { mutableStateOf(true) }
    var lastIndex by remember { mutableStateOf(0) }
    var lastScrollOffset by remember { mutableStateOf(0) }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.firstVisibleItemIndex to scrollState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index == lastIndex) {
                    isScrollingUp.value = offset < lastScrollOffset
                } else {
                    isScrollingUp.value = index < lastIndex
                }
                lastIndex = index
                lastScrollOffset = offset
            }
    }
    return isScrollingUp
}
@SuppressLint("PermissionLaunchedDuringComposition")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun MuzixListScreen(
    onMuzixClick: (List<Muzix>, Int) -> Unit,
    onMiniPlayerExpand: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val dao = app.database.playlistDao()
    val searchManager = remember { SearchManager.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    val muzixState = remember { mutableStateOf<List<Muzix>>(emptyList()) }
    val groupedItemsState = remember { mutableStateOf<List<ListItem>>(emptyList()) }
    val filteredItemsState = remember { mutableStateOf<List<ListItem>>(emptyList()) }
    val groupedAlbumItemsState = remember { mutableStateOf<List<AlbumItem>>(emptyList()) }
    val groupedArtistItemsState = remember { mutableStateOf<List<ArtistItem>>(emptyList()) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val recentSearches by searchManager.recentSearches.collectAsState()
    var currentSortOption by remember { mutableStateOf(SortOption.ALPHABETICAL_GROUPED) }
    var showSortMenu by remember { mutableStateOf(false) }
    var muzixService by remember { mutableStateOf<MuzixService?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentMuzix by remember { mutableStateOf<Muzix?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAddDialogForMuzix by remember { mutableStateOf<Muzix?>(null) }

    val tracksScrollState = rememberLazyListState()
    val artistsScrollState = rememberLazyListState()

    val isScrollingUpTracks by rememberScrollDirection(tracksScrollState)
    val isScrollingUpArtists by rememberScrollDirection(artistsScrollState)

    val isScrollingUp = when (selectedTab) {
        0 -> isScrollingUpTracks
        1 -> isScrollingUpArtists
        else -> true
    }

    val isAtTop = when (selectedTab) {
        0 -> tracksScrollState.firstVisibleItemIndex == 0 &&
                tracksScrollState.firstVisibleItemScrollOffset < 50
        1 -> artistsScrollState.firstVisibleItemIndex == 0 &&
                artistsScrollState.firstVisibleItemScrollOffset < 50
        else -> true
    }

    val showTopElements = isScrollingUp || isAtTop

    val showFABs = selectedTab == 0 &&
            showTopElements &&
            muzixState.value.isNotEmpty()

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val storagePermissionState = rememberPermissionState(storagePermission)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

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

    LaunchedEffect(Unit) {
        context.bindService(
            Intent(context, MuzixService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    DisposableEffect(Unit) {
        onDispose { context.unbindService(connection) }
    }

    LaunchedEffect(muzixService) {
        while (true) {
            muzixService?.let { service ->
                isPlaying = service.exoPlayer.isPlaying
                currentMuzix = service.getCurrentMuzix()
            }
            delay(500)
        }
    }

    LaunchedEffect(Unit) {
        storagePermissionState.launchPermissionRequest()
        notificationPermissionState?.launchPermissionRequest()
    }

    LaunchedEffect(storagePermissionState.status) {
        if (storagePermissionState.status.isGranted) {
            val loadedMuzix = withContext(Dispatchers.IO) { getMuzix(context) }
            muzixState.value = loadedMuzix
            groupedItemsState.value = createGroupedListItems(loadedMuzix, currentSortOption)
            filteredItemsState.value = groupedItemsState.value
            groupedAlbumItemsState.value = createGroupedAlbumItems(loadedMuzix)
            groupedArtistItemsState.value = createGroupedArtistItems(loadedMuzix)
        }
    }

    LaunchedEffect(currentSortOption) {
        if (muzixState.value.isNotEmpty()) {
            groupedItemsState.value = createGroupedListItems(muzixState.value, currentSortOption)
            filteredItemsState.value = if (searchQuery.isBlank()) {
                groupedItemsState.value
            } else {
                filterGroupedItems(groupedItemsState.value, searchQuery)
            }
        }
    }

    LaunchedEffect(searchQuery) {
        delay(300)
        if (searchQuery.isBlank()) {
            filteredItemsState.value = groupedItemsState.value
            searchSuggestions = emptyList()
        } else {
            filteredItemsState.value = filterGroupedItems(groupedItemsState.value, searchQuery)
            searchSuggestions = SearchUtils.getSearchSuggestions(muzixState.value, searchQuery, 5)
        }
    }

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

    fun handleSortOptionSelect(sortOption: SortOption) {
        currentSortOption = sortOption
        showSortMenu = false
    }

    fun startShufflePlay() {
        val shuffledList = muzixState.value.shuffled()
        onMuzixClick(shuffledList, 0)
    }

    fun startPlayFromBeginning() {
        onMuzixClick(muzixState.value, 0)
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text(stringResource(R.string.create_new_playlist)) },
            text = {
                var name by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_name)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (name.isNotBlank()) {
                            coroutineScope.launch {
                                dao.insertPlaylist(Playlist(name = name))
                                showCreatePlaylistDialog = false
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.playlist_created), Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    })
                )
            },
            confirmButton = {
                TextButton(onClick = {
                }) { Text(stringResource(R.string.create)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreatePlaylistDialog = false
                }) { Text(stringResource(R.string.Cancel)) }
            }
        )
    }

    if (showAddDialogForMuzix != null) {
        PlaylistAddDialog(
            muzix = showAddDialogForMuzix!!,
            onDismiss = { showAddDialogForMuzix = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isPlaying && currentMuzix != null) {
            AsyncImage(
                model = ContentUris.withAppendedId(
                    stringResource(R.string.album_art_uri).toUri(),
                    currentMuzix!!.albumId
                ),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.baseline_music_note_24),
                placeholder = painterResource(R.drawable.baseline_music_note_24)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                MiniPlayer(
                    onExpandClick = onMiniPlayerExpand,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = showFABs,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Shuffle FAB
                        FloatingActionButton(
                            onClick = { startShufflePlay() },
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_shuffle_24),
                                contentDescription = "Shuffle Play",
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Play FAB
                        FloatingActionButton(
                            onClick = { startPlayFromBeginning() },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_play_arrow_24),
                                contentDescription = "Play All",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
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
                    // Animated header that hides on scroll
                    AnimatedVisibility(
                        visible = showTopElements,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.MuzixListScreenWelcome),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    val tabTitles = listOf(
                        stringResource(R.string.muzix),
                        stringResource(R.string.artists),
                        stringResource(R.string.playlists)
                    )

                    // Animated tab row that hides on scroll
                    AnimatedVisibility(
                        visible = showTopElements,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = { tabPositions ->
                                TabRowDefaults.Indicator(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[selectedTab])
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            divider = {}
                        ) {
                            tabTitles.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTab == index)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Search bar and sort button - only show for tracks tab
                    if (selectedTab == 0) {
                        AnimatedVisibility(
                            visible = showTopElements,
                            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                            RoundedCornerShape(12.dp)
                                        )
                                )

                                Box {
                                    IconButton(
                                        onClick = { showSortMenu = true },
                                        modifier = Modifier
                                            .background(
                                                Color.Black.copy(alpha = 0.3f),
                                                RoundedCornerShape(12.dp)
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
                                            RoundedCornerShape(8.dp)
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
                        }
                    }

                    if (currentSortOption != SortOption.DEFAULT && selectedTab == 0) {
                        AnimatedVisibility(
                            visible = showTopElements,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.sorted_by,
                                    currentSortOption.displayName
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    if (isSearchExpanded && selectedTab != 2 && (searchSuggestions.isNotEmpty() || recentSearches.isNotEmpty())) {
                        SearchSuggestions(
                            suggestions = searchSuggestions,
                            recentSearches = if (searchQuery.isBlank()) recentSearches else emptyList(),
                            onSuggestionClick = { handleSuggestionClick(it) },
                            onClearRecentSearches = { searchManager.clearRecentSearches() },
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.7f),
                                    RoundedCornerShape(12.dp)
                                )
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    when (selectedTab) {
                        0 -> { // Tracks
                            if (storagePermissionState.status.isGranted) {
                                if (searchQuery.isNotBlank() && filteredItemsState.value.isEmpty()) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = stringResource(
                                                R.string.no_results_found_for,
                                                searchQuery
                                            ),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.try_searching_with_different_keywords),
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
                                            text = stringResource(
                                                R.string.result_found,
                                                resultCount,
                                                if (resultCount != 1) "s" else ""
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                    }
                                    // Pass the scroll state to GroupedMuzixList
                                    GroupedMuzixList(
                                        groupedItems = filteredItemsState.value,
                                        onMuzixClick = { muzix, index ->
                                            onMuzixClick(
                                                muzixState.value,
                                                index
                                            )
                                        },
                                        onMuzixLongPress = { showAddDialogForMuzix = it },
                                        modifier = Modifier.weight(1f),
                                        searchQuery = searchQuery,
                                        scrollState = tracksScrollState
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.storage_permission_required_to_load_your_music),
                                    color = Color.White
                                )
                            }
                        }

                        1 -> { // Artists
                            if (storagePermissionState.status.isGranted) {
                                GroupedArtistList(
                                    groupedItems = groupedArtistItemsState.value,
                                    onArtistClick = { tracks ->
                                        onMuzixClick(tracks, 0)
                                    },
                                    modifier = Modifier.weight(1f),
                                    scrollState = artistsScrollState
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.storage_permission_required_to_load_your_music),
                                    color = Color.White
                                )
                            }
                        }

                        2 -> { // Playlists
                            val playlists by dao.getAllPlaylists()
                                .collectAsState(initial = emptyList())

                            Box(modifier = Modifier.weight(1f)) {
                                if (playlists.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Card(
                                            modifier = Modifier.size(120.dp),
                                            shape = RoundedCornerShape(24.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = 0.3f
                                                )
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painterResource(R.drawable.outline_library_music_24),
                                                    contentDescription = stringResource(R.string.no_playlists),
                                                    modifier = Modifier.size(48.dp),
                                                    tint = MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.7f
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(24.dp))

                                        Text(
                                            stringResource(R.string.no_playlists_yet),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        Text(
                                            stringResource(R.string.create_your_first_playlist_to_organize_your_music),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 160.dp),
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(playlists, key = { it.id }) { playlist ->
                                            PlaylistCard(
                                                playlist = playlist,
                                                onClick = {
                                                    val intent = Intent(
                                                        context,
                                                        PlaylistDetailActivity::class.java
                                                    )
                                                    intent.putExtra("playlistId", playlist.id)
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier.animateItemPlacement()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 8.dp else 4.dp
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                if (playlist.coverAlbumId != null) {
                    AsyncImage(
                        model = ContentUris.withAppendedId(
                            stringResource(R.string.album_art_uri).toUri(),
                            playlist.coverAlbumId
                        ),
                        contentDescription = stringResource(R.string.playlist_cover),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.baseline_music_note_24),
                        placeholder = painterResource(R.drawable.baseline_music_note_24)
                    )
                } else {
                    DefaultPlaylistCover()
                }
            }

            Spacer(Modifier.height(12.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.titleMedium.lineHeight * 0.9f
                )

                Spacer(Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "${playlist.trackCount} ${if (playlist.trackCount == 1) "track" else "tracks"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultPlaylistCover() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                    ),
                    radius = 300f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val circleRadius = width * 0.15f

            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = circleRadius,
                center = Offset(width * 0.3f, height * 0.3f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = circleRadius * 0.7f,
                center = Offset(width * 0.7f, height * 0.2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = circleRadius * 1.2f,
                center = Offset(width * 0.8f, height * 0.8f)
            )
        }

        Icon(
            painterResource(R.drawable.baseline_music_note_24),
            contentDescription = stringResource(R.string.default_playlist_cover),
            modifier = Modifier.size(36.dp),
            tint = Color.White.copy(alpha = 0.9f)
        )
    }
}

fun Modifier.tabIndicatorOffset(tabPosition: TabPosition): Modifier {
    return fillMaxWidth()
        .wrapContentSize(Alignment.BottomStart)
        .offset(x = tabPosition.left)
        .width(tabPosition.width)
}