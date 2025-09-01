package com.ionic.muzix.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ionic.muzix.R
import com.ionic.muzix.data.model.Muzix
import android.content.ContentUris
import android.content.pm.ServiceInfo
import androidx.core.net.toUri
import com.ionic.muzix.activity.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.app.ServiceCompat
import android.util.Log

class MuzixService : Service() {

    private val binder = MusicBinder()
    lateinit var exoPlayer: ExoPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var isForeground = false
    private var isServiceStarted = false

    var muzixList: List<Muzix> = emptyList()
    var shuffledList: List<Muzix> = emptyList()
    var currentIndex: Int = 0
    var isShuffle: Boolean = false
    var isRepeat: Boolean = false

    // Audio Focus Management
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var wasPlayingBeforeFocusLoss = false

    private val scope = CoroutineScope(Dispatchers.Main)
    private var updateJob: Job? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_channel"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MuzixService", "Service onCreate")

        // Initialize AudioManager
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        exoPlayer = ExoPlayer.Builder(this).build()

        // MediaSession for headset & lockscreen control
        mediaSession = MediaSessionCompat(this, "MuzixService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    if (requestAudioFocus()) {
                        exoPlayer.play()
                    }
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

                override fun onStop() {
                    abandonAudioFocus()
                    exoPlayer.pause()
                }
            })
            isActive = true
        }

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d("MuzixService", "onIsPlayingChanged: $isPlaying")
                if (isPlaying) {
                    startUpdating()
                    startForegroundServiceSafely()
                } else {
                    stopUpdating()
                    stopForegroundServiceSafely()
                }
                updatePlaybackState()
                updateNotification()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d("MuzixService", "onPlaybackStateChanged: $playbackState")
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
                            if (hasAudioFocus) {
                                exoPlayer.playWhenReady = true
                            }
                        } else {
                            skipToNext()
                        }
                    }

                    Player.STATE_BUFFERING -> {
                        TODO()
                    }

                    Player.STATE_IDLE -> {
                        TODO()
                    }
                }
                updateNotification()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updatePlaybackState()
                updateNotification()
            }
        })

        // Setup audio focus change listener
        setupAudioFocusListener()
        createNotificationChannel()
    }

    private fun setupAudioFocusListener() {
        val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // Regained audio focus
                    hasAudioFocus = true
                    if (wasPlayingBeforeFocusLoss) {
                        exoPlayer.play()
                        exoPlayer.volume = 1.0f
                        wasPlayingBeforeFocusLoss = false
                    }
                }
                AudioManager.AUDIOFOCUS_LOSS -> {
                    hasAudioFocus = false
                    wasPlayingBeforeFocusLoss = exoPlayer.isPlaying
                    exoPlayer.pause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    hasAudioFocus = false
                    wasPlayingBeforeFocusLoss = exoPlayer.isPlaying
                    exoPlayer.pause()
                }
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.volume = 0.3f // Lower volume
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
        } else {
            // For older Android versions, I'll use the deprecated method
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        hasAudioFocus = false
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music playback controls"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle media button intents
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        val action = intent?.action
        when (action) {
            "ACTION_PLAY_PAUSE" -> {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    if (requestAudioFocus()) {
                        exoPlayer.play()
                    }
                }
            }
            "ACTION_NEXT" -> skipToNext()
            "ACTION_PREV" -> skipToPrevious()
            "ACTION_SHUFFLE" -> toggleShuffle()
            "ACTION_REPEAT" -> toggleRepeat()
            "ACTION_START_FOREGROUND" -> {
                isServiceStarted = true
                if (exoPlayer.isPlaying) {
                    startForegroundServiceSafely()
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopUpdating()
        stopForegroundServiceSafely()
        abandonAudioFocus()
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

        // Start the service properly before playing
        if (!isServiceStarted) {
            val intent = Intent(this, MuzixService::class.java).apply {
                action = "ACTION_START_FOREGROUND"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

        playCurrent()
    }

    private fun updateShuffledList() {
        shuffledList = if (isShuffle) muzixList.shuffled() else muzixList
    }

    fun toggleShuffle() {
        isShuffle = !isShuffle
        updateShuffledList()
        currentIndex = 0
        playCurrent()
    }

    fun toggleRepeat() {
        isRepeat = !isRepeat
        updateNotification()
    }

    private fun playCurrent() {
        val list = if (isShuffle) shuffledList else muzixList
        val muzix = list.getOrNull(currentIndex) ?: return
        val mediaItem = MediaItem.fromUri(muzix.data)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()

        // Only start playing if we have audio focus
        if (requestAudioFocus()) {
            exoPlayer.playWhenReady = true
        } else {
            exoPlayer.playWhenReady = false
        }

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
        }
        return null
    }

    private fun updatePlaybackState() {
        val state = if (exoPlayer.isPlaying)
            PlaybackStateCompat.STATE_PLAYING
        else
            PlaybackStateCompat.STATE_PAUSED

        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, exoPlayer.currentPosition, 1f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
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

    private fun startForegroundServiceSafely() {
        if (!isForeground) {
            try {
                val notification = buildNotification()
                val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                } else {
                    0
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // For Android 14+ (API 34+)
                    ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundServiceType)
                } else {
                    // For older versions
                    startForeground(NOTIFICATION_ID, notification)
                }

                isForeground = true
            } catch (_: Exception) {
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, buildNotification())
            }
        }
    }

    private fun stopForegroundServiceSafely() {
        if (isForeground) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                } else {
                    stopForeground(STOP_FOREGROUND_DETACH)
                }
                isForeground = false
            } catch (_: Exception) {
            }
        }
    }

    private fun buildNotification(): Notification {
        val currentList = if (isShuffle) shuffledList else muzixList
        val currentMuzix = currentList.getOrNull(currentIndex)
            ?: return NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_music_note_24)
                .setContentTitle("No Track")
                .setContentText("")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .build()

        val isPlaying = exoPlayer.isPlaying
        val playPauseIcon = if (isPlaying) R.drawable.outline_pause_24 else R.drawable.outline_play_arrow_24
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val shuffleIcon = if (isShuffle) R.drawable.outline_shuffle_on_24 else R.drawable.outline_shuffle_24
        val repeatIcon = if (isRepeat) R.drawable.outline_repeat_on_24 else R.drawable.outline_repeat_24

        val playPausePending = PendingIntent.getService(this, 0,
            Intent(this, MuzixService::class.java).apply { action = "ACTION_PLAY_PAUSE" },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextPending = PendingIntent.getService(this, 1,
            Intent(this, MuzixService::class.java).apply { action = "ACTION_NEXT" },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevPending = PendingIntent.getService(this, 2,
            Intent(this, MuzixService::class.java).apply { action = "ACTION_PREV" },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val shufflePending = PendingIntent.getService(this, 3,
            Intent(this, MuzixService::class.java).apply { action = "ACTION_SHUFFLE" },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val repeatPending = PendingIntent.getService(this, 4,
            Intent(this, MuzixService::class.java).apply { action = "ACTION_REPEAT" },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val contentIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bitmap = getAlbumArt(currentMuzix.albumId)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_music_note_24)
            .setContentTitle(currentMuzix.title)
            .setContentText(currentMuzix.artist)
            .setLargeIcon(bitmap)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(shuffleIcon, "Shuffle", shufflePending)
            .addAction(R.drawable.outline_skip_previous_24, "Previous", prevPending)
            .addAction(playPauseIcon, playPauseTitle, playPausePending)
            .addAction(R.drawable.outline_skip_next_24, "Next", nextPending)
            .addAction(repeatIcon, "Repeat", repeatPending)
            .setStyle(MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(1, 2, 3)
            )
            .build()
    }

    private fun updateNotification() {
        try {
            val notification = buildNotification()
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {
        }
    }

    fun getCurrentMuzix(): Muzix? {
        val list = if (isShuffle) shuffledList else muzixList
        return list.getOrNull(currentIndex)
    }

    inner class MusicBinder : Binder() {
        fun getService(): MuzixService = this@MuzixService
    }
}