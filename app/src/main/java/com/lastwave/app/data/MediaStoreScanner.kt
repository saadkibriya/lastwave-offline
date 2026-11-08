package com.lastwave.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.lastwave.app.model.LocalTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun scanAudioFiles(): List<LocalTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<LocalTrack>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Title"
                    val artist = cursor.getString(artistCol).takeIf { !it.isNullOrBlank() && it != "<unknown>" } ?: "Unknown Artist"
                    val album = cursor.getString(albumCol).takeIf { !it.isNullOrBlank() && it != "<unknown>" } ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol)
                    val filePath = cursor.getString(dataCol) ?: ""
                    val trackNum = cursor.getInt(trackCol)
                    val year = cursor.getInt(yearCol)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    val folderName = try {
                        File(filePath).parentFile?.name ?: "Music"
                    } catch (_: Exception) {
                        "Music"
                    }

                    tracks.add(
                        LocalTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            folderPath = folderName,
                            trackNumber = trackNum,
                            year = year
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks
    }
}
