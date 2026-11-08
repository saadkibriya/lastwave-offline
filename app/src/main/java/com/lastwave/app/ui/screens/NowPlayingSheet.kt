package com.lastwave.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lastwave.app.data.LrcParser
import com.lastwave.app.model.LocalTrack
import com.lastwave.app.model.LrcLine
import com.lastwave.app.playback.RepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    track: LocalTrack?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (track == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLyrics by remember { mutableStateOf(false) }
    var lyrics by remember(track.id) { mutableStateOf<List<LrcLine>?>(null) }

    LaunchedEffect(track.id) {
        lyrics = LrcParser.findLrcForTrack(track.contentUri.path ?: "")
    }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val currentSliderVal = if (isDraggingSlider) {
        sliderPosition
    } else {
        if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Collapse arrow, Now Playing title, Lyrics button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                IconButton(onClick = { showLyrics = !showLyrics }) {
                    Icon(
                        imageVector = Icons.Filled.Lyrics,
                        contentDescription = "Lyrics",
                        tint = if (showLyrics) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showLyrics && !lyrics.isNullOrEmpty()) {
                // Synced lyrics view
                val listState = rememberLazyListState()
                val currentLrcIndex = lyrics?.indexOfLast { it.timestampMs <= positionMs } ?: -1

                LaunchedEffect(currentLrcIndex) {
                    if (currentLrcIndex >= 0) {
                        listState.animateScrollToItem((currentLrcIndex - 2).coerceAtLeast(0))
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    itemsIndexed(lyrics!!) { index, item ->
                        val isActive = index == currentLrcIndex
                        Text(
                            text = item.text,
                            style = if (isActive) MaterialTheme.typography.headlineSmall
                                   else MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            } else {
                // Large Album Artwork
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp
                ) {
                    if (track.albumArtUri != null) {
                        AsyncImage(
                            model = track.albumArtUri,
                            contentDescription = track.album,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title, Artist and Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = { onToggleFavorite(track.id) }) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (track.isFavorite) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider & Time
            Slider(
                value = currentSliderVal,
                onValueChange = {
                    isDraggingSlider = true
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    val targetMs = (sliderPosition * durationMs).toLong()
                    onSeekTo(targetMs)
                    isDraggingSlider = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(if (isDraggingSlider) (sliderPosition * durationMs).toLong() else positionMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatMs(durationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Controls: Shuffle, Prev, Play/Pause, Next, Repeat
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Previous
                IconButton(onClick = onSkipPrevious) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Play / Pause
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(68.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(68.dp)
                    )
                }

                // Next
                IconButton(onClick = onSkipNext) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Repeat
                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        imageVector = when (repeatMode) {
                            RepeatMode.ONE -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
