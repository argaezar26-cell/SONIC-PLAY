package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.OutputProfileType
import com.example.data.repository.ScanProgress
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverMist
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.VerifiedEmerald

@Composable
fun OnboardingScreen(
    trackCount: Int,
    folderCount: Int,
    scanProgress: ScanProgress,
    selectedOutputProfile: OutputProfileType,
    onPickFolder: () -> Unit,
    onSelectOutputProfile: (OutputProfileType) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }
    val accent = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(4) { idx ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (idx == step) 28.dp else 12.dp)
                                .clip(CircleShape)
                                .background(if (idx <= step) accent else SlateBorder)
                        )
                    }
                }
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Skip", color = SilverMist)
                }
            }

            when (step) {
                0 -> {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = surface),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
                    ) {
                        Column {
                            Image(
                                painter = painterResource(R.drawable.img_sonicflow_hero),
                                contentDescription = "SONICFLOW welcome artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(190.dp)
                            )
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Welcome to SONICFLOW", style = MaterialTheme.typography.headlineMedium, color = IceWhite)
                                Text(
                                    text = "A clean, local music player built for natural sound, crisp vocals, and effortless listening on Infinix Note 40 4G.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SilverMist
                                )
                                FeatureBullet("Plays FLAC, WAV, MP3, M4A, OGG & OPUS offline")
                                FeatureBullet("No account, no internet required, no bloat")
                                FeatureBullet("Simple sound presets + pro parametric EQ when you want it")
                            }
                        }
                    }
                }

                1 -> {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = surface),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.CreateNewFolder, contentDescription = null, tint = accent, modifier = Modifier.size(36.dp))
                            Text("Choose Your Music Folder", style = MaterialTheme.typography.headlineSmall, color = IceWhite)
                            Text(
                                text = "SONICFLOW only reads folders you pick using Android's built-in folder picker — no broad storage permissions needed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SilverMist
                            )
                            if (scanProgress.isScanning) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                    color = accent
                                )
                                Text(
                                    text = "Finding songs (${scanProgress.scannedCount}): ${scanProgress.currentFile}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
                                )
                            } else if (trackCount > 0) {
                                Surface(
                                    color = VerifiedEmerald.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, VerifiedEmerald.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = VerifiedEmerald)
                                        Text(
                                            text = "Ready: $trackCount songs indexed across $folderCount folder(s).",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IceWhite
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = onPickFolder,
                                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianBlack),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("onboarding_pick_folder_button")
                            ) {
                                Icon(Icons.Filled.CreateNewFolder, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (trackCount > 0) "Add Another Folder" else "Select Music Folder",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                2 -> {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = surface),
                        border = BorderStroke(1.dp, SlateBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("How Do You Usually Listen?", style = MaterialTheme.typography.headlineSmall, color = IceWhite)
                            Text(
                                text = "You can switch anytime. SONICFLOW also detects wired USB-C, USB DACs, and Bluetooth automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SilverMist
                            )
                            OutputProfileType.entries.forEach { profile ->
                                val selected = selectedOutputProfile == profile
                                val icon = when (profile) {
                                    OutputProfileType.PHONE_SPEAKERS -> Icons.Filled.Speaker
                                    OutputProfileType.WIRED_USB -> Icons.Filled.Headphones
                                    OutputProfileType.EXTERNAL_DAC -> Icons.Filled.Usb
                                    OutputProfileType.BLUETOOTH -> Icons.Filled.BluetoothAudio
                                    OutputProfileType.AUTO -> Icons.Filled.GraphicEq
                                }
                                Surface(
                                    color = if (selected) primaryContainer else surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (selected) accent else SlateBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectOutputProfile(profile) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(icon, contentDescription = null, tint = if (selected) accent else SilverMist)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(profile.displayName, style = MaterialTheme.typography.titleSmall, color = IceWhite)
                                            Text(profile.subtitle, style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                                        }
                                        if (selected) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = accent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = surface),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = accent, modifier = Modifier.size(40.dp))
                            Text("All Set to Listen", style = MaterialTheme.typography.headlineMedium, color = IceWhite)
                            Text(
                                text = if (trackCount > 0) {
                                    "Your library has $trackCount songs ready to play. Tap any song on Home or Library to begin."
                                } else {
                                    "You can add your music folders anytime from the Home or Library tab."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = SilverMist
                            )
                            FeatureBullet("Swipe artwork left/right to skip songs")
                            FeatureBullet("Tap any quality badge to understand what FLAC / kHz / bit depth means")
                            FeatureBullet("Per-device EQ automatically remembers tuning for speakers vs headphones")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (step > 0) {
                OutlinedButton(
                    onClick = { step -= 1 },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Back", color = IceWhite)
                }
            }
            Button(
                onClick = {
                    if (step < 3) step += 1 else onComplete()
                },
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianBlack),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(
                    text = if (step < 3) "Continue" else "Start Listening",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FeatureBullet(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = IceWhite)
    }
}
