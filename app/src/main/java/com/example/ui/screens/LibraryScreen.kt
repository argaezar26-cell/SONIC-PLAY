package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.ErrorTranslator
import com.example.data.model.LibraryFormatFilter
import com.example.data.model.LibrarySortOrder
import com.example.data.model.Track
import com.example.data.repository.ScanProgress
import com.example.ui.components.QualityTierBadge
import com.example.ui.components.TrackArtworkImage
import com.example.ui.components.UserFriendlyErrorBanner
import com.example.ui.components.formatDurationMs
import com.example.ui.components.formatFileSize
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
    playlists: List<PlaylistEntity>,
    scanFolders: List<ScanFolderEntity>,
    scanProgress: ScanProgress,
    searchQuery: String,
    selectedTab: LibraryTab,
    formatFilter: LibraryFormatFilter,
    sortOrder: LibrarySortOrder,
    selectedTrackIds: Set<Long>,
    currentPlayingUri: String?,
    artworkEnabled: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectTab: (LibraryTab) -> Unit,
    onSelectFormatFilter: (LibraryFormatFilter) -> Unit,
    onSelectSortOrder: (LibrarySortOrder) -> Unit,
    onToggleTrackSelection: (Long) -> Unit,
    onSelectAllTracks: () -> Unit,
    onClearTrackSelection: () -> Unit,
    onAddSelectedToQueue: () -> Unit,
    onFavoriteSelectedTracks: () -> Unit,
    onAddSelectedToPlaylist: (Long) -> Unit,
    onPickFolder: () -> Unit,
    onRescanFolders: () -> Unit,
    onRemoveFolder: (String) -> Unit,
    onPlayTracks: (List<Track>, Int) -> Unit,
    onShuffleTracks: (List<Track>) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    observePlaylistTracks: (Long) -> Flow<List<Track>>,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    var selectedGroupTitle by remember { mutableStateOf<String?>(null) }
    var selectedGroupTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var selectedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var trackForPlaylistPicker by remember { mutableStateOf<Track?>(null) }
    var pickPlaylistForBatch by remember { mutableStateOf(false) }
    var trackForDetails by remember { mutableStateOf<Track?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var errorDismissed by remember(scanProgress.lastError) { mutableStateOf(false) }

    val inMultiSelect = selectedTrackIds.isNotEmpty()

    BackHandler(enabled = inMultiSelect || selectedGroupTitle != null || selectedPlaylist != null) {
        when {
            inMultiSelect -> onClearTrackSelection()
            selectedGroupTitle != null -> selectedGroupTitle = null
            selectedPlaylist != null -> selectedPlaylist = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
    ) {
        // Header or Multi-Select Batch Toolbar
        if (inMultiSelect) {
            Surface(
                color = primaryContainer,
                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClearTrackSelection, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel selection", tint = IceWhite)
                    }
                    Text(
                        text = "${selectedTrackIds.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = IceWhite,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSelectAllTracks, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.SelectAll, contentDescription = "Select all", tint = accent)
                    }
                    IconButton(onClick = onAddSelectedToQueue, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = "Add selected to queue", tint = IceWhite)
                    }
                    IconButton(onClick = onFavoriteSelectedTracks, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Favorite selected", tint = accent)
                    }
                    IconButton(onClick = { pickPlaylistForBatch = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.PlaylistAdd, contentDescription = "Add selected to playlist", tint = IceWhite)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Music Library", style = MaterialTheme.typography.headlineMedium, color = IceWhite)
                    Text(
                        text = "${allTracks.size} songs • ${scanFolders.size} folders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSteel
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (filteredTracks.isNotEmpty() && selectedTab == LibraryTab.SONGS) {
                        IconButton(
                            onClick = { onToggleTrackSelection(filteredTracks.first().id) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Checklist, contentDescription = "Multi-select songs", tint = SilverMist)
                        }
                    }
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
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "Add music folder", tint = accent)
                    }
                }
            }
        }

        if (scanProgress.isScanning) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = accent,
                    trackColor = surfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Scanning (${scanProgress.scannedCount}): ${scanProgress.currentFile}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (scanProgress.lastError != null && !errorDismissed) {
            UserFriendlyErrorBanner(
                error = ErrorTranslator.toFriendlyError(scanProgress.lastError),
                onAction = onPickFolder,
                onDismiss = { errorDismissed = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search songs, artists, albums...", color = MutedSteel) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MutedSteel) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = SilverMist)
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = SlateBorder,
                focusedContainerColor = surface,
                unfocusedContainerColor = surface,
                focusedTextColor = IceWhite,
                unfocusedTextColor = IceWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("library_search_input")
        )

        Spacer(Modifier.height(8.dp))

        // 5 Streamlined Tabs
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
                        selectedContainerColor = primaryContainer,
                        selectedLabelColor = accent,
                        containerColor = surface,
                        labelColor = SilverMist
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = SlateBorder,
                        selectedBorderColor = accent
                    ),
                    modifier = Modifier.testTag("library_tab_${tab.name.lowercase()}")
                )
            }
        }

        // Format Filter + Sort Row (shown on Songs tab when no drill-down is active)
        if (selectedTab == LibraryTab.SONGS && selectedGroupTitle == null && selectedPlaylist == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(LibraryFormatFilter.entries) { filter ->
                        val active = formatFilter == filter
                        FilterChip(
                            selected = active,
                            onClick = { onSelectFormatFilter(filter) },
                            label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = primaryContainer,
                                selectedLabelColor = accent,
                                containerColor = surfaceVariant,
                                labelColor = SilverMist
                            )
                        )
                    }
                }
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.size(48.dp).testTag("library_sort_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort library", tint = accent)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = surfaceVariant
                    ) {
                        LibrarySortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (order == sortOrder) "✓ ${order.label}" else order.label,
                                        color = if (order == sortOrder) accent else IceWhite
                                    )
                                },
                                onClick = {
                                    onSelectSortOrder(order)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(6.dp))
        }

        // Drill-down Group Header
        if (selectedGroupTitle != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedGroupTitle = null }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = accent)
                }
                Text(
                    text = selectedGroupTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = { if (selectedGroupTracks.isNotEmpty()) onPlayTracks(selectedGroupTracks, 0) }) {
                    Text("PLAY ALL", color = accent)
                }
            }
            TrackLazyList(
                tracks = selectedGroupTracks,
                selectedTrackIds = selectedTrackIds,
                currentPlayingUri = currentPlayingUri,
                artworkEnabled = artworkEnabled,
                onPlay = { idx -> onPlayTracks(selectedGroupTracks, idx) },
                onToggleSelect = onToggleTrackSelection,
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
                IconButton(onClick = { selectedPlaylist = null }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = accent)
                }
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { if (plTracks.isNotEmpty()) onPlayTracks(plTracks, 0) }) {
                    Text("PLAY ALL", color = accent)
                }
            }
            if (plTracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "This playlist is empty. Tap the 3-dot menu on any song to add it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSteel
                    )
                }
            } else {
                TrackLazyList(
                    tracks = plTracks,
                    selectedTrackIds = selectedTrackIds,
                    currentPlayingUri = currentPlayingUri,
                    artworkEnabled = artworkEnabled,
                    onPlay = { idx -> onPlayTracks(plTracks, idx) },
                    onToggleSelect = onToggleTrackSelection,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onToggleFavorite = onToggleFavorite,
                    onAddToPlaylist = { trackForPlaylistPicker = it },
                    onShowInfo = { trackForDetails = it }
                )
            }
        } else {
            when (selectedTab) {
                LibraryTab.SONGS -> {
                    if (allTracks.isEmpty()) {
                        EmptyLibraryPrompt(onPickFolder = onPickFolder)
                    } else if (filteredTracks.isEmpty()) {
                        EmptyFilteredPrompt(
                            onClearFilters = {
                                onSearchQueryChange("")
                                onSelectFormatFilter(LibraryFormatFilter.ALL)
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onShuffleTracks(filteredTracks) },
                                border = BorderStroke(1.dp, SlateBorder),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(Icons.Filled.Shuffle, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Shuffle (${filteredTracks.size})", color = IceWhite, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        TrackLazyList(
                            tracks = filteredTracks,
                            selectedTrackIds = selectedTrackIds,
                            currentPlayingUri = currentPlayingUri,
                            artworkEnabled = artworkEnabled,
                            onPlay = { idx -> onPlayTracks(filteredTracks, idx) },
                            onToggleSelect = onToggleTrackSelection,
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
                LibraryTab.FOLDERS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = onPickFolder,
                                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianBlack),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Add Music Folder", fontWeight = FontWeight.Bold)
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
                                colors = CardDefaults.cardColors(containerColor = surface),
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Folder, contentDescription = null, tint = accent)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(folder.displayName, style = MaterialTheme.typography.titleSmall, color = IceWhite)
                                        Text("${folderTracks.size} songs", style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                                    }
                                    IconButton(onClick = { onRemoveFolder(folder.treeUri) }, modifier = Modifier.size(48.dp)) {
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
                                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianBlack),
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
                                colors = CardDefaults.cardColors(containerColor = surface),
                                border = BorderStroke(1.dp, SlateBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = accent)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        playlist.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = IceWhite,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onDeletePlaylist(playlist.id) }, modifier = Modifier.size(48.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete playlist", tint = MutedSteel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            containerColor = surface,
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
                }) { Text("Create", color = accent) }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel", color = SilverMist) }
            }
        )
    }

    if (trackForPlaylistPicker != null || pickPlaylistForBatch) {
        AlertDialog(
            onDismissRequest = {
                trackForPlaylistPicker = null
                pickPlaylistForBatch = false
            },
            containerColor = surface,
            title = {
                Text(
                    if (pickPlaylistForBatch) "Add ${selectedTrackIds.size} Songs to Playlist" else "Add to Playlist",
                    color = IceWhite
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (playlists.isEmpty()) {
                        Text("No playlists yet. Create one first!", color = SilverMist)
                        OutlinedButton(onClick = {
                            trackForPlaylistPicker = null
                            pickPlaylistForBatch = false
                            showCreatePlaylistDialog = true
                        }) { Text("Create Playlist", color = accent) }
                    } else {
                        playlists.forEach { pl ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (pickPlaylistForBatch) {
                                            onAddSelectedToPlaylist(pl.id)
                                            pickPlaylistForBatch = false
                                        } else {
                                            trackForPlaylistPicker?.let { onAddTrackToPlaylist(pl.id, it.id) }
                                            trackForPlaylistPicker = null
                                        }
                                    },
                                color = surfaceVariant,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(pl.name, color = IceWhite, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    trackForPlaylistPicker = null
                    pickPlaylistForBatch = false
                }) { Text("Close", color = accent) }
            }
        )
    }

    trackForDetails?.let { track ->
        AlertDialog(
            onDismissRequest = { trackForDetails = null },
            containerColor = surface,
            title = { Text(track.title, color = IceWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Artist: ${track.artist}", color = SilverMist)
                    Text("Album: ${track.album}", color = SilverMist)
                    Text("Format: ${track.codec} (${track.qualityTier.label})", color = accent)
                    Text("Sample Rate: ${track.sampleRateHz} Hz", color = IceWhite)
                    Text("Bit Depth: ${if (track.bitDepth > 0) "${track.bitDepth}-bit" else "Variable/Float"}", color = IceWhite)
                    Text("Bitrate: ${track.bitrateKbps} kbps • Channels: ${track.channels}", color = SilverMist)
                    Text("Duration: ${formatDurationMs(track.durationMs)} • Size: ${formatFileSize(track.fileSizeBytes)}", color = SilverMist)
                }
            },
            confirmButton = {
                TextButton(onClick = { trackForDetails = null }) { Text("Done", color = accent) }
            }
        )
    }
}

