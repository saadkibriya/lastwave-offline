package com.lastwave.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lastwave.app.model.LocalTrack

@Composable
fun TrackListItem(
    track: LocalTrack,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art thumbnail
            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (track.albumArtUri != null) {
                    AsyncImage(
                        model = track.albumArtUri,
                        contentDescription = track.album,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title and Artist
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} • ${track.album}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration
            Text(
                text = track.formattedDuration,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            // Favorite Button
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (track.isFavorite) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
