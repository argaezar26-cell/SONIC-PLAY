package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.controller.PlaybackUiState
import com.example.audio.routing.AudioRoutingState
import com.example.data.model.OutputRouteCategory
import com.example.data.model.SimpleEqPreset
import com.example.data.model.Track
import com.example.data.prefs.AppPreferencesState
import com.example.ui.components.AudioSpecPill
import com.example.ui.components.QualityTierBadge
import com.example.ui.components.TrackArtworkImage
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverMist
import com.example.ui.theme.SlateBorder

@Composable
fun HomeScreen(
    allTracks: List<Track>,
    recentTracks: List<Track>,
    recentlyAdded: List<Track>,
    favoriteTracks: List<Track>,
    playbackState: PlaybackUiState,
    routingState: AudioRoutingState,
    preferences: AppPreferencesState,
    onPickFolder: () -> Unit,
    onPlayTrackFromList: (List<Track>, Int) -> Unit,
    onShuffleAll: (List<Track>) -> Unit,
    onTogglePlayPause: () -> Unit,
    onJumpToQueueIndex: (Int) -> Unit,
    onSelectSimpleEqPreset: (SimpleEqPreset) -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val activeDevice = routingState.activeDevice
    val routeIcon = when (activeDevice?.routeCategory) {
        OutputRouteCategory.EXTERNAL_USB_DAC,
        OutputRouteCategory.USB_TYPE_C_ADAPTER -> Icons.Filled.Usb
        OutputRouteCategory.BLUETOOTH -> Icons.Filled.BluetoothAudio
        OutputRouteCategory.WIRED_HEADPHONES -> Icons.Filled.Headphones
        else -> Icons.Filled.Speaker
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Clean Musical Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.img_sonicflow_hero),
                    contentDescription = "SONICFLOW acoustic header",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    bg.copy(alpha = 0.25f),
                                    bg.copy(alpha = 0.78f),
                                    bg
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(routeIcon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                                Text(
                                    text = activeDevice?.routeCategory?.displayName ?: "System Audio",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (allTracks.isNotEmpty()) {
                            AudioSpecPill(text = "${allTracks.size} Songs")
                        }
                    }
                    Text("SONICFLOW", style = MaterialTheme.typography.displayMedium, color = IceWhite)
                    Text(
                        text = if (allTracks.isEmpty()) "Add a local music folder to start listening" else "Your offline Hi-Res & lossless music collection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverMist
                    )
                }
            }
        }

        // First-time or Empty Library CTA
        if (allTracks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = surface),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Start Your Music Library", style = MaterialTheme.typography.titleLarge, color = IceWhite)
                        Text(
                            text = "Pick any folder with FLAC, WAV, MP3, M4A, or OGG files. Everything stays 100% private and offline on your phone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SilverMist
                        )
                        Button(
                            onClick = onPickFolder,
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianBlack),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("home_pick_folder_button")
                        ) {
                            Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Select Music Folder", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Continue Listening / Hero Play Card
            item {
                val heroTrack = playbackState.currentTrack ?: recentTracks.firstOrNull() ?: allTracks.first()
                val isCurrentLoaded = playbackState.currentTrack?.id == heroTrack.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            if (isCurrentLoaded) onOpenPlayer()
                            else onPlayTrackFromList(allTracks, allTracks.indexOfFirst { it.id == heroTrack.id }.coerceAtLeast(0))
                        },
                    colors = CardDefaults.cardColors(containerColor = surface),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isCurrentLoaded) "CONTINUE LISTENING" else "READY TO PLAY",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TrackArtworkImage(
                                artworkPath = heroTrack.artworkCachePath,
                                artworkEnabled = preferences.albumArtworkEnabled,
                                contentDescription = heroTrack.album,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = heroTrack.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IceWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${heroTrack.artist} • ${heroTrack.album}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SilverMist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    QualityTierBadge(tier = heroTrack.qualityTier)
                                    Text(heroTrack.badgeText, style = MaterialTheme.typography.labelSmall, color = MutedSteel, maxLines = 1)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                onClick = {
                                    if (isCurrentLoaded) onTogglePlayPause()
                                    else onPlayTrackFromList(allTracks, allTracks.indexOfFirst { it.id == heroTrack.id }.coerceAtLeast(0))
                                },
                                shape = CircleShape,
                                color = accent,
                                modifier = Modifier.size(52.dp).testTag("home_continue_play_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isCurrentLoaded && playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isCurrentLoaded && playbackState.isPlaying) "Pause" else "Play",
                                        tint = ObsidianBlack,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onShuffleAll(allTracks) },
                                border = BorderStroke(1.dp, SlateBorder),
                                modifier = Modifier.weight(1f).height(46.dp).testTag("home_shuffle_all_button")
                            ) {
                                Icon(Icons.Filled.Shuffle, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Shuffle All", color = IceWhite)
                            }
                            OutlinedButton(
                                onClick = onOpenLibrary,
                                border = BorderStroke(1.dp, SlateBorder),
                                modifier = Modifier.weight(1f).height(46.dp).testTag("home_browse_library_button")
                            ) {
                                Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Library", color = IceWhite)
                            }
                        }
                    }
                }
            }
        }

        // Current Queue Preview (if active)
        if (playbackState.queue.size > 1) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                            Text("UP NEXT IN QUEUE", style = MaterialTheme.typography.labelLarge, color = SilverMist)
                        }
                        TextButton(onClick = onOpenPlayer) {
                            Text("Open Player", color = accent)
                        }
                    }
                    val start = (playbackState.currentIndex + 1).coerceAtLeast(0)
                    val previewItems = playbackState.queue.drop(start).take(3)
                    if (previewItems.isNotEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = surface),
                            border = BorderStroke(1.dp, SlateBorder),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                previewItems.forEachIndexed { offset, qTrack ->
                                    val targetIdx = start + offset
                                    Surface(
                                        color = surfaceVariant,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onJumpToQueueIndex(targetIdx) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${targetIdx + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = accent,
                                                modifier = Modifier.width(24.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(qTrack.title, style = MaterialTheme.typography.titleSmall, color = IceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(qTrack.artist, style = MaterialTheme.typography.bodySmall, color = MutedSteel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play next song", tint = SilverMist, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recently Played Shelf
        if (recentTracks.isNotEmpty()) {
            item {
                HorizontalTrackShelf(
                    title = "RECENTLY PLAYED",
                    tracks = recentTracks,
                    artworkEnabled = preferences.albumArtworkEnabled,
                    onPlayTrack = { idx -> onPlayTrackFromList(recentTracks, idx) }
                )
            }
        }

        // Recently Added Shelf
        if (recentlyAdded.isNotEmpty()) {
            item {
                HorizontalTrackShelf(
                    title = "RECENTLY ADDED",
                    tracks = recentlyAdded,
                    artworkEnabled = preferences.albumArtworkEnabled,
                    onPlayTrack = { idx -> onPlayTrackFromList(recentlyAdded, idx) }
                )
            }
        }

        // Favorite Tracks Shelf
        if (favoriteTracks.isNotEmpty()) {
            item {
                HorizontalTrackShelf(
                    title = "FAVORITE TRACKS",
                    tracks = favoriteTracks,
                    artworkEnabled = preferences.albumArtworkEnabled,
                    onPlayTrack = { idx -> onPlayTrackFromList(favoriteTracks, idx) }
                )
            }
        }

        // Quick Sound Tone Selector (Simple Presets)
        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Equalizer, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                        Text("QUICK SOUND TONE", style = MaterialTheme.typography.labelLarge, color = SilverMist)
                    }
                    TextButton(onClick = onOpenEqualizer) {
                        Text("Tune EQ", color = accent)
                    }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(SimpleEqPreset.entries.filter { it != SimpleEqPreset.CUSTOM }) { preset ->
                        val selected = !preferences.dspSettings.bitTransparentMode && preferences.simpleEqPreset == preset
                        Surface(
                            modifier = Modifier
                                .width(152.dp)
                                .testTag("home_eq_${preset.name.lowercase()}")
                                .clickable { onSelectSimpleEqPreset(preset) },
                            color = if (selected) primaryContainer else surface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (selected) accent else SlateBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) accent else IceWhite,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SilverMist,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HorizontalTrackShelf(
    title: String,
    tracks: List<Track>,
    artworkEnabled: Boolean,
    onPlayTrack: (Int) -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = SilverMist,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(tracks.take(12).size) { index ->
                val track = tracks[index]
                Surface(
                    modifier = Modifier
                        .width(150.dp)
                        .clickable { onPlayTrack(index) },
                    color = surface,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SlateBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        TrackArtworkImage(
                            artworkPath = track.artworkCachePath,
                            artworkEnabled = artworkEnabled,
                            contentDescription = track.album,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(122.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = IceWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSteel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        QualityTierBadge(tier = track.qualityTier)
                    }
                }
            }
        }
    }
}
