package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.audio.controller.PlaybackUiState
import com.example.audio.routing.AudioRoutingState
import com.example.data.model.PlaybackAudioInfo
import com.example.data.model.Track
import com.example.data.model.VisualizerFrame
import com.example.ui.components.AudioSpecPill
import com.example.ui.components.QualityTierBadge
import com.example.ui.components.RealTimeVisualizer
import com.example.ui.components.TrackArtworkImage
import com.example.ui.components.VisualizerMode
import com.example.ui.components.formatDurationMs
import com.example.ui.components.formatFileSize
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverMist
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.VerifiedEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playbackState: PlaybackUiState,
    routingState: AudioRoutingState,
    visualizerFrame: VisualizerFrame,
    visualizerEnabled: Boolean,
    artworkEnabled: Boolean,
    bitTransparentRequested: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onJumpToQueueIndex: (Int) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onToggleBitTransparent: (Boolean) -> Unit,
    onGoToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playbackState.currentTrack
    var visualizerMode by remember { mutableStateOf(VisualizerMode.SPECTRUM_BARS) }
    var showAudioInfoSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top status bar: Output & Bit-Transparent indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (playbackState.audioInfo.isBitTransparentActive) VerifiedEmerald.copy(alpha = 0.14f) else CyanMuted,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    1.dp,
                    if (playbackState.audioInfo.isBitTransparentActive) VerifiedEmerald.copy(alpha = 0.6f) else ElectricCyan.copy(alpha = 0.45f)
                ),
                modifier = Modifier.clickable { onToggleBitTransparent(!bitTransparentRequested) }
            ) {
                Text(
                    text = if (playbackState.audioInfo.isBitTransparentActive) "BIT-TRANSPARENT" else "DSP ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (playbackState.audioInfo.isBitTransparentActive) VerifiedEmerald else ElectricCyan,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { showSleepDialog = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.Timer,
                        contentDescription = "Sleep timer",
                        tint = if (playbackState.sleepTimerRemainingMs > 0L) ElectricCyan else SilverMist
                    )
                }
                IconButton(
                    onClick = { showAudioInfoSheet = true },
                    modifier = Modifier.size(48.dp).testTag("player_audio_info_button")
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "Hi-Res audio info", tint = ElectricCyan)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Large Album Artwork
        Box(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.5.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .background(GraphiteSurface)
        ) {
            TrackArtworkImage(
                artworkPath = track?.artworkCachePath,
                artworkEnabled = artworkEnabled,
                contentDescription = track?.album ?: "No track selected",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(16.dp))

        if (track == null) {
            Text(
                text = "No Track Loaded",
                style = MaterialTheme.typography.headlineMedium,
                color = IceWhite
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Add a music folder in Library to start lossless FLAC or WAV playback.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSteel
            )
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onGoToLibrary) {
                Text("Browse Library", color = ElectricCyan)
            }
        } else {
            // Audio Quality Badge Row (real metadata)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QualityTierBadge(tier = track.qualityTier)
                AudioSpecPill(
                    text = track.badgeText,
                    highlighted = true,
                    modifier = Modifier.clickable { showAudioInfoSheet = true }
                )
            }

            Spacer(Modifier.height(14.dp))

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
                        tint = if (track.isFavorite) ElectricCyan else MutedSteel
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Real-time Audio Visualizer
            if (visualizerEnabled) {
                RealTimeVisualizer(
                    frame = visualizerFrame,
                    mode = visualizerMode,
                    modifier = Modifier.clickable {
                        val nextOrd = (visualizerMode.ordinal + 1) % VisualizerMode.entries.size
                        visualizerMode = VisualizerMode.entries[nextOrd]
                    }
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VISUALIZER: ${visualizerMode.label.uppercase()} (tap to cycle)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedSteel
                    )
                    Text(
                        text = playbackState.audioInfo.outputRouteLabel.substringBefore(" ("),
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

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
                    thumbColor = ElectricCyan,
                    activeTrackColor = ElectricCyan,
                    inactiveTrackColor = GraphiteElevated
                ),
                modifier = Modifier.fillMaxWidth().testTag("player_seek_bar")
            )

            val displayedPos = if (isScrubbing) (scrubProgress * dur).toLong() else currentPos
            val remaining = (dur - displayedPos).coerceAtLeast(0L)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDurationMs(displayedPos),
                    style = MaterialTheme.typography.labelMedium,
                    color = SilverMist
                )
                Text(
                    text = "-${formatDurationMs(remaining)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSteel
                )
            }

            Spacer(Modifier.height(10.dp))

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
                        tint = if (playbackState.shuffleEnabled) ElectricCyan else MutedSteel
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
                    color = ElectricCyan,
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
                    val tint = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) ElectricCyan else MutedSteel
                    Icon(icon, contentDescription = "Repeat", tint = tint)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Bottom Action Row: Signal Chain + Queue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = { showAudioInfoSheet = true },
                    color = GraphiteSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Signal Chain", style = MaterialTheme.typography.labelLarge, color = IceWhite)
                    }
                }
                Surface(
                    onClick = { showQueueSheet = true },
                    color = GraphiteSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SlateBorder),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("player_queue_button")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Queue (${playbackState.queue.size})", style = MaterialTheme.typography.labelLarge, color = IceWhite)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    // 4-Layer Hi-Res Audio Information Bottom Sheet
    if (showAudioInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAudioInfoSheet = false },
            containerColor = GraphiteSurface
        ) {
            SignalChainSheetContent(
                info = playbackState.audioInfo,
                routingState = routingState
            )
        }
    }

    // Queue Bottom Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            containerColor = GraphiteSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("PLAYBACK QUEUE (${playbackState.queue.size})", style = MaterialTheme.typography.titleLarge, color = IceWhite)
                Spacer(Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(playbackState.queue) { idx, qTrack ->
                        val active = idx == playbackState.currentIndex
                        Surface(
                            color = if (active) CyanMuted else GraphiteElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (active) ElectricCyan else SlateBorder),
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
                                    color = if (active) ElectricCyan else MutedSteel,
                                    modifier = Modifier.width(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(qTrack.title, style = MaterialTheme.typography.titleSmall, color = IceWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${qTrack.artist} • ${qTrack.badgeText}", style = MaterialTheme.typography.bodySmall, color = SilverMist, maxLines = 1)
                                }
                                IconButton(onClick = { onRemoveFromQueue(idx) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove from queue", tint = MutedSteel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            containerColor = GraphiteSurface,
            title = { Text("Sleep Timer", color = IceWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (playbackState.sleepTimerRemainingMs > 0L) {
                        Text(
                            "Active: ${formatDurationMs(playbackState.sleepTimerRemainingMs)} remaining",
                            color = ElectricCyan
                        )
                    }
                    listOf(0 to "Off", 15 to "15 Minutes", 30 to "30 Minutes", 45 to "45 Minutes", 60 to "60 Minutes").forEach { (min, label) ->
                        Surface(
                            color = GraphiteElevated,
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
                TextButton(onClick = { showSleepDialog = false }) { Text("Close", color = ElectricCyan) }
            }
        )
    }
}

@Composable
private fun SignalChainSheetContent(
    info: PlaybackAudioInfo,
    routingState: AudioRoutingState
) {
    val active = routingState.activeDevice
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("HI-RES SIGNAL CHAIN TELEMETRY", style = MaterialTheme.typography.titleLarge, color = IceWhite)
        Text(
            text = "Source sample rate is reported independently from Android mixer and hardware capabilities.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSteel
        )

        InfoGroupCard(
            title = "1. SOURCE AUDIO",
            rows = listOf(
                "Codec" to info.sourceCodec,
                "Sample Rate" to if (info.sourceSampleRateHz > 0) "${info.sourceSampleRateHz} Hz" else "—",
                "Bit Depth" to if (info.sourceBitDepth > 0) "${info.sourceBitDepth}-bit" else "Variable",
                "Bitrate" to if (info.sourceBitrateKbps > 0) "${info.sourceBitrateKbps} kbps" else "—",
                "Channels" to if (info.sourceChannels > 0) "${info.sourceChannels} ch" else "—",
                "Duration" to formatDurationMs(info.sourceDurationMs),
                "File Size" to formatFileSize(info.sourceFileSizeBytes),
                "Classification" to info.qualityTier.label
            )
        )

        InfoGroupCard(
            title = "2. DECODED AUDIO",
            rows = listOf(
                "Decoder Used" to info.decoderUsed,
                "Decoded PCM" to info.decodedPcmEncoding,
                "Decoded Rate" to if (info.decodedSampleRateHz > 0) "${info.decodedSampleRateHz} Hz" else "—",
                "Active DSP" to info.activeDspSummary,
                "Processing State" to info.processingState
            )
        )

        InfoGroupCard(
            title = "3. ANDROID OUTPUT CONFIGURATION",
            rows = listOf(
                "Requested Rate" to if (info.requestedSampleRateHz > 0) "${info.requestedSampleRateHz} Hz" else "—",
                "Output Route" to info.outputRouteLabel,
                "Mixer Status" to (active?.mixerStatus?.label ?: "Unknown"),
                "Active Mixer Format" to (active?.activeMixerFormatSummary ?: "Standard Android AudioTrack")
            )
        )

        InfoGroupCard(
            title = "4. HARDWARE-SUPPORTED CAPABILITIES",
            rows = listOf(
                "Device" to (active?.productName ?: "Unknown"),
                "Supported Rates" to (active?.supportedSampleRates?.takeIf { it.isNotEmpty() }?.joinToString { "$it Hz" } ?: "Dynamic / System-managed"),
                "Encodings" to (active?.supportedEncodings?.takeIf { it.isNotEmpty() }?.joinToString() ?: "PCM 16-bit"),
                "Channels" to (active?.supportedChannelCounts?.takeIf { it.isNotEmpty() }?.joinToString() ?: "2"),
                "API 34+ Bit-Perfect Mixer" to if (active?.bitPerfectSupportedByMixer == true) "Reported Supported" else "Not reported / Standard Mixer"
            )
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoGroupCard(title: String, rows: List<Pair<String, String>>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GraphiteElevated),
        border = BorderStroke(1.dp, SlateBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
            rows.forEach { (k, v) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(k, style = MaterialTheme.typography.bodySmall, color = MutedSteel, modifier = Modifier.weight(0.42f))
                    Text(v, style = MaterialTheme.typography.labelMedium, color = IceWhite, modifier = Modifier.weight(0.58f))
                }
            }
        }
    }
}
