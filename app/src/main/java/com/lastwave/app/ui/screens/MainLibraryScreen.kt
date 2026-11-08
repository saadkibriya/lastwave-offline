package com.lastwave.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lastwave.app.data.OfflineMusicRepository
import com.lastwave.app.model.LocalAlbum
import com.lastwave.app.model.LocalArtist
import com.lastwave.app.model.LocalFolder
import com.lastwave.app.model.LocalTrack
import com.lastwave.app.playback.OfflineAudioPlayer
import com.lastwave.app.ui.components.MiniPlayer
import com.lastwave.app.ui.components.TrackListItem

enum class LibraryTab(val title: String) {
    TRACKS("Tracks"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    FAVORITES("Favorites")
}

@Composable
fun MainLibraryScreen(
    repository: OfflineMusicRepository,
    player: OfflineAudioPlayer
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNowPlaying by remember { mutableStateOf(false) }

    val tracks by repository.tracks.collectAsState()
    val albums by repository.albums.collectAsState()
    val artists by repository.artists.collectAsState()
    val folders by repository.folders.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val isScanning by repository.isScanning.collectAsState()
    val searchQuery by repository.searchQuery.collectAsState()

    val currentTrack by player.currentTrack.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val positionMs by player.currentPositionMs.collectAsState()
    val durationMs by player.durationMs.collectAsState()
    val isShuffle by player.isShuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()

    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f

    // Optional drill-down filters (e.g. clicking an album or artist)
    var activeFilterTitle by remember { mutableStateOf<String?>(null) }
    var filteredTrackList by remember { mutableStateOf<List<LocalTrack>?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                // Header with Search and Rescan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { repository.setSearchQuery(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search songs, artists, albums...") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { repository.setSearchQuery("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { repository.rescan() },
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Rescan")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable category tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    LibraryTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                activeFilterTitle = null
                                filteredTrackList = null
                            },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        },
        bottomBar = {
            MiniPlayer(
                track = currentTrack,
                isPlaying = isPlaying,
                progress = progress,
                onPlayPause = { player.togglePlayPause() },
                onSkipNext = { player.skipNext() },
                onClick = { showNowPlaying = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredTrackList != null) {
                // Filtered songs view (e.g. from an album or artist)
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { filteredTrackList = null }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Close filter")
                        }
                        Text(
                            text = activeFilterTitle ?: "Filtered Tracks",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(filteredTrackList!!) { index, track ->
                            TrackListItem(
                                track = track,
                                isPlaying = currentTrack?.id == track.id,
                                onClick = { player.playTrackList(filteredTrackList!!, index) },
                                onToggleFavorite = { repository.toggleFavorite(track.id) }
                            )
                        }
                    }
                }
            } else {
                when (LibraryTab.entries[selectedTab]) {
                    LibraryTab.TRACKS -> {
                        if (tracks.isEmpty() && !isScanning) {
                            EmptyLibraryView(onRescan = { repository.rescan() })
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(tracks) { index, track ->
                                    TrackListItem(
                                        track = track,
                                        isPlaying = currentTrack?.id == track.id,
                                        onClick = { player.playTrackList(tracks, index) },
                                        onToggleFavorite = { repository.toggleFavorite(track.id) }
                                    )
                                }
                            }
                        }
                    }

                    LibraryTab.ALBUMS -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 140.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(albums) { album ->
                                AlbumCard(album = album, onClick = {
                                    activeFilterTitle = album.title
                                    filteredTrackList = tracks.filter { it.album == album.title }
                                })
                            }
                        }
                    }

                    LibraryTab.ARTISTS -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(artists) { artist ->
                                ArtistRow(artist = artist, onClick = {
                                    activeFilterTitle = artist.name
                                    filteredTrackList = tracks.filter { it.artist == artist.name }
                                })
                            }
                        }
                    }

                    LibraryTab.FOLDERS -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(folders) { folder ->
                                FolderRow(folder = folder, onClick = {
                                    activeFilterTitle = folder.name
                                    filteredTrackList = tracks.filter { it.folderPath == folder.path }
                                })
                            }
                        }
                    }

                    LibraryTab.FAVORITES -> {
                        if (favorites.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No favorites yet. Tap the heart on any song!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(favorites) { index, track ->
                                    TrackListItem(
                                        track = track,
                                        isPlaying = currentTrack?.id == track.id,
                                        onClick = { player.playTrackList(favorites, index) },
                                        onToggleFavorite = { repository.toggleFavorite(track.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full screen player bottom sheet
    if (showNowPlaying) {
        NowPlayingSheet(
            track = currentTrack,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            onPlayPause = { player.togglePlayPause() },
            onSeekTo = { player.seekTo(it) },
            onSkipNext = { player.skipNext() },
            onSkipPrevious = { player.skipPrevious() },
            onToggleShuffle = { player.toggleShuffle() },
            onToggleRepeat = { player.toggleRepeat() },
            onToggleFavorite = { repository.toggleFavorite(it) },
            onDismiss = { showNowPlaying = false }
        )
    }
}

@Composable
fun AlbumCard(
    album: LocalAlbum,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (album.albumArtUri != null) {
                    AsyncImage(
                        model = album.albumArtUri,
                        contentDescription = album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${album.artist} • ${album.trackCount} songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ArtistRow(
    artist: LocalArtist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${artist.trackCount} tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FolderRow(
    folder: LocalFolder,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${folder.trackCount} tracks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyLibraryView(onRescan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No offline music found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add audio files (.mp3, .flac, .m4a, .wav, .opus) to your device storage and tap Rescan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        androidx.compose.material3.Button(onClick = onRescan) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Rescan Device")
        }
    }
}
