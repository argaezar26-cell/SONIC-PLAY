package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.routing.AudioRoutingState
import com.example.data.model.AccentColorOption
import com.example.data.model.AppThemeStyle
import com.example.data.model.ArtworkStyle
import com.example.data.model.BufferPreset
import com.example.data.model.MixerVerificationStatus
import com.example.data.model.OutputProfileType
import com.example.data.model.PlayerLayoutMode
import com.example.data.prefs.AppPreferencesState
import com.example.ui.components.AudioSpecPill
import com.example.ui.theme.AmberPeak
import com.example.ui.theme.ClipCrimson
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverMist
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.VerifiedEmerald

@Composable
fun SettingsScreen(
    preferences: AppPreferencesState,
    routingState: AudioRoutingState,
    mixerCheckResult: MixerVerificationStatus?,
    onSetOutputProfile: (OutputProfileType) -> Unit,
    onRefreshDevices: () -> Unit,
    onVerifyBitPerfectMixer: () -> Unit,
    onSetRememberEqPerDevice: (Boolean) -> Unit,
    onSetGapless: (Boolean) -> Unit,
    onSetFadeInOut: (Boolean) -> Unit,
    onSetCrossfadeSeconds: (Int) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetAutoPlayOnSelect: (Boolean) -> Unit,
    onSetAudioFocus: (Boolean) -> Unit,
    onSetResumeOnRestart: (Boolean) -> Unit,
    onSetVisualizerEnabled: (Boolean) -> Unit,
    onSetArtworkEnabled: (Boolean) -> Unit,
    onSetNotificationEnabled: (Boolean) -> Unit,
    onSetBufferPreset: (BufferPreset) -> Unit,
    onSetPreferDirectRate: (Boolean) -> Unit,
    onSetThemeStyle: (AppThemeStyle) -> Unit,
    onSetAccentColor: (AccentColorOption) -> Unit,
    onSetArtworkStyle: (ArtworkStyle) -> Unit,
    onSetPlayerLayout: (PlayerLayoutMode) -> Unit,
    onSetGesturesEnabled: (Boolean) -> Unit,
    onResetAllToDefault: () -> Unit,
    onOpenTestLab: () -> Unit,
    onOpenQualityDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accent = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    val pm = context.getSystemService(PowerManager::class.java)
    val isBatteryUnrestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null) {
        pm.isIgnoringBatteryOptimizations(context.packageName)
    } else true

    var showResetConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Settings & Look", style = MaterialTheme.typography.headlineMedium, color = IceWhite)
            Text(
                text = "Personalize playback, sound output, and visual style — all saved locally.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSteel
            )
        }

        // 1. Personalization & Appearance
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = accent)
                        Text("PERSONALIZATION", style = MaterialTheme.typography.labelLarge, color = accent)
                    }

                    Text("Theme Background", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemeStyle.entries.forEach { style ->
                            FilterChip(
                                selected = preferences.themeStyle == style,
                                onClick = { onSetThemeStyle(style) },
                                label = { Text(style.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryContainer,
                                    selectedLabelColor = accent
                                ),
                                modifier = Modifier.testTag("theme_${style.name.lowercase()}")
                            )
                        }
                    }

                    Text("Accent Color", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AccentColorOption.entries) { option ->
                            FilterChip(
                                selected = preferences.accentColor == option,
                                onClick = { onSetAccentColor(option) },
                                label = { Text(option.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryContainer,
                                    selectedLabelColor = accent
                                ),
                                modifier = Modifier.testTag("accent_${option.name.lowercase()}")
                            )
                        }
                    }

                    Text("Album Artwork Style", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ArtworkStyle.entries) { art ->
                            FilterChip(
                                selected = preferences.artworkStyle == art,
                                onClick = { onSetArtworkStyle(art) },
                                label = { Text(art.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryContainer,
                                    selectedLabelColor = accent
                                )
                            )
                        }
                    }

                    Text("Now Playing Layout", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayerLayoutMode.entries.forEach { layout ->
                            FilterChip(
                                selected = preferences.playerLayout == layout,
                                onClick = { onSetPlayerLayout(layout) },
                                label = { Text(layout.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryContainer,
                                    selectedLabelColor = accent
                                )
                            )
                        }
                    }

                    SettingSwitchRow(
                        title = "Artwork Swipe Gestures",
                        subtitle = "Swipe left/right on Player artwork to change songs",
                        checked = preferences.gesturesEnabled,
                        onCheckedChange = onSetGesturesEnabled
                    )
                    SettingSwitchRow(
                        title = "Real-time Audio Visualizer",
                        subtitle = "Live spectrum/waveform (automatically pauses when music stops)",
                        checked = preferences.visualizerEnabled,
                        onCheckedChange = onSetVisualizerEnabled
                    )
                    SettingSwitchRow(
                        title = "Embedded Album Artwork",
                        subtitle = "Display embedded album covers from audio files",
                        checked = preferences.albumArtworkEnabled,
                        onCheckedChange = onSetArtworkEnabled
                    )
                }
            }
        }

        // 2. Playback Behavior
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("PLAYBACK BEHAVIOR", style = MaterialTheme.typography.labelLarge, color = SilverMist)

                    SettingSwitchRow("Auto-Play When Selecting Song", "Start playing immediately on tap", preferences.autoPlayOnSelect, onSetAutoPlayOnSelect)
                    SettingSwitchRow("Gapless Playback", "Seamless transitions between consecutive album tracks", preferences.gaplessPlayback, onSetGapless)
                    SettingSwitchRow("Smooth Fade In / Fade Out", "Gentle volume ramp on Play and Pause", preferences.fadeInOutEnabled, onSetFadeInOut)

                    Text(
                        text = "Crossfade Between Songs: ${if (preferences.crossfadeSeconds == 0) "Off" else "${preferences.crossfadeSeconds}s"}",
                        style = MaterialTheme.typography.titleSmall,
                        color = IceWhite
                    )
                    Slider(
                        value = preferences.crossfadeSeconds.toFloat(),
                        onValueChange = { onSetCrossfadeSeconds(it.toInt()) },
                        valueRange = 0f..8f,
                        steps = 7,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )

                    Text("Default Playback Speed: ${preferences.playbackSpeed}x", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(0.75f, 0.9f, 1.0f, 1.15f, 1.25f, 1.5f)) { spd ->
                            FilterChip(
                                selected = preferences.playbackSpeed == spd,
                                onClick = { onSetPlaybackSpeed(spd) },
                                label = { Text("${spd}x") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryContainer,
                                    selectedLabelColor = accent
                                )
                            )
                        }
                    }

                    SettingSwitchRow("Audio Focus Handling", "Pause or lower volume during calls and navigation", preferences.audioFocusEnabled, onSetAudioFocus)
                    SettingSwitchRow("Restore Last Queue on Launch", "Remember your queue and position between sessions", preferences.resumePlaybackOnRestart, onSetResumeOnRestart)
                    SettingSwitchRow("Lock-Screen & Media Controls", "Keep media notification active while playing", preferences.playbackNotificationEnabled, onSetNotificationEnabled)
                }
            }
        }

        // 3. Output Device & Per-Device EQ
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OUTPUT DEVICES & MIXER", style = MaterialTheme.typography.labelLarge, color = accent)
                        IconButton(onClick = onRefreshDevices, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh audio devices", tint = accent)
                        }
                    }

                    SettingSwitchRow(
                        title = "Per-Device EQ Memory",
                        subtitle = "Automatically restore your preferred EQ for Speakers, Headphones & USB DAC",
                        checked = preferences.rememberEqPerDevice,
                        onCheckedChange = onSetRememberEqPerDevice
                    )

                    routingState.outputDevices.forEach { dev ->
                        val isActive = dev.id == routingState.activeDevice?.id
                        Surface(
                            color = if (isActive) primaryContainer.copy(alpha = 0.55f) else surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isActive) accent else SlateBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(dev.productName, style = MaterialTheme.typography.titleSmall, color = IceWhite)
                                    if (isActive) AudioSpecPill("ACTIVE", highlighted = true)
                                }
                                Text(dev.routeCategory.displayName, style = MaterialTheme.typography.bodySmall, color = SilverMist)
                                Text(
                                    text = "Rates: ${dev.supportedSampleRates.takeIf { it.isNotEmpty() }?.joinToString { "${it / 1000.0}k" } ?: "Dynamic"} • Encodings: ${dev.supportedEncodings.joinToString().ifBlank { "PCM 16-bit" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedSteel
                                )
                                dev.bluetoothCodecHint?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = accent)
                                }
                                Text(
                                    text = "Mixer: ${dev.mixerStatus.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (dev.mixerStatus == MixerVerificationStatus.VERIFIED_ANDROID_MIXER) VerifiedEmerald else AmberPeak
                                )
                            }
                        }
                    }

                    Text("Listening Output Profile", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                    OutputProfileType.entries.forEach { profile ->
                        val selected = preferences.outputProfile == profile
                        Surface(
                            color = if (selected) primaryContainer else surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (selected) accent else SlateBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetOutputProfile(profile) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(profile.displayName, style = MaterialTheme.typography.titleSmall, color = if (selected) accent else IceWhite)
                                Text(profile.subtitle, style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                            }
                        }
                    }

                    SettingSwitchRow("Prefer Native Sample Rate", "Request matching USB DAC rate when supported (Android 14+)", preferences.preferDirectBitPerfectRate, onSetPreferDirectRate)

                    Button(
                        onClick = onVerifyBitPerfectMixer,
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianBlack),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_bit_perfect_button")
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Check Android USB Bit-Perfect Mixer")
                    }

                    mixerCheckResult?.let { res ->
                        Text(
                            text = "Mixer Check Result: ${res.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (res == MixerVerificationStatus.VERIFIED_ANDROID_MIXER) VerifiedEmerald else AmberPeak
                        )
                    }

                    Text("Audio Buffer Policy (applies on service restart)", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BufferPreset.entries) { preset ->
                            FilterChip(
                                selected = preferences.bufferPreset == preset,
                                onClick = { onSetBufferPreset(preset) },
                                label = { Text(preset.displayName) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Infinix Note 40 4G / XOS Optimization Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("INFINIX NOTE 40 4G / XOS TIPS", style = MaterialTheme.typography.labelLarge, color = accent)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = AmberPeak, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Keep system DTS / Sound by JBL effects off if you are using SONICFLOW's Equalizer, so two sound processors don't stack.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SilverMist
                        )
                    }
                    Text(
                        text = if (isBatteryUnrestricted) "Background Playback: Unrestricted" else "Battery Optimization Active: Screen-off audio may pause on XOS unless allowed",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isBatteryUnrestricted) VerifiedEmerald else AmberPeak
                    )
                    if (!isBatteryUnrestricted) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                runCatching { context.startActivity(intent) }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, tint = accent)
                            Spacer(Modifier.width(8.dp))
                            Text("Allow Unrestricted Background Audio", color = accent)
                        }
                    }
                }
            }
        }

        // 5. Diagnostics & Reset to Default
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("DIAGNOSTICS & RESET", style = MaterialTheme.typography.labelLarge, color = SilverMist)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenTestLab,
                        colors = ButtonDefaults.buttonColors(containerColor = surfaceVariant, contentColor = accent),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f).height(50.dp).testTag("open_test_lab_card")
                    ) {
                        Icon(Icons.Filled.Science, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Audio Test Lab")
                    }
                    Button(
                        onClick = onOpenQualityDiagnostics,
                        colors = ButtonDefaults.buttonColors(containerColor = surfaceVariant, contentColor = ElectricBlue),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f).height(50.dp).testTag("open_quality_tests_card")
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Quality Tests")
                    }
                }

                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    border = BorderStroke(1.dp, ClipCrimson.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("settings_reset_default_button")
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = ClipCrimson)
                    Spacer(Modifier.width(8.dp))
                    Text("Reset All Settings to Default", color = ClipCrimson, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = surface,
            title = { Text("Reset All Settings?", color = IceWhite) },
            text = {
                Text(
                    text = "This restores equalizer tuning, playback preferences, and theme customization to defaults. Your music folders and playlists stay intact.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SilverMist
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllToDefault()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ClipCrimson, contentColor = IceWhite)
                ) { Text("Reset Defaults") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel", color = SilverMist) }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = IceWhite)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedSteel)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
