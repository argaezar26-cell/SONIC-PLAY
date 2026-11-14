package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DspSettings
import com.example.data.model.DspTelemetry
import com.example.data.model.EqBand
import com.example.data.model.EqPreset
import com.example.data.model.FilterType
import com.example.data.model.ReplayGainMode
import com.example.ui.components.ParametricEqResponseGraph
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
import kotlin.math.exp
import kotlin.math.ln

@Composable
fun EqualizerScreen(
    dspSettings: DspSettings,
    telemetry: DspTelemetry,
    presets: List<EqPreset>,
    sampleRateHz: Int,
    onSetBitTransparent: (Boolean) -> Unit,
    onSetEqEnabled: (Boolean) -> Unit,
    onToggleBypassBeforeAfter: () -> Unit,
    onSetAutoHeadroom: (Boolean) -> Unit,
    onSetPreampDb: (Float) -> Unit,
    onUpdateBand: (EqBand) -> Unit,
    onAddBand: () -> Unit,
    onRemoveLastBand: () -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (EqPreset) -> Unit,
    onExportPresetJson: () -> String,
    onImportPresetJson: (String) -> Boolean,
    onSetCrossfeed: (Boolean, Float) -> Unit,
    onSetReplayGain: (ReplayGainMode, Float) -> Unit,
    onSetDigitalGain: (Float) -> Unit,
    onSetLimiter: (Boolean, Float) -> Unit,
    onSetStereoControls: (Float, Boolean) -> Unit,
    onResetClipDetector: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedBandIndex by remember { mutableIntStateOf(0) }
    val safeBandIndex = selectedBandIndex.coerceIn(0, (dspSettings.bands.size - 1).coerceAtLeast(0))
    val activeBand = dspSettings.bands.getOrNull(safeBandIndex)

    var showSaveDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    val eqActuallyActive = !dspSettings.bitTransparentMode &&
        dspSettings.dspMasterEnabled &&
        dspSettings.eqEnabled &&
        !dspSettings.eqBypassBeforeAfter

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Bit-Transparent vs DSP Mode Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = BorderStroke(
                    1.dp,
                    if (dspSettings.bitTransparentMode) VerifiedEmerald.copy(alpha = 0.5f) else ElectricCyan.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PARAMETRIC DSP ENGINE", style = MaterialTheme.typography.titleMedium, color = IceWhite)
                            Text(
                                text = if (dspSettings.bitTransparentMode) {
                                    "Bit-Transparent Mode active — all EQ & DSP bypassed"
                                } else {
                                    "64-bit Biquad DSP active (cannot claim bit-perfect while DSP is on)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (dspSettings.bitTransparentMode) VerifiedEmerald else SilverMist
                            )
                        }
                        Switch(
                            checked = !dspSettings.bitTransparentMode,
                            onCheckedChange = { dspOn -> onSetBitTransparent(!dspOn) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianBlack,
                                checkedTrackColor = ElectricCyan
                            ),
                            modifier = Modifier.testTag("eq_dsp_master_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = if (telemetry.isClipping) ClipCrimson.copy(alpha = 0.2f) else GraphiteElevated,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (telemetry.isClipping) ClipCrimson else SlateBorder),
                                modifier = Modifier.clickable(onClick = onResetClipDetector)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (telemetry.isClipping) ClipCrimson else VerifiedEmerald)
                                    )
                                    Text(
                                        text = if (telemetry.isClipping) "CLIP (${telemetry.clippedSampleCount})" else "NO CLIP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (telemetry.isClipping) ClipCrimson else SilverMist
                                    )
                                }
                            }
                            Text(
                                text = "L ${telemetry.peakLeftDbfs.toInt()} dB | R ${telemetry.peakRightDbfs.toInt()} dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSteel
                            )
                        }

                        // Before / After Bypass button
                        Surface(
                            onClick = onToggleBypassBeforeAfter,
                            color = if (dspSettings.eqBypassBeforeAfter) AmberPeak.copy(alpha = 0.2f) else GraphiteElevated,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (dspSettings.eqBypassBeforeAfter) AmberPeak else SlateBorder),
                            modifier = Modifier.testTag("eq_ab_compare_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.CompareArrows, contentDescription = null, tint = if (dspSettings.eqBypassBeforeAfter) AmberPeak else ElectricCyan, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (dspSettings.eqBypassBeforeAfter) "BEFORE (BYPASS)" else "AFTER (EQ ON)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (dspSettings.eqBypassBeforeAfter) AmberPeak else ElectricCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // Real-time Frequency Response Graph
        item {
            ParametricEqResponseGraph(
                bands = dspSettings.bands,
                preampDb = dspSettings.preampDb + telemetry.appliedHeadroomDb,
                selectedBandIndex = safeBandIndex,
                sampleRateHz = sampleRateHz,
                eqActive = eqActuallyActive
            )
        }

        // Presets Row + Save / Import / Export
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("EQ PRESETS", style = MaterialTheme.typography.labelLarge, color = SilverMist)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { showSaveDialog = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Save, contentDescription = "Save custom preset", tint = ElectricCyan)
                        }
                        IconButton(onClick = { showImportDialog = true }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.FileDownload, contentDescription = "Import preset JSON", tint = SilverMist)
                        }
                        IconButton(
                            onClick = {
                                val json = onExportPresetJson()
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("SONICFLOW EQ Preset", json))
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, json)
                                }
                                runCatching { context.startActivity(Intent.createChooser(share, "Export EQ Preset JSON")) }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Filled.FileUpload, contentDescription = "Export preset JSON", tint = SilverMist)
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets, key = { it.id }) { preset ->
                        val selected = preset.id == dspSettings.activePresetId
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectPreset(preset) },
                            label = { Text(preset.name) },
                            trailingIcon = if (!preset.isBuiltIn) {
                                {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete preset",
                                        modifier = Modifier.size(16.dp).clickable { onDeleteCustomPreset(preset) }
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanMuted,
                                selectedLabelColor = ElectricCyan,
                                containerColor = GraphiteSurface,
                                labelColor = SilverMist
                            )
                        )
                    }
                }
            }
        }

        // Preamp & Headroom + Band Count (10-15 bands)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Preamp: ${String.format(Locale.US, "%+.1f", dspSettings.preampDb)} dB",
                            style = MaterialTheme.typography.labelLarge,
                            color = IceWhite
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("EQ On", style = MaterialTheme.typography.bodySmall, color = SilverMist)
                            Switch(
                                checked = dspSettings.eqEnabled,
                                onCheckedChange = onSetEqEnabled
                            )
                        }
                    }
                    Slider(
                        value = dspSettings.preampDb,
                        onValueChange = onSetPreampDb,
                        valueRange = -15f..6f,
                        colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Headroom Compensation", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                            Text(
                                text = "Applied: ${String.format(Locale.US, "%.1f", telemetry.appliedHeadroomDb)} dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                        Switch(checked = dspSettings.autoHeadroomEnabled, onCheckedChange = onSetAutoHeadroom)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bands: ${dspSettings.bands.size} (10–15)", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = onRemoveLastBand,
                                enabled = dspSettings.bands.size > 10
                            ) { Icon(Icons.Filled.Remove, contentDescription = "Remove band") }
                            OutlinedButton(
                                onClick = onAddBand,
                                enabled = dspSettings.bands.size < 15
                            ) { Icon(Icons.Filled.Add, contentDescription = "Add band") }
                        }
                    }
                }
            }
        }

        // Band Selector Strip
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dspSettings.bands, key = { it.index }) { band ->
                    val selected = band.index == safeBandIndex
                    Surface(
                        onClick = { selectedBandIndex = band.index },
                        color = if (selected) CyanMuted else GraphiteSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (selected) ElectricCyan else SlateBorder),
                        modifier = Modifier.width(78.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatHz(band.frequencyHz),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) ElectricCyan else SilverMist
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "%+.1f", band.gainDb),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (band.enabled) IceWhite else MutedSteel
                            )
                            Text(
                                text = band.filterType.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSteel
                            )
                        }
                    }
                }
            }
        }

        // Selected Band Parametric Editor
        if (activeBand != null) {
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
                            Text(
                                text = "BAND ${activeBand.index + 1} • ${formatHz(activeBand.frequencyHz)}",
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan
                            )
                            Switch(
                                checked = activeBand.enabled,
                                onCheckedChange = { onUpdateBand(activeBand.copy(enabled = it)) }
                            )
                        }

                        // Filter Type selector
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(FilterType.entries) { fType ->
                                val isType = activeBand.filterType == fType
                                FilterChip(
                                    selected = isType,
                                    onClick = { onUpdateBand(activeBand.copy(filterType = fType)) },
                                    label = { Text(fType.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanMuted,
                                        selectedLabelColor = ElectricCyan,
                                        containerColor = GraphiteElevated,
                                        labelColor = SilverMist
                                    )
                                )
                            }
                        }

                        // Gain Slider
                        Text(
                            text = "Gain: ${String.format(Locale.US, "%+.1f dB", activeBand.gainDb)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = IceWhite
                        )
                        Slider(
                            value = activeBand.gainDb,
                            onValueChange = { onUpdateBand(activeBand.copy(gainDb = it)) },
                            valueRange = -12f..12f,
                            colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                        )

                        // Log Frequency Slider
                        val minLn = ln(20f)
                        val maxLn = ln(20000f)
                        val freqPos = ((ln(activeBand.frequencyHz.coerceIn(20f, 20000f)) - minLn) / (maxLn - minLn)).coerceIn(0f, 1f)
                        Text(
                            text = "Frequency: ${formatHz(activeBand.frequencyHz)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = IceWhite
                        )
                        Slider(
                            value = freqPos,
                            onValueChange = { pos ->
                                val hz = exp(minLn + pos * (maxLn - minLn))
                                onUpdateBand(activeBand.copy(frequencyHz = hz))
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                        )

                        // Q Factor Slider
                        Text(
                            text = "Q Factor: ${String.format(Locale.US, "%.2f", activeBand.qFactor)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = IceWhite
                        )
                        Slider(
                            value = activeBand.qFactor,
                            onValueChange = { onUpdateBand(activeBand.copy(qFactor = it)) },
                            valueRange = 0.3f..10.0f,
                            colors = SliderDefaults.colors(thumbColor = ElectricCyan, activeTrackColor = ElectricCyan)
                        )
                    }
                }
            }
        }

        // Advanced DSP Modules: Crossfeed, ReplayGain, Volume/Limiter, Stereo Management
        item {
            Text("ADVANCED AUDIO DSP", style = MaterialTheme.typography.labelLarge, color = SilverMist)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GraphiteSurface),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // A. Crossfeed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bauer Stereo Crossfeed", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                            Text("Natural headphone soundstage imaging", style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                        }
                        Switch(
                            checked = dspSettings.crossfeedEnabled,
                            onCheckedChange = { onSetCrossfeed(it, dspSettings.crossfeedIntensity) }
                        )
                    }
                    if (dspSettings.crossfeedEnabled) {
                        Text("Intensity: ${(dspSettings.crossfeedIntensity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = ElectricCyan)
                        Slider(
                            value = dspSettings.crossfeedIntensity,
                            onValueChange = { onSetCrossfeed(true, it) },
                            valueRange = 0.05f..1f
                        )
                    }

                    // B. ReplayGain
                    Text("ReplayGain Loudness Normalization", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReplayGainMode.entries.forEach { mode ->
                            FilterChip(
                                selected = dspSettings.replayGainMode == mode,
                                onClick = { onSetReplayGain(mode, dspSettings.replayGainPreampDb) },
                                label = { Text(mode.displayName) }
                            )
                        }
                    }
                    if (dspSettings.replayGainMode != ReplayGainMode.OFF) {
                        Text(
                            "ReplayGain Preamp: ${String.format(Locale.US, "%+.1f dB", dspSettings.replayGainPreampDb)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                        Slider(
                            value = dspSettings.replayGainPreampDb,
                            onValueChange = { onSetReplayGain(dspSettings.replayGainMode, it) },
                            valueRange = -12f..12f
                        )
                    }

                    // C & D. Digital Gain + Peak Limiter
                    Text(
                        "Digital Volume Trim: ${String.format(Locale.US, "%+.1f dB", dspSettings.digitalGainDb)}",
                        style = MaterialTheme.typography.titleSmall,
                        color = IceWhite
                    )
                    Slider(
                        value = dspSettings.digitalGainDb,
                        onValueChange = onSetDigitalGain,
                        valueRange = -18f..6f
                    )
                    if (dspSettings.digitalGainDb > 0f && !dspSettings.limiterEnabled) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = AmberPeak, modifier = Modifier.size(16.dp))
                            Text("Positive digital gain without limiter may clip.", style = MaterialTheme.typography.bodySmall, color = AmberPeak)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Soft-Knee Peak Limiter", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                            Text("Prevents accidental digital clipping", style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                        }
                        Switch(
                            checked = dspSettings.limiterEnabled,
                            onCheckedChange = { onSetLimiter(it, dspSettings.limiterThresholdDb) }
                        )
                    }
                    if (dspSettings.limiterEnabled) {
                        Text(
                            "Limiter Threshold: ${String.format(Locale.US, "%.1f dBFS", dspSettings.limiterThresholdDb)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                        Slider(
                            value = dspSettings.limiterThresholdDb,
                            onValueChange = { onSetLimiter(true, it) },
                            valueRange = -6f..-0.1f
                        )
                    }

                    // E. Stereo Balance & Mono Check
                    Text(
                        "Stereo L/R Balance: ${
                            when {
                                dspSettings.stereoBalance < -0.05f -> "L ${(-dspSettings.stereoBalance * 100).toInt()}%"
                                dspSettings.stereoBalance > 0.05f -> "R ${(dspSettings.stereoBalance * 100).toInt()}%"
                                else -> "Center"
                            }
                        }",
                        style = MaterialTheme.typography.titleSmall,
                        color = IceWhite
                    )
                    Slider(
                        value = dspSettings.stereoBalance,
                        onValueChange = { onSetStereoControls(it, dspSettings.monoDownmix) },
                        valueRange = -1f..1f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mono Compatibility Check (L+R)", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                        Switch(
                            checked = dspSettings.monoDownmix,
                            onCheckedChange = { onSetStereoControls(dspSettings.stereoBalance, it) }
                        )
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = GraphiteSurface,
            title = { Text("Save Custom EQ Preset", color = IceWhite) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) onSaveCustomPreset(name)
                    showSaveDialog = false
                }) { Text("Save", color = ElectricCyan) }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel", color = SilverMist) }
            }
        )
    }

    if (showImportDialog) {
        var raw by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImportDialog = false; importError = null },
            containerColor = GraphiteSurface,
            title = { Text("Import EQ Preset JSON", color = IceWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = raw,
                        onValueChange = { raw = it; importError = null },
                        label = { Text("Paste SONICFLOW JSON") },
                        minLines = 4,
                        maxLines = 8
                    )
                    if (importError != null) {
                        Text(importError!!, color = ClipCrimson, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onImportPresetJson(raw)) {
                            showImportDialog = false
                            importError = null
                        } else {
                            importError = "Invalid SONICFLOW preset JSON"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack)
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; importError = null }) { Text("Cancel", color = SilverMist) }
            }
        )
    }
}
