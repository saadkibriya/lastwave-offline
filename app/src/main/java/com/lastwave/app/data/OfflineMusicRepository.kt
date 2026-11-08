package com.lastwave.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lastwave.app.model.LocalAlbum
import com.lastwave.app.model.LocalArtist
import com.lastwave.app.model.LocalFolder
import com.lastwave.app.model.LocalTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "offline_music_prefs")

@Singleton
class OfflineMusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: MediaStoreScanner
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val FAVORITES_KEY = stringSetPreferencesKey("favorite_track_ids")

    private val _rawTracks = MutableStateFlow<List<LocalTrack>>(emptyList())
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val favoriteIds = context.dataStore.data.map { prefs ->
        prefs[FAVORITES_KEY] ?: emptySet()
    }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    val tracks: StateFlow<List<LocalTrack>> = combine(_rawTracks, favoriteIds, _searchQuery) { raw, favs, query ->
        val mapped = raw.map { track ->
            track.copy(isFavorite = favs.contains(track.id.toString()))
        }
        if (query.isBlank()) {
            mapped
        } else {
            val q = query.trim().lowercase()
            mapped.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<LocalAlbum>> = tracks.map { list ->
        list.groupBy { it.album }
            .map { (albumTitle, trackList) ->
                val first = trackList.first()
                LocalAlbum(
                    id = first.id,
                    title = albumTitle,
                    artist = first.artist,
                    albumArtUri = first.albumArtUri,
                    trackCount = trackList.size
                )
            }.sortedBy { it.title.lowercase() }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artists: StateFlow<List<LocalArtist>> = tracks.map { list ->
        list.groupBy { it.artist }
            .map { (artistName, trackList) ->
                LocalArtist(
                    name = artistName,
                    trackCount = trackList.size
                )
            }.sortedBy { it.name.lowercase() }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<LocalFolder>> = tracks.map { list ->
        list.groupBy { it.folderPath }
            .map { (folderName, trackList) ->
                LocalFolder(
                    name = folderName,
                    path = folderName,
                    trackCount = trackList.size
                )
            }.sortedBy { it.name.lowercase() }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<LocalTrack>> = tracks.map { list ->
        list.filter { it.isFavorite }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        rescan()
    }

    fun rescan() {
        scope.launch {
            _isScanning.value = true
            _rawTracks.value = scanner.scanAudioFiles()
            _isScanning.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(trackId: Long) {
        scope.launch {
            context.dataStore.edit { prefs ->
                val current = prefs[FAVORITES_KEY] ?: emptySet()
                val idStr = trackId.toString()
                prefs[FAVORITES_KEY] = if (current.contains(idStr)) {
                    current - idStr
                } else {
                    current + idStr
                }
            }
        }
    }
}
