package com.ionic.muzix.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ionic.muzix.R
import com.ionic.muzix.data.Muzix
import android.content.ContentUris
import androidx.core.net.toUri
import com.ionic.muzix.activity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MusicService : Service() {

    private val binder = MusicBinder()
    lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private var isForeground = false

    var muzixList: List<Muzix> = emptyList()
    var shuffledList: List<Muzix> = emptyList()
    var currentIndex: Int = 0
    var isShuffle: Boolean = false
    var isRepeat: Boolean = false

    private val scope = CoroutineScope(Dispatchers.Main)
    private var updateJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
        mediaSession = MediaSessionCompat(this, "MusicService")
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                exoPlayer.play()
            }

            override fun onPause() {
                exoPlayer.pause()
            }

            override fun onSkipToNext() {
                skipToNext()
            }

            override fun onSkipToPrevious() {
                skipToPrevious()
            }

            override fun onSeekTo(pos: Long) {
                exoPlayer.seekTo(pos)
            }
        })

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startUpdating()
                } else {
                    stopUpdating()
                }
                updatePlaybackState()
                updateNotification()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        val muzix = getCurrentMuzix()
                        if (muzix != null) {
                            updateMetadata(muzix)
                        }
                        updatePlaybackState()
                    }
                    Player.STATE_ENDED -> {
                        if (isRepeat) {
                            exoPlayer.seekTo(0)
                            exoPlayer.playWhenReady = true
                        } else {
                            skipToNext()
                        }
                    }
                }
                updateNotification()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updatePlaybackState()
                updateNotification()
            }
        })

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "music_channel",
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            "ACTION_PLAY_PAUSE" -> {
                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
            }
            "ACTION_NEXT" -> skipToNext()
            "ACTION_PREV" -> skipToPrevious()
            "ACTION_SHUFFLE" -> toggleShuffle()
            "ACTION_REPEAT" -> toggleRepeat()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopUpdating()
        exoPlayer.release()
        mediaSession.release()
        super.onDestroy()
    }

    fun setPlaylist(list: List<Muzix>, index: Int, shuffle: Boolean, repeat: Boolean) {
        muzixList = list
        currentIndex = index
        isShuffle = shuffle
        isRepeat = repeat
        updateShuffledList()
        playCurrent()
    }

    private fun updateShuffledList() {
        shuffledList = if (isShuffle) muzixList.shuffled() else muzixList
    }

    fun toggleShuffle() {
        isShuffle = !isShuffle
        updateShuffledList()
        currentIndex = 0 // Reset to avoid index issues
        playCurrent()
    }

    fun toggleRepeat() {
        isRepeat = !isRepeat
        updateNotification() // To update the icon
    }

    private fun playCurrent() {
        val list = if (isShuffle) shuffledList else muzixList
        val muzix = list.getOrNull(currentIndex) ?: return
        val mediaItem = MediaItem.fromUri(muzix.data)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        updateMetadata(muzix)
        updatePlaybackState()
        updateNotification()
    }

    fun skipToNext() {
        val list = if (isShuffle) shuffledList else muzixList
        currentIndex = (currentIndex + 1) % list.size
        playCurrent()
    }

    fun skipToPrevious() {
        val list = if (isShuffle) shuffledList else muzixList
        currentIndex = if (currentIndex - 1 < 0) list.size - 1 else currentIndex - 1
        playCurrent()
    }

    private fun updateMetadata(muzix: Muzix) {
        val bitmap = getAlbumArt(muzix.albumId)
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, muzix.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, muzix.artist)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.duration)
            .build()
        mediaSession.setMetadata(metadata)
    }

    private fun getAlbumArt(albumId: Long): Bitmap? {
        val uri = ContentUris.withAppendedId(
            "content://media/external/audio/albumart".toUri(),
            albumId
        )
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                return BitmapFactory.decodeStream(input)
            }
        } catch (_: Exception) {
            // Handle error silently
        }
        return null
    }

    private fun updatePlaybackState() {
        val state = if (exoPlayer.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, exoPlayer.currentPosition, 1f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun startUpdating() {
        updateJob?.cancel()
        updateJob = scope.launch {
            while (true) {
                updatePlaybackState()
                delay(1000)
            }
        }
    }

    private fun stopUpdating() {
        updateJob?.cancel()
        updateJob = null
    }

    private fun buildNotification(): Notification {
        val currentList = if (isShuffle) shuffledList else muzixList
        val currentMuzix = currentList.getOrNull(currentIndex)
            ?: return NotificationCompat.Builder(this, "music_channel")
                .setSmallIcon(R.drawable.baseline_music_note_24)
                .setContentTitle("No Track")
                .setContentText("")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

        val isPlaying = exoPlayer.isPlaying
        val playPauseIcon = if (isPlaying) R.drawable.outline_pause_24 else R.drawable.outline_play_arrow_24
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val shuffleIcon = if (isShuffle) R.drawable.outline_shuffle_on_24 else R.drawable.outline_shuffle_24
        val repeatIcon = if (isRepeat) R.drawable.outline_repeat_on_24 else R.drawable.outline_repeat_24

        val playPauseIntent = Intent(this, MusicService::class.java).apply { action = "ACTION_PLAY_PAUSE" }
        val playPausePending = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val nextIntent = Intent(this, MusicService::class.java).apply { action = "ACTION_NEXT" }
        val nextPending = PendingIntent.getService(this, 1, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val prevIntent = Intent(this, MusicService::class.java).apply { action = "ACTION_PREV" }
        val prevPending = PendingIntent.getService(this, 2, prevIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val shuffleIntent = Intent(this, MusicService::class.java).apply { action = "ACTION_SHUFFLE" }
        val shufflePending = PendingIntent.getService(this, 3, shuffleIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val repeatIntent = Intent(this, MusicService::class.java).apply { action = "ACTION_REPEAT" }
        val repeatPending = PendingIntent.getService(this, 4, repeatIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val contentIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val bitmap = getAlbumArt(currentMuzix.albumId)

        return NotificationCompat.Builder(this, "music_channel")
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setContentTitle(currentMuzix.title)
            .setContentText(currentMuzix.artist)
            .setLargeIcon(bitmap)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .addAction(shuffleIcon, "Shuffle", shufflePending)
            .addAction(R.drawable.outline_skip_previous_24, "Previous", prevPending)
            .addAction(playPauseIcon, playPauseTitle, playPausePending)
            .addAction(R.drawable.outline_skip_next_24, "Next", nextPending)
            .addAction(repeatIcon, "Repeat", repeatPending)
            .setStyle(MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(1, 2, 3) // Prev, Play/Pause, Next in compact view
            )
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(1, notification)

        if (exoPlayer.isPlaying) {
            if (!isForeground) {
                startForeground(1, notification)
                isForeground = true
            }
        } else {
            if (isForeground) {
                stopForeground(false)
                isForeground = false
            }
        }
    }

    fun getCurrentMuzix(): Muzix? {
        val list = if (isShuffle) shuffledList else muzixList
        return list.getOrNull(currentIndex)
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
}