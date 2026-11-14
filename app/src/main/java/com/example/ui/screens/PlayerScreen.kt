package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.audio.controller.PlaybackUiState
import com.example.audio.routing.AudioRoutingState
import com.example.data.db.PlaylistEntity
import com.example.data.model.ArtworkStyle
import com.example.data.model.OutputProfileType
import com.example.data.model.PlayerLayoutMode
import com.example.data.model.Track
import com.example.data.model.VisualizerFrame
import com.example.ui.components.AudioQualityExplainerSheetContent
import com.example.ui.components.AudioSpecPill
import com.example.ui.components.LyricsPanel
import com.example.ui.components.QualityTierBadge
import com.example.ui.components.RealTimeVisualizer
import com.example.ui.components.StyledPlayerArtwork
import com.example.ui.components.UserFriendlyErrorBanner
import com.example.ui.components.VisualizerMode
import com.example.ui.components.formatDurationMs
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverMist
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.VerifiedEmerald
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlayerScreen(
    playbackState: PlaybackUiState,
    routingState: AudioRoutingState,
    visualizerFrame: VisualizerFrame,
    visualizerEnabled: Boolean,
    artworkEnabled: Boolean,
    artworkStyle: ArtworkStyle,
    playerLayout: PlayerLayoutMode,
    gesturesEnabled: Boolean,
    bitTransparentRequested: Boolean,
    outputProfile: OutputProfileType,
    playlists: List<PlaylistEntity>,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSelectOutputProfile: (OutputProfileType) -> Unit,
    onJumpToQueueIndex: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onShuffleQueue: () -> Unit,
    onClearQueue: () -> Unit,
    onAddTrackToPlaylist: (Long, Long) -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onToggleBitTransparent: (Boolean) -> Unit,
    onClearError: () -> Unit,
    onGoToLibrary: () -> Unit,
    onGoToEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val track = playbackState.currentTrack
    var visualizerMode by remember { mutableStateOf(VisualizerMode.SPECTRUM_BARS) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showOutputSheet by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showContextSheet by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }

    val compactMode = playerLayout == PlayerLayoutMode.COMPACT_ONE_HAND

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top status bar: Output Switcher & Sound Mode & Tools
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (playbackState.audioInfo.isBitTransparentActive) VerifiedEmerald.copy(alpha = 0.14f) else primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (playbackState.audioInfo.isBitTransparentActive) VerifiedEmerald.copy(alpha = 0.6f) else accent.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.clickable { onToggleBitTransparent(!bitTransparentRequested) }
                ) {
                    Text(
                        text = if (playbackState.audioInfo.isBitTransparentActive) "PURE DIRECT" else "EQ ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (playbackState.audioInfo.isBitTransparentActive) VerifiedEmerald else accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    color = surface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.clickable { showOutputSheet = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Speaker, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                        Text(
                            text = routingState.activeDevice?.routeCategory?.displayName?.take(16) ?: "Output",
                            style = MaterialTheme.typography.labelSmall,
                            color = SilverMist,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = { showLyrics = !showLyrics },
                    modifier = Modifier.size(48.dp).testTag("player_lyrics_button")
                ) {
                    Icon(
                        Icons.Filled.Lyrics,
                        contentDescription = "Toggle lyrics",
                        tint = if (showLyrics) accent else SilverMist
                    )
                }
                IconButton(
                    onClick = { showSleepDialog = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = "Sleep timer",
                        tint = if (playbackState.sleepTimerRemainingMs > 0L) accent else SilverMist
                    )
                }
                IconButton(
                    onClick = { if (track != null) showQualitySheet = true },
                    modifier = Modifier.size(48.dp).testTag("player_audio_info_button")
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "Explain audio quality", tint = accent)
                }
            }
        }

        playbackState.friendlyError?.let { err ->
            Spacer(Modifier.height(6.dp))
            UserFriendlyErrorBanner(
                error = err,
                onAction = {
                    onClearError()
                    onGoToLibrary()
                },
                onDismiss = onClearError
            )
        }

        Spacer(Modifier.height(if (compactMode) 6.dp else 10.dp))

        // Interactive Artwork / Lyrics Stage (Swipe horizontal to skip, Tap to flip/options, Long-press for track menu)
        var dragAccum by remember { mutableFloatStateOf(0f) }
        val artFraction = when {
            compactMode -> 0.62f
            artworkStyle == ArtworkStyle.FULL_BLEED -> 0.96f
            else -> 0.84f
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(artFraction)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(if (artworkStyle == ArtworkStyle.VINYL_DISC && !showLyrics) 200.dp else 24.dp))
                .pointerInput(gesturesEnabled, track?.id) {
                    if (!gesturesEnabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onHorizontalDrag = { _, dragAmount -> dragAccum += dragAmount },
                        onDragEnd = {
                            if (dragAccum < -90f) onNext()
                            else if (dragAccum > 90f) onPrevious()
                            dragAccum = 0f
                        }
                    )
                }
                .combinedClickable(
                    onClick = {
                        if (track != null) showLyrics = !showLyrics
                    },
                    onLongClick = {
                        if (track != null) showContextSheet = true
                    }
                )
        ) {
            if (showLyrics && track != null) {
                LyricsPanel(
                    lyrics = track.lyrics,
                    positionMs = playbackState.positionMs,
                    onSeekTo = onSeekTo,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                )
            } else {
                StyledPlayerArtwork(
                    artworkPath = track?.artworkCachePath,
                    artworkEnabled = artworkEnabled,
                    artworkStyle = artworkStyle,
                    isPlaying = playbackState.isPlaying,
                    contentDescription = track?.album ?: "No track selected",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (track != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (gesturesEnabled) "Swipe artwork for Next/Prev • Tap for Lyrics • Long-press for options" else "Tap artwork for Lyrics • Long-press for options",
                style = MaterialTheme.typography.labelSmall,
                color = MutedSteel
            )
        }

        Spacer(Modifier.height(12.dp))

        if (track == null) {
            Text(
                text = "Nothing Playing Yet",
                style = MaterialTheme.typography.headlineMedium,
                color = IceWhite
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Choose any song from your Library to start listening.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSteel
            )
            Spacer(Modifier.height(14.dp))
            TextButton(onClick = onGoToLibrary) {
                Text("Open Music Library", color = accent)
            }
        } else {
            // Audio Quality Explainer Badge Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QualityTierBadge(tier = track.qualityTier)
                AudioSpecPill(
                    text = "${track.badgeText} (?)",
                    highlighted = true,
                    modifier = Modifier
                        .testTag("player_quality_pill")
                        .clickable { showQualitySheet = true }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Track Title / Artist / Album + Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = IceWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${track.artist} — ${track.album}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverMist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { onToggleFavorite(track) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (track.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (track.isFavorite) accent else MutedSteel
                    )
                }
            }

            if (visualizerEnabled && !compactMode) {
                Spacer(Modifier.height(10.dp))
                RealTimeVisualizer(
                    frame = visualizerFrame,
                    mode = visualizerMode,
                    isPlaying = playbackState.isPlaying,
                    modifier = Modifier.clickable {
                        val nextOrd = (visualizerMode.ordinal + 1) % VisualizerMode.entries.size
                        visualizerMode = VisualizerMode.entries[nextOrd]
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            // SeekBar & Timestamps
            val dur = playbackState.durationMs.coerceAtLeast(1L)
            val currentPos = playbackState.positionMs.coerceIn(0L, dur)
            val sliderVal = if (isScrubbing) scrubProgress else (currentPos.toFloat() / dur.toFloat())

            Slider(
                value = sliderVal,
                onValueChange = {
                    isScrubbing = true
                    scrubProgress = it
                },
                onValueChangeFinished = {
                    isScrubbing = false
                    onSeekTo((scrubProgress * dur).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().testTag("player_seek_bar")
            )

            val displayedPos = if (isScrubbing) (scrubProgress * dur).toLong() else currentPos
            val remaining = (dur - displayedPos).coerceAtLeast(0L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatDurationMs(displayedPos), style = MaterialTheme.typography.labelMedium, color = SilverMist)
                Text("-${formatDurationMs(remaining)}", style = MaterialTheme.typography.labelMedium, color = MutedSteel)
            }

            Spacer(Modifier.height(8.dp))

            // Transport Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(48.dp).testTag("player_shuffle_button")
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffleEnabled) accent else MutedSteel
                    )
                }
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(52.dp).testTag("player_prev_button")
                ) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = IceWhite, modifier = Modifier.size(34.dp))
                }
                Surface(
                    onClick = onTogglePlayPause,
                    shape = CircleShape,
                    color = accent,
                    modifier = Modifier.size(68.dp).testTag("player_play_pause_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(52.dp).testTag("player_next_button")
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = IceWhite, modifier = Modifier.size(34.dp))
                }
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.size(48.dp).testTag("player_repeat_button")
                ) {
                    val icon = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat
                    val tint = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) accent else MutedSteel
                    Icon(icon, contentDescription = "Repeat", tint = tint)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Bottom Action Row: Speed + Equalizer + Queue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { showSpeedDialog = true },
                    color = surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.weight(0.85f).height(48.dp).testTag("player_speed_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Speed, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${String.format(Locale.US, "%.2f", playbackState.playbackSpeed).trimEnd('0').trimEnd('.')}x",
                            style = MaterialTheme.typography.labelLarge,
                            color = IceWhite
                        )
                    }
                }
                Surface(
                    onClick = onGoToEqualizer,
                    color = surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.weight(0.95f).height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Equalizer", style = MaterialTheme.typography.labelLarge, color = IceWhite)
                    }
                }
                Surface(
                    onClick = { showQueueSheet = true },
                    color = surface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.weight(1.1f).height(48.dp).testTag("player_queue_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Queue (${playbackState.queue.size})", style = MaterialTheme.typography.labelLarge, color = IceWhite)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // Audio Quality Explainer Sheet
    if (showQualitySheet && track != null) {
        ModalBottomSheet(
            onDismissRequest = { showQualitySheet = false },
            containerColor = surface
        ) {
            AudioQualityExplainerSheetContent(track = track, audioInfo = playbackState.audioInfo)
        }
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Queue (${playbackState.queue.size})", style = MaterialTheme.typography.titleLarge, color = IceWhite)
                    Row {
                        IconButton(onClick = onShuffleQueue, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle queue", tint = accent)
                        }
                        IconButton(onClick = { onClearQueue(); showQueueSheet = false }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear queue", tint = SilverMist)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(playbackState.queue) { idx, qTrack ->
                        val active = idx == playbackState.currentIndex
                        Surface(
                            color = if (active) primaryContainer else surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (active) accent else SlateBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onJumpToQueueIndex(idx) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (active) accent else MutedSteel,
                                    modifier = Modifier.width(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(qTrack.title, style = MaterialTheme.typography.titleSmall, color = IceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${qTrack.artist} • ${qTrack.badgeText}", style = MaterialTheme.typography.bodySmall, color = SilverMist, maxLines = 1)
                                }
                                IconButton(onClick = { onRemoveFromQueue(idx) }, modifier = Modifier.size(44.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove from queue", tint = MutedSteel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Output Switcher Sheet
    if (showOutputSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOutputSheet = false },
            containerColor = surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Audio Output Device", style = MaterialTheme.typography.titleLarge, color = IceWhite)
                Text(
                    text = "Active hardware: ${routingState.activeDevice?.productName ?: "System Default"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accent
                )
                OutputProfileType.entries.forEach { profile ->
                    val selected = outputProfile == profile
                    Surface(
                        color = if (selected) primaryContainer else surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (selected) accent else SlateBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectOutputProfile(profile)
                                showOutputSheet = false
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(profile.displayName, style = MaterialTheme.typography.titleSmall, color = IceWhite)
                            Text(profile.subtitle, style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // Contextual Long-Press Track Actions Sheet
    if (showContextSheet && track != null) {
        ModalBottomSheet(
            onDismissRequest = { showContextSheet = false },
            containerColor = surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(track.title, style = MaterialTheme.typography.titleMedium, color = IceWhite)
                Text(track.artist, style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { showContextSheet = false; showLyrics = !showLyrics },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.Lyrics, contentDescription = null, tint = accent)
                    Spacer(Modifier.width(8.dp))
                    Text(if (showLyrics) "Show Album Artwork" else "Show Lyrics", color = IceWhite)
                }
                OutlinedButton(
                    onClick = { showContextSheet = false; showPlaylistDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.PlaylistAdd, contentDescription = null, tint = accent)
                    Spacer(Modifier.width(8.dp))
                    Text("Add to Playlist", color = IceWhite)
                }
                OutlinedButton(
                    onClick = { showContextSheet = false; showQualitySheet = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = accent)
                    Spacer(Modifier.width(8.dp))
                    Text("Explain Audio Quality", color = IceWhite)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Add Current Track to Playlist Dialog
    if (showPlaylistDialog && track != null) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            containerColor = surface,
            title = { Text("Add to Playlist", color = IceWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (playlists.isEmpty()) {
                        Text("No playlists yet. Create one in Library -> Playlists.", color = SilverMist)
                    } else {
                        playlists.forEach { pl ->
                            Surface(
                                color = surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAddTrackToPlaylist(pl.id, track.id)
                                        showPlaylistDialog = false
                                    }
                            ) {
                                Text(pl.name, color = IceWhite, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistDialog = false }) { Text("Close", color = accent) }
            }
        )
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = surface,
            title = { Text("Playback Speed", color = IceWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (bitTransparentRequested) {
                        Text(
                            text = "Speed is locked to 1.0x while Pure Direct (Bit-Transparent) mode is active.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SilverMist
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.75f, 0.9f, 1.0f, 1.15f, 1.25f, 1.5f).forEach { spd ->
                            val label = if (spd == 1.0f) "1.0x" else "${String.format(Locale.US, "%.2f", spd).trimEnd('0').trimEnd('.')}x"
                            FilterChip(
                                selected = playbackState.playbackSpeed == spd,
                                onClick = {
                                    if (bitTransparentRequested && spd != 1.0f) {
                                        onToggleBitTransparent(false)
                                    }
                                    onSetPlaybackSpeed(spd)
                                    showSpeedDialog = false
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryContainer,
                                    selectedLabelColor = accent
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Close", color = accent) }
            }
        )
    }

    // Sleep Timer Dialog
    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            containerColor = surface,
            title = { Text("Sleep Timer", color = IceWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (playbackState.sleepTimerRemainingMs > 0L) {
                        Text(
                            "Active: ${formatDurationMs(playbackState.sleepTimerRemainingMs)} remaining",
                            color = accent
                        )
                    }
                    listOf(0 to "Off", 15 to "15 Minutes", 30 to "30 Minutes", 45 to "45 Minutes", 60 to "60 Minutes").forEach { (min, label) ->
                        Surface(
                            color = surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSetSleepTimerMinutes(min)
                                    showSleepDialog = false
                                }
                        ) {
                            Text(label, color = IceWhite, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepDialog = false }) { Text("Close", color = accent) }
            }
        )
    }
}
