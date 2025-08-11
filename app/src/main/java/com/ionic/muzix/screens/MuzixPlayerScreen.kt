package com.ionic.muzix.screens

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.ionic.muzix.R
import com.ionic.muzix.data.Muzix
import com.ionic.muzix.utils.MuzixService
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun MuzixPlayerScreen(
    muzixList: List<Muzix>,
    initialIndex: Int = 0,
    onBack: () -> Unit,
    shouldStartPlayback: Boolean = false
) {
    val context = LocalContext.current

    var muzixService by remember { mutableStateOf<MuzixService?>(null) }
    var currentIndex by rememberSaveable { mutableIntStateOf(initialIndex) }
    var isShuffle by rememberSaveable { mutableStateOf(false) }
    var isRepeat by rememberSaveable { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }

    // Swipe gesture states - improved for smoother animation
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var targetIndex by remember { mutableIntStateOf(initialIndex) }

    // Smoother swipe animation with spring physics
    val swipeAnimationProgress by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = if (isDragging) {
            // Immediate response while dragging
            tween(durationMillis = 0)
        } else {
            // Smooth spring back when released
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        },
        label = "swipeAnimation"
    )

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                muzixService = (service as MuzixService.MusicBinder).getService()
                if (shouldStartPlayback) {
                    muzixService?.setPlaylist(muzixList, initialIndex, isShuffle, isRepeat)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                muzixService = null
            }
        }
    }

    LaunchedEffect(Unit) {
        val intent = Intent(context, MuzixService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    DisposableEffect(Unit) {
        onDispose {
            context.unbindService(connection)
        }
    }

    // Poll for updates
    LaunchedEffect(muzixService) {
        while (true) {
            muzixService?.let { service ->
                isPlaying = service.exoPlayer.isPlaying
                elapsed = service.exoPlayer.currentPosition
                duration = service.exoPlayer.duration
                playbackState = service.exoPlayer.playbackState
                isLoading = playbackState == Player.STATE_BUFFERING
                currentIndex = service.currentIndex
                isShuffle = service.isShuffle
                isRepeat = service.isRepeat
            }
            delay(100)
        }
    }

    // Player control functions
    fun togglePlayPause() {
        muzixService?.exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun skipToNext() {
        muzixService?.skipToNext()
        targetIndex = muzixService?.currentIndex ?: currentIndex
    }

    fun skipToPrevious() {
        muzixService?.skipToPrevious()
        targetIndex = muzixService?.currentIndex ?: currentIndex
    }

    fun seekTo(position: Float) {
        muzixService?.exoPlayer?.let {
            if (it.duration > 0) {
                val seekPosition = (position * it.duration).toLong()
                it.seekTo(seekPosition)
            }
        }
    }

    fun toggleShuffle() {
        muzixService?.toggleShuffle()
    }

    fun toggleRepeat() {
        muzixService?.toggleRepeat()
    }

    // UI
    val muzix = muzixService?.getCurrentMuzix()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        muzix?.let { currentMuzix ->
            val albumArtUri = ContentUris.withAppendedId(
                "content://media/external/audio/albumart".toUri(),
                currentMuzix.albumId
            )

            // Blurred background
            AsyncImage(
                model = albumArtUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(18.dp),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.baseline_music_note_24),
                placeholder = painterResource(R.drawable.baseline_music_note_24)
            )

            // Dark overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0x30FFFFFF), shape = CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Main content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Album Art with improved swipe gesture and better spacing
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                    },
                                    onDragEnd = {
                                        val threshold = 200f // Lowered for responsiveness
                                        when {
                                            swipeAnimationProgress > threshold && currentIndex > 0 -> {
                                                offsetX = 350f // Animate to full swipe right
                                                skipToPrevious()
                                            }
                                            swipeAnimationProgress < -threshold && currentIndex < muzixList.size - 1 -> {
                                                offsetX = -350f // Animate to full swipe left
                                                skipToNext()
                                            }
                                            else -> {
                                                // No skip, just snap back
                                                offsetX = 0f
                                            }
                                        }
                                        isDragging = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val newOffsetX = offsetX + dragAmount.x * 0.8f
                                        offsetX = newOffsetX.coerceIn(-300f, 300f)
                                    }
                                )
                            }
                    ) {
                        val cardSpacing = 450f

                        // Previous track album art
                        if (currentIndex > 0) {
                            val prevAlbumArtUri = ContentUris.withAppendedId(
                                "content://media/external/audio/albumart".toUri(),
                                muzixList[currentIndex - 1].albumId
                            )
                            Card(
                                modifier = Modifier
                                    .size(280.dp)
                                    .graphicsLayer {
                                        translationX = swipeAnimationProgress - cardSpacing
                                        alpha = if (swipeAnimationProgress > 0) {
                                            (swipeAnimationProgress / 150f).coerceIn(0f, 1f)
                                        } else 0f
                                        scaleX = 0.9f + (swipeAnimationProgress / 500f).coerceIn(0f, 0.1f)
                                        scaleY = 0.9f + (swipeAnimationProgress / 500f).coerceIn(0f, 0.1f)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                AsyncImage(
                                    model = prevAlbumArtUri,
                                    contentDescription = "Previous Album Art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(R.drawable.baseline_music_note_24),
                                    placeholder = painterResource(R.drawable.baseline_music_note_24)
                                )
                            }
                        }

                        // Next track album art
                        if (currentIndex < muzixList.size - 1) {
                            val nextAlbumArtUri = ContentUris.withAppendedId(
                                "content://media/external/audio/albumart".toUri(),
                                muzixList[currentIndex + 1].albumId
                            )
                            Card(
                                modifier = Modifier
                                    .size(280.dp)
                                    .graphicsLayer {
                                        translationX = swipeAnimationProgress + cardSpacing
                                        alpha = if (swipeAnimationProgress < 0) {
                                            (-swipeAnimationProgress / 150f).coerceIn(0f, 1f)
                                        } else 0f
                                        scaleX = 0.9f + (-swipeAnimationProgress / 500f).coerceIn(0f, 0.1f)
                                        scaleY = 0.9f + (-swipeAnimationProgress / 500f).coerceIn(0f, 0.1f)
                                    },
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                AsyncImage(
                                    model = nextAlbumArtUri,
                                    contentDescription = "Next Album Art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    error = painterResource(R.drawable.baseline_music_note_24),
                                    placeholder = painterResource(R.drawable.baseline_music_note_24)
                                )
                            }
                        }

                        // Current track album art
                        val currentMuzix = muzixService?.getCurrentMuzix()
                        val albumArtUri = currentMuzix?.let {
                            ContentUris.withAppendedId(
                                "content://media/external/audio/albumart".toUri(),
                                it.albumId
                            )
                        }
                        Card(
                            modifier = Modifier
                                .size(280.dp)
                                .graphicsLayer {
                                    translationX = swipeAnimationProgress
                                    rotationZ = swipeAnimationProgress / 30f
                                    scaleX = 1f - (abs(swipeAnimationProgress) / 800f).coerceIn(0f, 0.1f)
                                    scaleY = 1f - (abs(swipeAnimationProgress) / 800f).coerceIn(0f, 0.1f)
                                },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            AsyncImage(
                                model = albumArtUri,
                                contentDescription = "Album Art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = painterResource(R.drawable.baseline_music_note_24),
                                placeholder = painterResource(R.drawable.baseline_music_note_24)
                            )
                        }
                    }

                    // Update the swipe animation state management
                    val swipeAnimationProgress by animateFloatAsState(
                        targetValue = if (isDragging) offsetX else 0f,
                        animationSpec = if (isDragging) {
                            tween(durationMillis = 0)
                        } else {
                            tween(durationMillis = 400, easing = FastOutSlowInEasing)
                        },
                        label = "swipeAnimation",
                        finishedListener = {
                            if (!isDragging) {
                                offsetX = 0f // Reset only if no skip occurred
                            }
                        }
                    )

                    Text(
                        text = "← Swipe to change tracks →",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 8.dp)
                    )


                    Spacer(modifier = Modifier.height(32.dp))

                    // Song Info
                    Text(
                        modifier = Modifier.basicMarquee(),
                        text = currentMuzix.title.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        modifier = Modifier.basicMarquee(),
                        text = currentMuzix.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // VLC-style Linear Progress Bar
                    MuzixProgressBar(
                        progress = if (duration > 0) elapsed.toFloat() / duration else 0f,
                        onSeek = { seekTo(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Time indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(elapsed),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = formatTime(duration),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Control buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle button
                        IconButton(
                            onClick = { toggleShuffle() }
                        ) {
                            Icon(
                                painter = if (isShuffle) painterResource(R.drawable.outline_shuffle_on_24) else painterResource(
                                    R.drawable.outline_shuffle_24
                                ),
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(
                                    alpha = 0.7f
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Previous button
                        IconButton(
                            onClick = { skipToPrevious() }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_skip_previous_24),
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Play/Pause button
                        IconButton(
                            onClick = { togglePlayPause() },
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color(0x30FFFFFF), shape = CircleShape)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying)
                                        ImageVector.vectorResource(R.drawable.outline_pause_24)
                                    else
                                        ImageVector.vectorResource(R.drawable.outline_play_arrow_24),
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // Next button
                        IconButton(
                            onClick = { skipToNext() }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.outline_skip_next_24),
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Repeat button
                        IconButton(
                            onClick = { toggleRepeat() }
                        ) {
                            Icon(
                                painter = if (isRepeat) painterResource(R.drawable.outline_repeat_on_24) else painterResource(
                                    R.drawable.outline_repeat_24
                                ),
                                contentDescription = "Repeat",
                                tint = if (isRepeat) MaterialTheme.colorScheme.primary else Color.White.copy(
                                    alpha = 0.7f
                                ),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Track info
                    Text(
                        text = "${currentIndex + 1} of ${muzixList.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MuzixProgressBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }
    var isHovered by remember { mutableStateOf(false) }

    val currentProgress = if (isDragging) dragPosition else progress

    var trackWidthPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                trackWidthPx = coordinates.size.width.toFloat()
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragPosition = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                        isHovered = true
                    },
                    onDragEnd = {
                        onSeek(dragPosition)
                        isDragging = false
                        isHovered = false
                    },
                    onDrag = { change, _ ->
                        dragPosition = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val seekPosition = offset.x / trackWidthPx
                    onSeek(seekPosition.coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Background track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.2f))
        )

        // Progress track
        Box(
            modifier = Modifier
                .fillMaxWidth(currentProgress)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White)
        )

        // Progress knob
        val dotSize = if (isDragging || isHovered) 12.dp else 4.dp
        val offsetAdjust = dotSize / 2
        val offsetX = with(density) {
            (currentProgress * trackWidthPx).toDp() - offsetAdjust
        }

        Box(
            modifier = Modifier
                .offset(x = offsetX)
                .size(dotSize)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@SuppressLint("DefaultLocale")
fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun WavyCircleButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    // Capture composable values outside Canvas (important!)
    val waveColor = MaterialTheme.colorScheme.primary
    val iconTint = MaterialTheme.colorScheme.onPrimary

    // Infinite animation for pulsating effect (only visually active when isPlaying)
    val infiniteTransition = rememberInfiniteTransition()
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val waveAmplitude by infiniteTransition.animateFloat(
        initialValue = 3f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    IconButton(
        onClick = onClick,
        modifier = modifier.size(72.dp)
    ) {
        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            // Canvas drawScope: no composable calls here
            Canvas(modifier = Modifier.matchParentSize()) {
                val radius = size.minDimension / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val points = 120
                val path = Path()

                for (i in 0..points) {
                    val angleDeg = i * (360f / points)
                    val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()

                    val dynamicAmp = if (isPlaying) waveAmplitude else 0f
                    val phaseRad = Math.toRadians(wavePhase.toDouble()).toFloat()
                    // multiple waves for visual complexity
                    val wave = (sin(angleRad * 6 + phaseRad) * 0.6f +
                            sin(angleRad * 2 - phaseRad * 0.5f) * 0.25f)

                    val r = radius + dynamicAmp * wave
                    val x = center.x + r * cos(angleRad)
                    val y = center.y + r * sin(angleRad)

                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()

                // use the captured color (waveColor) here -- NOT MaterialTheme inside Canvas
                drawPath(path = path, color = waveColor, style = Fill)
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = iconTint,
                    strokeWidth = 2.dp
                )
            } else {
                // icon is a composable call so it's outside draw scope (OK)
                Icon(
                    painter = if (isPlaying)
                        painterResource(R.drawable.outline_pause_24)
                    else
                        painterResource(R.drawable.outline_play_arrow_24),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}