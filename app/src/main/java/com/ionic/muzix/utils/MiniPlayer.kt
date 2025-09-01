package com.ionic.muzix.utils

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.ionic.muzix.R
import com.ionic.muzix.data.Muzix
import kotlinx.coroutines.delay

@Composable
fun MiniPlayer(
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var musicService by remember { mutableStateOf<MuzixService?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var currentMuzix by remember { mutableStateOf<Muzix?>(null) }
    var isShuffle by remember { mutableStateOf(false) }
    var isRepeat by remember { mutableStateOf(false) }

    val connection = remember {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                musicService = (service as MuzixService.MusicBinder).getService()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                musicService = null
            }
        }
    }

    LaunchedEffect(Unit) {
        val intent = Intent(context, MuzixService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                context.unbindService(connection)
            } catch (_: Exception) {
                // Service might not be bound
            }
        }
    }

    // Poll for updates
    LaunchedEffect(musicService) {
        while (true) {
            musicService?.let { service ->
                isPlaying = service.exoPlayer.isPlaying
                playbackState = service.exoPlayer.playbackState
                isLoading = playbackState == Player.STATE_BUFFERING
                currentMuzix = service.getCurrentMuzix()
                isShuffle = service.isShuffle
                isRepeat = service.isRepeat
            }
            delay(1000)
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

    currentMuzix?.let { muzix ->
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp)
                .clickable { onExpandClick() },
            colors = CardDefaults.cardColors(
                containerColor = Color(0x1AFFFFFF)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
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
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.baseline_music_note_24),
                    placeholder = painterResource(R.drawable.baseline_music_note_24)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Song Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = muzix.title.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = muzix.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.basicMarquee()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Control Buttons Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Previous button
                    IconButton(
                        onClick = { skipToPrevious() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_skip_previous_24),
                            contentDescription = "Previous",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Play/Pause button
                    IconButton(
                        onClick = { togglePlayPause() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying)
                                    ImageVector.vectorResource(R.drawable.outline_pause_24)
                                else
                                    ImageVector.vectorResource(R.drawable.outline_play_arrow_24),
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Next button
                    IconButton(
                        onClick = { skipToNext() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_skip_next_24),
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                }
            }
        }
    }
}