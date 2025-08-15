package com.ionic.muzix.screens

import android.content.ContentUris
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.res.painterResource
import com.ionic.muzix.data.Muzix
import com.ionic.muzix.data.MyApplication
import com.ionic.muzix.data.Playlist
import com.ionic.muzix.utils.getMuzixByIds
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ionic.muzix.R
import com.ionic.muzix.data.PlaylistDao
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    onPlay: (List<Muzix>, Int) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyApplication
    val dao = app.database.playlistDao()
    var playlist by remember { mutableStateOf<Playlist?>(null) }
    val muzixList = remember { mutableStateListOf<Muzix>() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isShuffled by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val dragDropState = rememberDragDropState(
        lazyListState = listState,
        onMove = { fromIndex, toIndex ->
            muzixList.apply {
                add(toIndex, removeAt(fromIndex))
            }
            coroutineScope.launch {
                dao.reorderPlaylist(playlistId, muzixList.map { it.id })
            }
        }
    )

    LaunchedEffect(playlistId) {
        playlist = dao.getPlaylist(playlistId)
        val ids = dao.getMuzixIdsForPlaylist(playlistId)
        muzixList.clear()
        muzixList.addAll(getMuzixByIds(context, ids))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    stringResource(R.string.delete_playlist),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    "This will delete \"${playlist?.name}\" and cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            dao.deletePlaylistAndCrossRefs(playlistId)
                            showDeleteConfirm = false
                            onBack()
                        }
                        Toast.makeText(context, "Playlist deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        playlist?.name ?: "<Unknown>",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            stringResource(R.string.delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
        ) {

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                radius = 800f
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Card(
                                modifier = Modifier.size(120.dp),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                            ) {
                                if (playlist?.coverAlbumId != null) {
                                    AsyncImage(
                                        model = ContentUris.withAppendedId(
                                            "content://media/external/audio/albumart".toUri(),
                                            playlist!!.coverAlbumId!!
                                        ),
                                        contentDescription = "Cover",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        MaterialTheme.colorScheme.secondary
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_music_note_24),
                                            contentDescription = "Default",
                                            modifier = Modifier.size(48.dp),
                                            tint = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(20.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    playlist?.name ?: "",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${muzixList.size} ${if (muzixList.size == 1) "track" else "tracks"}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (muzixList.isNotEmpty()) {
                            Spacer(Modifier.height(20.dp))


                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { onPlay(muzixList, 0) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Play All")
                                }

                                OutlinedButton(
                                    onClick = {
                                        isShuffled = !isShuffled
                                        val shuffledList = if (isShuffled) muzixList.shuffled() else muzixList
                                        onPlay(shuffledList, 0)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.outline_shuffle_24),
                                        contentDescription = "Shuffle",
                                        modifier = Modifier.size(20.dp),
                                        tint = if (isShuffled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Shuffle")
                                }
                            }
                        }
                    }
                }
            }

            if (muzixList.isEmpty()) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            painterResource(R.drawable.baseline_music_off_24),
                            contentDescription = "No music",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No tracks in this playlist",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.add_some_music_to_get_started),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(muzixList, key = { _, muzix -> muzix.id }) { index, muzix ->
                        val isDragging = dragDropState.draggingItemIndex == index
                        val offsetY = if (isDragging) dragDropState.draggedDistance else 0f

                        SwipeToDeleteTrackItem(
                            muzix = muzix,
                            index = index,
                            playlistId = playlistId,
                            muzixList = muzixList,
                            dao = dao,
                            onPlay = { onPlay(muzixList, index) },
                            dragDropState = dragDropState,
                            isDragging = isDragging,
                            offsetY = offsetY,
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteTrackItem(
    muzix: Muzix,
    index: Int,
    playlistId: Long,
    muzixList: MutableList<Muzix>,
    dao: PlaylistDao,
    onPlay: () -> Unit,
    dragDropState: DragDropState,
    isDragging: Boolean,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                coroutineScope.launch {
                    dao.removeMuzixFromPlaylist(playlistId, muzix.id)
                    muzixList.removeAt(index)
                }
                true
            } else {
                true
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd)
                    MaterialTheme.colorScheme.error
                else
                    Color.Transparent,
                label = "background_color"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        content = {
            EnhancedTrackItem(
                muzix = muzix,
                index = index,
                onPlay = onPlay,
                dragDropState = dragDropState,
                isDragging = isDragging,
                offsetY = offsetY,
                modifier = modifier
            )
        }
    )
}

@Composable
private fun EnhancedTrackItem(
    muzix: Muzix,
    index: Int,
    onPlay: () -> Unit,
    dragDropState: DragDropState,
    isDragging: Boolean,
    offsetY: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .graphicsLayer {
                translationY = offsetY
                scaleX = if (isDragging) 1.05f else 1f
                scaleY = if (isDragging) 1.05f else 1f
                shadowElevation = if (isDragging) with(density) { 8.dp.toPx() } else 0f
            }
            .zIndex(if (isDragging) 1f else 0f),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.baseline_drag_handle_24),
                contentDescription = "Drag to reorder",
                tint = if (isDragging)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(32.dp)
                    .padding(4.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                dragDropState.onDragStart(index)
                            },
                            onDragEnd = {
                                dragDropState.onDragEnd()
                            },
                            onDrag = { _, dragAmount ->
                                dragDropState.onDrag(dragAmount.y)

                                val itemHeight = 80.dp.toPx()
                                val currentPosition = index * itemHeight + dragDropState.draggedDistance
                                val targetIndex = (currentPosition / itemHeight).roundToInt()
                                    .coerceIn(0, dragDropState.itemCount - 1)

                                if (targetIndex != index) {
                                    dragDropState.onDragOver(targetIndex)
                                }
                            }
                        )
                    }
            )

            Spacer(Modifier.width(12.dp))

            // Track number
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(12.dp))

            // Album art
            Card(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                AsyncImage(
                    model = ContentUris.withAppendedId(
                        "content://media/external/audio/albumart".toUri(),
                        muzix.albumId
                    ),
                    contentDescription = "Album Art",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.baseline_music_note_24),
                    placeholder = painterResource(R.drawable.baseline_music_note_24)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Track info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    muzix.title ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    muzix.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

// Improved drag and drop implementation
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (Int, Int) -> Unit
): DragDropState {
    return remember(lazyListState) {
        DragDropState(
            lazyListState = lazyListState,
            onMove = onMove
        )
    }
}

class DragDropState(
    private val lazyListState: LazyListState,
    private val onMove: (Int, Int) -> Unit
) {
    var draggedDistance by mutableStateOf(0f)
    var draggingItemIndex by mutableStateOf<Int?>(null)
    var itemCount by mutableStateOf(0)

    fun onDragStart(index: Int) {
        draggingItemIndex = index
        itemCount = lazyListState.layoutInfo.totalItemsCount
    }

    fun onDragEnd() {
        draggingItemIndex = null
        draggedDistance = 0f
    }

    fun onDrag(offset: Float) {
        draggedDistance += offset
    }

    fun onDragOver(targetIndex: Int) {
        draggingItemIndex?.let { fromIndex ->
            if (fromIndex != targetIndex) {
                onMove(fromIndex, targetIndex)
                draggingItemIndex = targetIndex
                draggedDistance = 0f // Reset drag distance after moving
            }
        }
    }
}