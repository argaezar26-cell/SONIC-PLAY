package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.db.PlaylistEntity
import com.example.data.db.ScanFolderEntity
import com.example.data.model.Track
import com.example.data.repository.ScanProgress
import com.example.ui.components.QualityTierBadge
import com.example.ui.components.TrackArtworkImage
import com.example.ui.components.formatDurationMs
import com.example.ui.components.formatFileSize
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverMist
import com.example.ui.theme.SlateBorder
import com.example.ui.viewmodel.LibraryTab
import kotlinx.coroutines.flow.Flow

@Composable
fun LibraryScreen(
    allTracks: List<Track>,
    filteredTracks: List<Track>,
    favoriteTracks: List<Track>,
    recentTracks: List<Track>,
    recentlyAdded: List<Track>,
    playlists: List<PlaylistEntity>,
    scanFolders: List<ScanFolderEntity>,
    scanProgress: ScanProgress,
    searchQuery: String,
    selectedTab: LibraryTab,
    currentPlayingUri: String?,
    artworkEnabled: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectTab: (LibraryTab) -> Unit,
    onPickFolder: () -> Unit,
    onRescanFolders: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    observePlaylistTracks: (Long) -> Flow<List<Track>>,
    modifier: Modifier = Modifier
) {
    var selectedGroupTitle by remember { mutableStateOf<String?>(null) }
    var selectedGroupTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var trackForPlaylistPicker by remember { mutableStateOf<Track?>(null) }
    var trackForDetails by remember { mutableStateOf<Track?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // Header + SAF Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("MUSIC LIBRARY", style = MaterialTheme.typography.headlineMedium, color = IceWhite)
                Text(
                    text = "${allTracks.size} lossless & hi-res files • ${scanFolders.size} SAF folders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSteel
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onRescanFolders,
                    modifier = Modifier.size(48.dp).testTag("library_rescan_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Rescan folders", tint = SilverMist)
                }
                IconButton(
                    onClick = onPickFolder,
                    modifier = Modifier.size(48.dp).testTag("library_add_folder_button")
                ) {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = "Add SAF music folder", tint = ElectricCyan)
                }
            }
        }

        if (scanProgress.isScanning) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = ElectricCyan,
                    trackColor = GraphiteElevated
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Scanning (${scanProgress.scannedCount}): ${scanProgress.currentFile}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search title, artist, album, FLAC...", color = MutedSteel) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MutedSteel) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = SlateBorder,
                focusedContainerColor = GraphiteSurface,
                unfocusedContainerColor = GraphiteSurface,
                focusedTextColor = IceWhite,
                unfocusedTextColor = IceWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("library_search_input")
        )

        Spacer(Modifier.height(10.dp))

        // Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryTab.entries) { tab ->
                val selected = selectedTab == tab
                FilterChip(
                    selected = selected,
                    onClick = {
                        selectedGroupTitle = null
                        selectedPlaylist = null
                        onSelectTab(tab)
                    },
                    label = { Text(tab.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanMuted,
                        selectedLabelColor = ElectricCyan,
                        containerColor = GraphiteSurface,
                        labelColor = SilverMist
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = SlateBorder,
                        selectedBorderColor = ElectricCyan
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Drill-down Group Header
        if (selectedGroupTitle != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedGroupTitle = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
                }
                Text(
                    text = selectedGroupTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = { onPlayTracks(selectedGroupTracks, 0) }) {
                    Text("PLAY ALL", color = ElectricCyan)
                }
            }
            TrackLazyList(
                tracks = selectedGroupTracks,
                currentPlayingUri = currentPlayingUri,
                artworkEnabled = artworkEnabled,
                onPlay = { idx -> onPlayTracks(selectedGroupTracks, idx) },
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylist = { trackForPlaylistPicker = it },
                onShowInfo = { trackForDetails = it }
            )
        } else if (selectedPlaylist != null) {
            val playlist = selectedPlaylist!!
            val plTracks by observePlaylistTracks(playlist.id).collectAsState(initial = emptyList())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedPlaylist = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
                }
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { if (plTracks.isNotEmpty()) onPlayTracks(plTracks, 0) }) {
                    Text("PLAY ALL", color = ElectricCyan)
                }
            }
            TrackLazyList(
                tracks = plTracks,
                currentPlayingUri = currentPlayingUri,
                artworkEnabled = artworkEnabled,
                onPlay = { idx -> onPlayTracks(plTracks, idx) },
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onToggleFavorite = onToggleFavorite,
                onAddToPlaylist = { trackForPlaylistPicker = it },
                onShowInfo = { trackForDetails = it }
            )
        } else {
            when (selectedTab) {
                LibraryTab.TRACKS -> {
                    if (filteredTracks.isEmpty()) {
                        EmptyLibraryPrompt(onPickFolder = onPickFolder)
                    } else {
                        TrackLazyList(
                            tracks = filteredTracks,
                            currentPlayingUri = currentPlayingUri,
                            artworkEnabled = artworkEnabled,
                            onPlay = { idx -> onPlayTracks(filteredTracks, idx) },
                            onPlayNext = onPlayNext,
                            onAddToQueue = onAddToQueue,
                            onToggleFavorite = onToggleFavorite,
                            onAddToPlaylist = { trackForPlaylistPicker = it },
                            onShowInfo = { trackForDetails = it }
                        )
                    }
                }
                LibraryTab.ALBUMS -> {
                    val groups = remember(filteredTracks) { filteredTracks.groupBy { it.album } }
                    GroupList(
                        groups = groups,
                        icon = Icons.Filled.Album,
                        onClickGroup = { name, list ->
                            selectedGroupTitle = name
                            selectedGroupTracks = list.sortedBy { it.trackNumber }
                        }
                    )
                }
                LibraryTab.ARTISTS -> {
                    val groups = remember(filteredTracks) { filteredTracks.groupBy { it.artist } }
                    GroupList(
                        groups = groups,
                        icon = Icons.Filled.Person,
                        onClickGroup = { name, list ->
                            selectedGroupTitle = name
                            selectedGroupTracks = list
                        }
                    )
                }
                LibraryTab.GENRES -> {
                    val groups = remember(filteredTracks) { filteredTracks.groupBy { it.genre } }
                    GroupList(
                        groups = groups,
                        icon = Icons.Filled.MusicNote,
                        onClickGroup = { name, list ->
                            selectedGroupTitle = name
                            selectedGroupTracks = list
                        }
                    )
                }
                LibraryTab.FOLDERS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = onPickFolder,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add SAF Music Folder", fontWeight = FontWeight.Bold)
                            }
                        }
                        items(scanFolders) { folder ->
                            val folderTracks = allTracks.filter { it.folderUri == folder.treeUri }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedGroupTitle = folder.displayName
                                        selectedGroupTracks = folderTracks
                                    },
                                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Folder, contentDescription = null, tint = ElectricCyan)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(folder.displayName, style = MaterialTheme.typography.titleSmall, color = IceWhite)
                                        Text("${folderTracks.size} tracks", style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                                    }
                                    IconButton(onClick = { onRemoveFolder(folder.treeUri) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove folder", tint = MutedSteel)
                                    }
                                }
                            }
                        }
                    }
                }
                LibraryTab.PLAYLISTS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = { showCreatePlaylistDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Create New Playlist", fontWeight = FontWeight.Bold)
                            }
                        }
                        items(playlists) { playlist ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlaylist = playlist },
                                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = ElectricCyan)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        playlist.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = IceWhite,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete playlist", tint = MutedSteel)
                                    }
                                }
                            }
                        }
                    }
                }
                LibraryTab.FAVORITES -> {
                    TrackLazyList(
                        tracks = favoriteTracks,
                        currentPlayingUri = currentPlayingUri,
                        artworkEnabled = artworkEnabled,
                        onPlay = { idx -> onPlayTracks(favoriteTracks, idx) },
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onToggleFavorite = onToggleFavorite,
                        onAddToPlaylist = { trackForPlaylistPicker = it },
                        onShowInfo = { trackForDetails = it }
                    )
                }
                LibraryTab.RECENT -> {
                    val combined = (recentTracks + recentlyAdded).distinctBy { it.id }
                    TrackLazyList(
                        tracks = combined,
                        currentPlayingUri = currentPlayingUri,
                        artworkEnabled = artworkEnabled,
                        onPlay = { idx -> onPlayTracks(combined, idx) },
                        onPlayNext = onPlayNext,
                        onAddToQueue = onAddToQueue,
                        onToggleFavorite = onToggleFavorite,
                        onAddToPlaylist = { trackForPlaylistPicker = it },
                        onShowInfo = { trackForDetails = it }
                    )
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = GraphiteSurface,
            title = { Text("Create Playlist", color = IceWhite) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) onCreatePlaylist(name)
                    showCreatePlaylistDialog = false
                }) { Text("Create", color = ElectricCyan) }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel", color = SilverMist) }
            }
        )
    }

    trackForPlaylistPicker?.let { track ->
        AlertDialog(
            onDismissRequest = { trackForPlaylistPicker = null },
            containerColor = GraphiteSurface,
            title = { Text("Add to Playlist", color = IceWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (playlists.isEmpty()) {
                        Text("No playlists yet. Create one in the Playlists tab.", color = SilverMist)
                    } else {
                        playlists.forEach { pl ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddTrackToPlaylist(pl.id, track.id)
                                        trackForPlaylistPicker = null
                                    },
                                color = GraphiteElevated,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(pl.name, color = IceWhite, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { trackForPlaylistPicker = null }) { Text("Close", color = ElectricCyan) }
            }
        )
    }

    trackForDetails?.let { track ->
        AlertDialog(
            onDismissRequest = { trackForDetails = null },
            containerColor = GraphiteSurface,
            title = { Text(track.title, color = IceWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Artist: ${track.artist}", color = SilverMist)
                    Text("Album: ${track.album}", color = SilverMist)
                    Text("Codec: ${track.codec} (${track.mimeType})", color = ElectricCyan)
                    Text("Sample Rate: ${track.sampleRateHz} Hz", color = IceWhite)
                    Text("Bit Depth: ${if (track.bitDepth > 0) "${track.bitDepth}-bit" else "Variable/Float"}", color = IceWhite)
                    Text("Bitrate: ${track.bitrateKbps} kbps • Channels: ${track.channels}", color = SilverMist)
                    Text("Duration: ${formatDurationMs(track.durationMs)} • Size: ${formatFileSize(track.fileSizeBytes)}", color = SilverMist)
                }
            },
            confirmButton = {
                TextButton(onClick = { trackForDetails = null }) { Text("Done", color = ElectricCyan) }
            }
        )
    }
}

@Composable
private fun EmptyLibraryPrompt(onPickFolder: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text("No Audio Files Indexed Yet", style = MaterialTheme.typography.titleLarge, color = IceWhite)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tap below to grant SAF access to your local FLAC, WAV, MP3, or M4A music folder.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSteel
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onPickFolder,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Select Music Folder", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GroupList(
    groups: Map<String, List<Track>>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClickGroup: (String, List<Track>) -> Unit
) {
    val entries = remember(groups) { groups.entries.sortedBy { it.key.lowercase() } }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries, key = { it.key }) { (name, tracks) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClickGroup(name, tracks) },
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = ElectricCyan)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleSmall, color = IceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${tracks.size} tracks", style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackLazyList(
    tracks: List<Track>,
    currentPlayingUri: String?,
    artworkEnabled: Boolean,
    onPlay: (Int) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onShowInfo: (Track) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
            val isCurrent = track.uri == currentPlayingUri
            var menuExpanded by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(index) },
                color = if (isCurrent) CyanMuted.copy(alpha = 0.45f) else GraphiteSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isCurrent) ElectricCyan else SlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrackArtworkImage(
                        artworkPath = track.artworkCachePath,
                        artworkEnabled = artworkEnabled,
                        contentDescription = track.album,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isCurrent) ElectricCyan else IceWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${track.artist} • ${track.album}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSteel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            QualityTierBadge(tier = track.qualityTier)
                            Text(
                                text = "${track.badgeText} • ${formatDurationMs(track.durationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverMist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = { onToggleFavorite(track) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Toggle favorite",
                            tint = if (track.isFavorite) ElectricCyan else MutedSteel
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Track options", tint = SilverMist)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = GraphiteElevated
                        ) {
                            DropdownMenuItem(
                                text = { Text("Play Next", color = IceWhite) },
                                onClick = { menuExpanded = false; onPlayNext(track) }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Queue", color = IceWhite) },
                                onClick = { menuExpanded = false; onAddToQueue(track) }
                            )
                            DropdownMenuItem(
                                text = { Text("Add to Playlist", color = IceWhite) },
                                leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null, tint = ElectricCyan) },
                                onClick = { menuExpanded = false; onAddToPlaylist(track) }
                            )
                            DropdownMenuItem(
                                text = { Text("Audio Metadata Info", color = IceWhite) },
                                onClick = { menuExpanded = false; onShowInfo(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}
