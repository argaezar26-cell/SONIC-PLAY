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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.data.model.SoundProfileId
import com.example.data.model.Track
import com.example.data.prefs.AppPreferencesState
import com.example.ui.components.AudioSpecPill
import com.example.ui.components.QualityTierBadge
import com.example.ui.components.TrackArtworkImage
import com.example.ui.theme.AmberPeak
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.GraphiteHighlight
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverMist
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.VerifiedEmerald

@Composable
fun HomeScreen(
    allTracks: List<Track>,
    recentTracks: List<Track>,
    favoriteTracks: List<Track>,
    playbackState: PlaybackUiState,
    routingState: AudioRoutingState,
    preferences: AppPreferencesState,
    onPickFolder: () -> Unit,
    onPlayTrackFromList: (List<Track>, Int) -> Unit,
    onSelectSoundProfile: (SoundProfileId) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenTestLab: () -> Unit,
    onOpenQualityDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.img_sonicflow_hero),
                    contentDescription = "SONICFLOW acoustic wave banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    ObsidianBlack.copy(alpha = 0.25f),
                                    ObsidianBlack.copy(alpha = 0.72f),
                                    ObsidianBlack
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AudioSpecPill(
                            text = if (preferences.dspSettings.bitTransparentMode) "BIT-TRANSPARENT" else "64-BIT DSP",
                            highlighted = true
                        )
                        AudioSpecPill(
                            text = "${allTracks.size} TRACKS",
                            highlighted = false
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "SONICFLOW",
                        style = MaterialTheme.typography.displayLarge,
                        color = IceWhite
                    )
                    Text(
                        text = "Precision Local Hi-Res Audio Engine",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverMist
                    )
                }
            }
        }

        // Empty Library Quick Onboarding
        if (allTracks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Select Your Local Music Folder",
                            style = MaterialTheme.typography.titleLarge,
                            color = IceWhite
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Uses Android Storage Access Framework (SAF) for zero-bloat local FLAC, WAV, MP3, AAC, OGG, and OPUS scanning — no cloud, no account, no excessive storage permissions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SilverMist
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = onPickFolder,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ObsidianBlack
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("home_pick_folder_button")
                        ) {
                            Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add Music Folder (SAF)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Active Output Hardware & Signal Chain Card
        item {
            val active = routingState.activeDevice
            val icon = when (active?.routeCategory) {
                OutputRouteCategory.EXTERNAL_USB_DAC,
                OutputRouteCategory.USB_TYPE_C_ADAPTER -> Icons.Filled.Usb
                OutputRouteCategory.BLUETOOTH -> Icons.Filled.BluetoothAudio
                OutputRouteCategory.WIRED_HEADPHONES -> Icons.Filled.Headphones
                else -> Icons.Filled.Speaker
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        if (playbackState.currentTrack != null) onOpenPlayer()
                    },
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                color = CyanMuted,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(icon, contentDescription = null, tint = ElectricCyan)
                                }
                            }
                            Column {
                                Text(
                                    text = "ACTIVE OUTPUT ROUTE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSteel
                                )
                                Text(
                                    text = active?.productName ?: "Detecting Audio Output...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = IceWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (active?.bitPerfectSupportedByMixer == true) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Bit-perfect mixer supported",
                                tint = VerifiedEmerald
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AudioSpecPill(
                            text = active?.routeCategory?.displayName?.take(22) ?: "System Route",
                            modifier = Modifier.weight(1f)
                        )
                        val maxSr = active?.supportedSampleRates?.maxOrNull() ?: 0
                        AudioSpecPill(
                            text = if (maxSr > 0) "Up to ${maxSr / 1000} kHz" else "Auto Rate"
                        )
                    }

                    if (playbackState.currentTrack != null) {
                        val t = playbackState.currentTrack
                        Surface(
                            color = GraphiteElevated,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "${t.title} • ${t.badgeText}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = IceWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sound Profile Lab Quick Selector
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SOUND PROFILE LAB",
                        style = MaterialTheme.typography.labelLarge,
                        color = SilverMist
                    )
                    Text(
                        text = preferences.outputProfile.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(SoundProfileId.entries) { profile ->
                        val selected = preferences.soundProfile == profile
                        Surface(
                            modifier = Modifier
                                .width(156.dp)
                                .testTag("sound_profile_${profile.name.lowercase()}")
                                .clickable { onSelectSoundProfile(profile) },
                            color = if (selected) CyanMuted else GraphiteSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selected) ElectricCyan else SlateBorder
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = profile.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) ElectricCyan else IceWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (selected) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = "Active",
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = profile.tagline,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SilverMist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Note: Disable system JBL/DTS enhancements if applying SONICFLOW EQ to prevent double-DSP stacking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberPeak.copy(alpha = 0.9f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
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

        // Favorites Shelf
        if (favoriteTracks.isNotEmpty()) {
            item {
                HorizontalTrackShelf(
                    title = "FAVORITES",
                    tracks = favoriteTracks,
                    artworkEnabled = preferences.albumArtworkEnabled,
                    onPlayTrack = { idx -> onPlayTrackFromList(favoriteTracks, idx) }
                )
            }
        }

        // Engineering Labs Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "AUDIO ENGINEERING LABS",
                    style = MaterialTheme.typography.labelLarge,
                    color = SilverMist
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LabQuickCard(
                        title = "Audio Test Lab",
                        subtitle = "L/R/Mono, 20Hz–20kHz sweep & clipping tones",
                        icon = Icons.Filled.Science,
                        accent = ElectricCyan,
                        onClick = onOpenTestLab,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_test_lab_card")
                    )
                    LabQuickCard(
                        title = "Quality Tests",
                        subtitle = "FLAC, 96/192kHz, USB DAC & DSP verification",
                        icon = Icons.Filled.Verified,
                        accent = ElectricBlue,
                        onClick = onOpenQualityDiagnostics,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_quality_tests_card")
                    )
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
                        .width(148.dp)
                        .clickable { onPlayTrack(index) },
                    color = GraphiteSurface,
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
                                .height(120.dp)
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

@Composable
private fun LabQuickCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = IceWhite)
            Spacer(Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MutedSteel)
        }
    }
}
