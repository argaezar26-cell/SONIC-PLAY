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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.routing.AudioRoutingState
import com.example.data.model.BufferPreset
import com.example.data.model.MixerVerificationStatus
import com.example.data.model.OutputProfileType
import com.example.data.prefs.AppPreferencesState
import com.example.ui.components.AudioSpecPill
import com.example.ui.theme.AmberPeak
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

@Composable
fun SettingsScreen(
    preferences: AppPreferencesState,
    routingState: AudioRoutingState,
    mixerCheckResult: MixerVerificationStatus?,
    onSetOutputProfile: (OutputProfileType) -> Unit,
    onRefreshDevices: () -> Unit,
    onVerifyBitPerfectMixer: () -> Unit,
    onSetGapless: (Boolean) -> Unit,
    onSetAudioFocus: (Boolean) -> Unit,
    onSetResumeOnRestart: (Boolean) -> Unit,
    onSetVisualizerEnabled: (Boolean) -> Unit,
    onSetArtworkEnabled: (Boolean) -> Unit,
    onSetNotificationEnabled: (Boolean) -> Unit,
    onSetBufferPreset: (BufferPreset) -> Unit,
    onSetPreferDirectRate: (Boolean) -> Unit,
    onSetSleepTimerMinutes: (Int) -> Unit,
    onOpenTestLab: () -> Unit,
    onOpenQualityDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.getSystemService(PowerManager::class.java)
    val isBatteryUnrestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null) {
        pm.isIgnoringBatteryOptimizations(context.packageName)
    } else true

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("AUDIO SETTINGS & HARDWARE", style = MaterialTheme.typography.headlineMedium, color = IceWhite)
            Text(
                text = "Local-only configuration • Zero cloud sync • Zero tracking",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSteel
            )
        }

        // Bit-Perfect & Output Routing Inspector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("OUTPUT ROUTING & MIXER CHECK", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                        IconButton(onClick = onRefreshDevices, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh audio devices", tint = ElectricCyan)
                        }
                    }

                    routingState.outputDevices.forEach { dev ->
                        val isActive = dev.id == routingState.activeDevice?.id
                        Surface(
                            color = if (isActive) CyanMuted.copy(alpha = 0.4f) else GraphiteElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isActive) ElectricCyan else SlateBorder),
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
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                                }
                                Text(
                                    text = "Mixer: ${dev.mixerStatus.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (dev.mixerStatus == MixerVerificationStatus.VERIFIED_ANDROID_MIXER) VerifiedEmerald else AmberPeak
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onVerifyBitPerfectMixer,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_bit_perfect_button")
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Check Android Bit-Perfect Mixer (API 34+)")
                    }

                    mixerCheckResult?.let { res ->
                        Text(
                            text = "Mixer Check Result: ${res.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (res == MixerVerificationStatus.VERIFIED_ANDROID_MIXER) VerifiedEmerald else AmberPeak
                        )
                    }
                    Text(
                        text = "Honest reporting: SONICFLOW inspects Android AudioDeviceInfo & AudioMixerAttributes. Physical DAC analog output cannot be verified by software alone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSteel
                    )
                }
            }
        }

        // Output Profile Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("OUTPUT PROFILE", style = MaterialTheme.typography.labelLarge, color = SilverMist)
                    OutputProfileType.entries.forEach { profile ->
                        val selected = preferences.outputProfile == profile
                        Surface(
                            color = if (selected) CyanMuted else GraphiteElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (selected) ElectricCyan else SlateBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetOutputProfile(profile) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(profile.displayName, style = MaterialTheme.typography.titleSmall, color = if (selected) ElectricCyan else IceWhite)
                                Text(profile.subtitle, style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                            }
                        }
                    }
                }
            }
        }

        // Infinix Note 40 4G / XOS Optimization Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("INFINIX NOTE 40 4G / XOS OPTIMIZATION", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = AmberPeak, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Sound by JBL / DTS system effects cannot be overridden by third-party apps. Keep system DTS off when using SONICFLOW Parametric EQ to avoid double DSP.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SilverMist
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("XOS Background Battery Survival", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                            Text(
                                text = if (isBatteryUnrestricted) "Unrestricted background playback enabled" else "Optimization active — may interrupt screen-off playback",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBatteryUnrestricted) VerifiedEmerald else AmberPeak
                            )
                        }
                    }
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
                            Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, tint = ElectricCyan)
                            Spacer(Modifier.width(8.dp))
                            Text("Allow Unrestricted Background Audio", color = ElectricCyan)
                        }
                    }
                }
            }
        }

        // Advanced Audio Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ADVANCED AUDIO ENGINE", style = MaterialTheme.typography.labelLarge, color = SilverMist)

                    SettingSwitchRow("Gapless Playback", "Seamless album track transitions", preferences.gaplessPlayback, onSetGapless)
                    SettingSwitchRow("Prefer Native Sample Rate", "Request matching mixer sample rate (API 34+)", preferences.preferDirectBitPerfectRate, onSetPreferDirectRate)
                    SettingSwitchRow("Audio Focus Handling", "Pause/duck on calls and notifications", preferences.audioFocusEnabled, onSetAudioFocus)
                    SettingSwitchRow("Resume Last Track", "Restore queue & position on app launch", preferences.resumePlaybackOnRestart, onSetResumeOnRestart)
                    SettingSwitchRow("Real-time Visualizer", "FFT spectrum & waveform from playback PCM", preferences.visualizerEnabled, onSetVisualizerEnabled)
                    SettingSwitchRow("Embedded Album Artwork", "Decode and cache cover art locally", preferences.albumArtworkEnabled, onSetArtworkEnabled)
                    SettingSwitchRow("Playback Notification", "MediaSession lock-screen & Bluetooth controls", preferences.playbackNotificationEnabled, onSetNotificationEnabled)

                    Text("Audio Buffer Policy (applies on service restart)", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BufferPreset.entries.forEach { preset ->
                            FilterChip(
                                selected = preferences.bufferPreset == preset,
                                onClick = { onSetBufferPreset(preset) },
                                label = { Text(preset.displayName) }
                            )
                        }
                    }

                    Text("Quick Sleep Timer", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "60m").forEach { (min, label) ->
                            FilterChip(
                                selected = false,
                                onClick = { onSetSleepTimerMinutes(min) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // Labs Launchers
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onOpenTestLab,
                    colors = ButtonDefaults.buttonColors(containerColor = GraphiteElevated, contentColor = ElectricCyan),
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Filled.Science, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Audio Test Lab")
                }
                Button(
                    onClick = onOpenQualityDiagnostics,
                    colors = ButtonDefaults.buttonColors(containerColor = GraphiteElevated, contentColor = ElectricBlue),
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Icon(Icons.Filled.Verified, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Quality Tests")
                }
            }
        }
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
