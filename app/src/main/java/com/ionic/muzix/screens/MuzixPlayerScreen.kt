package com.ionic.muzix.screens

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.ionic.muzix.R
import com.ionic.muzix.data.Muzix
import com.ionic.muzix.utils.MusicService
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun MuzixPlayerScreen(
    muzixList: List<Muzix>,
    initialIndex: Int = 0,
    onBack: () -> Unit,
    shouldStartPlayback: Boolean = false // New parameter to control auto-play
) {
    val context = LocalContext.current

    var musicService by remember { mutableStateOf<MusicService?>(null) }
    var currentIndex by rememberSaveable { mutableIntStateOf(initialIndex) }
    var isShuffle by rememberSaveable { mutableStateOf(false) }
    var isRepeat by rememberSaveable { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }

    // Swipe gesture states
    var offsetX by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val waveform = remember { getWaveform() }
    var waveformProgress by remember { mutableFloatStateOf(0f) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                musicService = (service as MusicService.MusicBinder).getService()
                // Only set playlist and start playing if shouldStartPlayback is true
                // This prevents music from restarting when returning from mini player
                if (shouldStartPlayback) {
                    musicService?.setPlaylist(muzixList, initialIndex, isShuffle, isRepeat)
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                musicService = null
            }
        }
    }

    LaunchedEffect(Unit) {
        val intent = Intent(context, MusicService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    DisposableEffect(Unit) {
        onDispose {
            context.unbindService(connection)
        }
    }

    // Poll for updates
    LaunchedEffect(musicService) {
        while (true) {
            musicService?.let { service ->
                isPlaying = service.exoPlayer.isPlaying
                elapsed = service.exoPlayer.currentPosition
                duration = service.exoPlayer.duration
                playbackState = service.exoPlayer.playbackState
                isLoading = playbackState == Player.STATE_BUFFERING
                currentIndex = service.currentIndex
                isShuffle = service.isShuffle
                isRepeat = service.isRepeat
                if (duration > 0) {
                    waveformProgress = elapsed.toFloat() / duration
                }
            }
            delay(500)
        }
    }

    // Player control functions
    fun togglePlayPause() {
        musicService?.exoPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun skipToNext() {
        musicService?.skipToNext()
    }

    fun skipToPrevious() {
        musicService?.skipToPrevious()
    }

    fun seekTo(position: Float) {
        musicService?.exoPlayer?.let {
            if (it.duration > 0) {
                val seekPosition = (position * it.duration).toLong()
                it.seekTo(seekPosition)
            }
        }
    }

    fun toggleShuffle() {
        musicService?.toggleShuffle()
    }

    fun toggleRepeat() {
        musicService?.toggleRepeat()
    }

    // UI
    val muzix = musicService?.getCurrentMuzix()

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
                    // Album Art with swipe gesture
                    Card(
                        modifier = Modifier
                            .size(280.dp)
                            .graphicsLayer {
                                translationX = offsetX
                                rotationZ = offsetX / 20f // Subtle rotation effect
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                    },
                                    onDragEnd = {
                                        val threshold = 150f
                                        when {
                                            offsetX > threshold -> {
                                                // Swiped right - go to previous track
                                                skipToPrevious()
                                            }
                                            offsetX < -threshold -> {
                                                // Swiped left - go to next track
                                                skipToNext()
                                            }
                                        }
                                        // Reset position
                                        offsetX = 0f
                                        isDragging = false
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val newOffsetX = offsetX + dragAmount.x
                                        offsetX = newOffsetX.coerceIn(-300f, 300f)
                                    }
                                )
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

                    // Add a subtle hint for swipe gesture when not dragging
                    if (!isDragging && abs(offsetX) < 50f) {
                        Text(
                            text = "← Swipe to change tracks →",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Song Info
                    Text(
                        text = currentMuzix.title.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currentMuzix.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Waveform or Progress Bar
                    WaveformVisualization(
                        waveform = waveform,
                        progress = waveformProgress,
                        onSeek = { seekTo(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
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
                                painter = if (isShuffle) painterResource(R.drawable.outline_shuffle_on_24) else painterResource(R.drawable.outline_shuffle_24),
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
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
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    CircleShape
                                )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
//                                    color = Color.White
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
                                painter = if (isRepeat) painterResource(R.drawable.outline_repeat_on_24) else painterResource(R.drawable.outline_repeat_24),
                                contentDescription = "Repeat",
                                tint = if (isRepeat) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
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
fun WaveformVisualization(
    waveform: IntArray,
    progress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxAmplitude = waveform.maxOrNull()?.toFloat() ?: 1f
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val seekPosition = offset.x / size.width
                onSeek(seekPosition.coerceIn(0f, 1f))
            }
        }
    ) {
        val barWidth = size.width / waveform.size
        val centerY = size.height / 2f
        val progressX = progress * size.width

        waveform.forEachIndexed { index, height ->
            val x = index * barWidth + barWidth / 2
            val barHeight = (height / maxAmplitude) * size.height
            val color = if (x <= progressX) Color.White else Color.White.copy(alpha = 0.3f)

            drawLine(
                color = color,
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth * 0.7f,
                cap = StrokeCap.Round
            )
        }
    }
}

@SuppressLint("DefaultLocale")
fun formatTime(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

fun getWaveform(): IntArray {
    return IntArray(50) { 5 + Random.nextInt(50) }
}