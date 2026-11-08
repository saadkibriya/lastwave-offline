package com.lastwave.app.model

import android.net.Uri

data class LocalTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri? = null,
    val folderPath: String = "",
    val trackNumber: Int = 0,
    val year: Int = 0,
    val isFavorite: Boolean = false
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

data class LocalAlbum(
    val id: Long,
    val title: String,
    val artist: String,
    val albumArtUri: Uri?,
    val trackCount: Int
)

data class LocalArtist(
    val name: String,
    val trackCount: Int
)

data class LocalFolder(
    val name: String,
    val path: String,
    val trackCount: Int
)

data class LrcLine(
    val timestampMs: Long,
    val text: String
)
