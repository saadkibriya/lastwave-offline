package com.lastwave.app.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lastwave.app.model.LocalTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class RepeatMode {
    OFF, ALL, ONE
}

@Singleton
class OfflineAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true // handle audio focus automatically
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val handler = Handler(Looper.getMainLooper())

    private val _currentTrack = MutableStateFlow<LocalTrack?>(null)
    val currentTrack: StateFlow<LocalTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private var activePlaylist: List<LocalTrack> = emptyList()
    private var currentIndex: Int = -1

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (exoPlayer.isPlaying) {
                _currentPositionMs.value = exoPlayer.currentPosition
                _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
            }
            handler.postDelayed(this, 300)
        }
    }

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    handler.post(progressRunnable)
                } else {
                    handler.removeCallbacks(progressRunnable)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    skipNext()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                _currentPositionMs.value = 0L
            }
        })
    }

    fun playTrackList(tracks: List<LocalTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        activePlaylist = if (_isShuffle.value) tracks.shuffled() else tracks
        val safeIndex = startIndex.coerceIn(0, activePlaylist.lastIndex)
        currentIndex = safeIndex
        playTrackInternal(activePlaylist[currentIndex])
    }

    private fun playTrackInternal(track: LocalTrack) {
        _currentTrack.value = track

        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.albumArtUri)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(track.contentUri)
            .setMediaId(track.id.toString())
            .setMediaMetadata(metadata)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (_currentTrack.value != null) {
                exoPlayer.play()
            } else if (activePlaylist.isNotEmpty()) {
                playTrackInternal(activePlaylist.first())
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun skipNext() {
        if (activePlaylist.isEmpty()) return
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0)
                exoPlayer.play()
            }
            RepeatMode.ALL -> {
                currentIndex = (currentIndex + 1) % activePlaylist.size
                playTrackInternal(activePlaylist[currentIndex])
            }
            RepeatMode.OFF -> {
                if (currentIndex < activePlaylist.lastIndex) {
                    currentIndex++
                    playTrackInternal(activePlaylist[currentIndex])
                } else {
                    exoPlayer.pause()
                    seekTo(0)
                }
            }
        }
    }

    fun skipPrevious() {
        if (activePlaylist.isEmpty()) return
        if (exoPlayer.currentPosition > 3000) {
            seekTo(0)
            return
        }
        if (currentIndex > 0) {
            currentIndex--
            playTrackInternal(activePlaylist[currentIndex])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            currentIndex = activePlaylist.lastIndex
            playTrackInternal(activePlaylist[currentIndex])
        } else {
            seekTo(0)
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun release() {
        handler.removeCallbacks(progressRunnable)
        exoPlayer.release()
    }
}
