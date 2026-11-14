package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.routing.AudioRoutingState
import com.example.audio.testlab.TestToneMode
import com.example.audio.testlab.ToneLabState
import com.example.data.model.QualityTestItem
import com.example.data.model.TestResultStatus
import com.example.ui.components.formatHz
import com.example.ui.theme.AmberPeak
import com.example.ui.theme.ClipCrimson
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.IceWhite
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverMist
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.VerifiedEmerald
import java.util.Locale

@Composable
fun AudioTestLabScreen(
    toneState: ToneLabState,
    routingState: AudioRoutingState,
    onStartTone: (TestToneMode) -> Unit,
    onStopTone: () -> Unit,
    onSetSineFrequency: (Float) -> Unit,
    onSetOutputLevelDb: (Float) -> Unit,
    onSetChannelBalance: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var pendingToneMode by remember { mutableStateOf<TestToneMode?>(null) }
    var safetyAcknowledged by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onStopTone(); onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
                }
                Column {
                    Text("AUDIO TEST LAB", style = MaterialTheme.typography.headlineMedium, color = IceWhite)
                    Text("Precision AudioTrack PCM signal generator", style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                }
            }
        }

        // Safety Notice + Active Route Sample Rate Info
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = BorderStroke(1.dp, AmberPeak.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = AmberPeak)
                        Text("Volume Safety Notice", style = MaterialTheme.typography.titleSmall, color = AmberPeak)
                    }
                    Text(
                        text = "Lower headphone/speaker volume before playing continuous sine tones or 20 Hz–20 kHz sweeps. SONICFLOW never alters device system volume automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SilverMist
                    )
                    val active = routingState.activeDevice
                    Text(
                        text = "Active Output: ${active?.productName ?: "System"} • Supported Rates: ${active?.supportedSampleRates?.joinToString { "${it}Hz" }?.ifBlank { "48000 Hz" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
            }
        }

        // Generator Controls (Frequency, Level dB, Balance)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sine Frequency: ${formatHz(toneState.sineFrequencyHz)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                    Slider(
                        value = toneState.sineFrequencyHz,
                        onValueChange = onSetSineFrequency,
                        valueRange = 20f..20000f
                    )

                    Text(
                        text = "Generator Level: ${String.format(Locale.US, "%.1f dBFS", toneState.outputLevelDb)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = IceWhite
                    )
                    Slider(
                        value = toneState.outputLevelDb,
                        onValueChange = onSetOutputLevelDb,
                        valueRange = -40f..-1f
                    )

                    Text(
                        text = "Channel Balance: ${
                            when {
                                toneState.channelBalance < -0.05f -> "Left ${(-toneState.channelBalance * 100).toInt()}%"
                                toneState.channelBalance > 0.05f -> "Right ${(toneState.channelBalance * 100).toInt()}%"
                                else -> "Center"
                            }
                        }",
                        style = MaterialTheme.typography.labelLarge,
                        color = IceWhite
                    )
                    Slider(
                        value = toneState.channelBalance,
                        onValueChange = onSetChannelBalance,
                        valueRange = -1f..1f
                    )

                    if (toneState.isPlaying) {
                        Button(
                            onClick = onStopTone,
                            colors = ButtonDefaults.buttonColors(containerColor = ClipCrimson, contentColor = IceWhite),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (toneState.activeMode == TestToneMode.FREQ_SWEEP) {
                                    "STOP SWEEP (${formatHz(toneState.currentSweepHz)})"
                                } else {
                                    "STOP TEST TONE"
                                }
                            )
                        }
                    }
                }
            }
        }

        // Test Tone Modes
        items(TestToneMode.entries) { mode ->
            val active = toneState.isPlaying && toneState.activeMode == mode
            Surface(
                color = if (active) CyanMuted else GraphiteSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (active) ElectricCyan else SlateBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (active) {
                            onStopTone()
                        } else if (safetyAcknowledged) {
                            onStartTone(mode)
                        } else {
                            pendingToneMode = mode
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mode.label, style = MaterialTheme.typography.titleSmall, color = if (active) ElectricCyan else IceWhite)
                        Text(mode.description, style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                    }
                    Icon(
                        imageVector = if (active) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = if (active) ClipCrimson else ElectricCyan
                    )
                }
            }
        }
    }

    pendingToneMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { pendingToneMode = null },
            containerColor = GraphiteSurface,
            title = { Text("Volume Safety Check", color = AmberPeak) },
            text = {
                Text(
                    "You are about to play a continuous audio test signal (${mode.label}). Please confirm your device/DAC volume is at a safe listening level.",
                    color = SilverMist
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        safetyAcknowledged = true
                        pendingToneMode = null
                        onStartTone(mode)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack)
                ) { Text("I Checked Volume — Play") }
            },
            dismissButton = {
                TextButton(onClick = { pendingToneMode = null }) { Text("Cancel", color = SilverMist) }
            }
        )
    }
}

@Composable
fun AudioQualityTestingScreen(
    tests: List<QualityTestItem>,
    isRunning: Boolean,
    onRunDiagnostics: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
                }
                Column {
                    Text("AUDIO QUALITY DIAGNOSTICS", style = MaterialTheme.typography.headlineMedium, color = IceWhite)
                    Text("Live hardware, codec, DSP & service verification", style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                }
            }
        }

        item {
            Button(
                onClick = onRunDiagnostics,
                enabled = !isRunning,
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("run_quality_diagnostics_button")
            ) {
                if (isRunning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ObsidianBlack, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Running Hardware & Pipeline Probes...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.Verified, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Run Real Audio Quality Tests", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(tests, key = { it.id }) { item ->
            val (badgeColor, badgeText) = when (item.status) {
                TestResultStatus.PASS -> VerifiedEmerald to "PASS"
                TestResultStatus.FAIL -> ClipCrimson to "FAIL"
                TestResultStatus.NOT_AVAILABLE -> AmberPeak to "NOT AVAILABLE"
                TestResultStatus.PENDING -> MutedSteel to "PENDING"
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, color = IceWhite, modifier = Modifier.weight(1f))
                        Surface(
                            color = badgeColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Text(item.detail, style = MaterialTheme.typography.bodySmall, color = SilverMist)
                }
            }
        }
    }
}