@Composable
private fun EmptyLibraryPrompt(onPickFolder: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = accent, modifier = Modifier.size(56.dp))
            Spacer(Modifier.height(12.dp))
            Text("Your Music Library is Empty", style = MaterialTheme.typography.titleLarge, color = IceWhite)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Select a music folder on your phone to index FLAC, WAV, MP3, or M4A tracks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSteel
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onPickFolder,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianBlack),
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
private fun EmptyFilteredPrompt(onClearFilters: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MutedSteel, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(10.dp))
            Text("No Matching Songs", style = MaterialTheme.typography.titleMedium, color = IceWhite)
            Spacer(Modifier.height(6.dp))
            Text("Try clearing your search or format filter.", style = MaterialTheme.typography.bodySmall, color = MutedSteel)
            Spacer(Modifier.height(14.dp))
            OutlinedButton(onClick = onClearFilters) {
                Text("Reset Filters", color = accent)
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
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
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
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, SlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = accent)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleSmall, color = IceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${tracks.size} songs", style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackLazyList(
    tracks: List<Track>,
    selectedTrackIds: Set<Long>,
    currentPlayingUri: String?,
    artworkEnabled: Boolean,
    onPlay: (Int) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onShowInfo: (Track) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val inSelectionMode = selectedTrackIds.isNotEmpty()

    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(tracks, key = { _, t -> t.id }) { index, track ->
            val isCurrent = track.uri == currentPlayingUri
            val isSelected = track.id in selectedTrackIds
            var menuExpanded by remember { mutableStateOf(false) }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (inSelectionMode) onToggleSelect(track.id) else onPlay(index)
                        },
                        onLongClick = { onToggleSelect(track.id) }
                    ),
                color = when {
                    isSelected -> primaryContainer
                    isCurrent -> primaryContainer.copy(alpha = 0.5f)
                    else -> surface
                },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (isSelected || isCurrent) accent else SlateBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (inSelectionMode) {
                        Icon(
                            imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                            contentDescription = if (isSelected) "Selected" else "Not selected",
                            tint = if (isSelected) accent else MutedSteel,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                    }
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
                            color = if (isCurrent) accent else IceWhite,
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

                    if (!inSelectionMode) {
                        IconButton(
                            onClick = { onToggleFavorite(track) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Toggle favorite",
                                tint = if (track.isFavorite) accent else MutedSteel
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
                                containerColor = surfaceVariant
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
                                    leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null, tint = accent) },
                                    onClick = { menuExpanded = false; onAddToPlaylist(track) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Select Multiple", color = IceWhite) },
                                    onClick = { menuExpanded = false; onToggleSelect(track.id) }
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
}
