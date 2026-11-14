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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.example.data.model.EqUiMode
import com.example.data.model.FilterType
import com.example.data.model.OutputRouteCategory
import com.example.data.model.ReplayGainMode
import com.example.data.model.SimpleEqPreset
import com.example.ui.components.ParametricEqResponseGraph
import com.example.ui.components.formatHz
import com.example.ui.theme.AmberPeak
import com.example.ui.theme.ClipCrimson
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
    eqUiMode: EqUiMode,
    simplePreset: SimpleEqPreset,
    simpleIntensity: Float,
    rememberEqPerDevice: Boolean,
    activeRouteCategory: OutputRouteCategory,
    canUndoEq: Boolean,
    telemetry: DspTelemetry,
    presets: List<EqPreset>,
    sampleRateHz: Int,
    onSetEqUiMode: (EqUiMode) -> Unit,
    onSelectSimplePreset: (SimpleEqPreset) -> Unit,
    onSetSimpleIntensity: (Float) -> Unit,
    onUndoEq: () -> Unit,
    onResetEq: () -> Unit,
    onSetRememberEqPerDevice: (Boolean) -> Unit,
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
    val accent = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    var selectedBandIndex by remember { mutableIntStateOf(0) }
    val safeBandIndex = selectedBandIndex.coerceIn(0, (dspSettings.bands.size - 1).coerceAtLeast(0))
    val activeBand = dspSettings.bands.getOrNull(safeBandIndex)

    var showSaveDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    val eqActuallyActive = !dspSettings.bitTransparentMode &&
        dspSettings.dspMasterEnabled &&
        dspSettings.eqEnabled &&
        !dspSettings.eqBypassBeforeAfter

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Header + Simple / Advanced Switch + Undo / Reset / Help
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sound & Equalizer", style = MaterialTheme.typography.headlineMedium, color = IceWhite)
                        Text(
                            text = if (rememberEqPerDevice) {
                                "Remembered for ${activeRouteCategory.displayName}"
                            } else {
                                "Applied across all outputs"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = accent
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = onUndoEq,
                            enabled = canUndoEq,
                            modifier = Modifier.size(44.dp).testTag("eq_undo_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo equalizer change",
                                tint = if (canUndoEq) accent else MutedSteel
                            )
                        }
                        IconButton(
                            onClick = onResetEq,
                            modifier = Modifier.size(44.dp).testTag("eq_reset_button")
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = "Reset equalizer", tint = SilverMist)
                        }
                        IconButton(
                            onClick = { showHelpDialog = true },
                            modifier = Modifier.size(44.dp).testTag("eq_help_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Equalizer guide", tint = SilverMist)
                        }
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    EqUiMode.entries.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = eqUiMode == mode,
                            onClick = { onSetEqUiMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = EqUiMode.entries.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = primaryContainer,
                                activeContentColor = accent,
                                inactiveContainerColor = surface,
                                inactiveContentColor = SilverMist
                            ),
                            modifier = Modifier.testTag("eq_mode_${mode.name.lowercase()}")
                        ) {
                            Text(mode.label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Master Pure Direct vs DSP Card + Clip + A/B Compare
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(
                    1.dp,
                    if (dspSettings.bitTransparentMode) VerifiedEmerald.copy(alpha = 0.5f) else accent.copy(alpha = 0.45f)
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
                            Text(
                                text = if (dspSettings.bitTransparentMode) "Pure Direct (EQ Bypassed)" else "Sound Tuning Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = IceWhite
                            )
                            Text(
                                text = if (dspSettings.bitTransparentMode) {
                                    "Bit-transparent signal with zero EQ coloration."
                                } else {
                                    "64-bit DSP tuning active (bit-perfect is off while EQ runs)."
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
                                checkedTrackColor = accent
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
                                color = if (telemetry.isClipping) ClipCrimson.copy(alpha = 0.2f) else surfaceVariant,
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
                                        text = if (telemetry.isClipping) "CLIP (${telemetry.clippedSampleCount})" else "CLEAN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (telemetry.isClipping) ClipCrimson else SilverMist
                                    )
                                }
                            }
                            Text(
                                text = "L ${telemetry.peakLeftDbfs.toInt()} | R ${telemetry.peakRightDbfs.toInt()} dB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MutedSteel
                            )
                        }

                        Surface(
                            onClick = onToggleBypassBeforeAfter,
                            color = if (dspSettings.eqBypassBeforeAfter) AmberPeak.copy(alpha = 0.2f) else surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (dspSettings.eqBypassBeforeAfter) AmberPeak else SlateBorder),
                            modifier = Modifier.testTag("eq_ab_compare_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.CompareArrows, contentDescription = null, tint = if (dspSettings.eqBypassBeforeAfter) AmberPeak else accent, modifier = Modifier.size(16.dp))
                                Text(
                                    text = if (dspSettings.eqBypassBeforeAfter) "ORIGINAL" else "TUNED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (dspSettings.eqBypassBeforeAfter) AmberPeak else accent
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Frequency Response Graph (visible in both Simple & Advanced)
        item {
            ParametricEqResponseGraph(
                bands = dspSettings.bands,
                preampDb = dspSettings.preampDb + telemetry.appliedHeadroomDb,
                selectedBandIndex = if (eqUiMode == EqUiMode.ADVANCED) safeBandIndex else -1,
                sampleRateHz = sampleRateHz,
                eqActive = eqActuallyActive
            )
        }

        if (eqUiMode == EqUiMode.SIMPLE) {
            // SIMPLE MODE: Musical presets + Intensity Slider + Per-Device Switch
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = surface),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Choose a Listening Character", style = MaterialTheme.typography.titleMedium, color = IceWhite)

                        SimpleEqPreset.entries.forEach { preset ->
                            val selected = !dspSettings.bitTransparentMode && simplePreset == preset
                            Surface(
                                color = if (selected) primaryContainer else surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (selected) accent else SlateBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("simple_eq_${preset.name.lowercase()}")
                                    .clickable {
                                        if (preset == SimpleEqPreset.CUSTOM) {
                                            onSetEqUiMode(EqUiMode.ADVANCED)
                                        } else {
                                            onSelectSimplePreset(preset)
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (selected) accent else IceWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = preset.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SilverMist
                                        )
                                    }
                                    if (preset == SimpleEqPreset.CUSTOM) {
                                        Text("Edit ->", style = MaterialTheme.typography.labelMedium, color = accent)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Effect Strength: ${(simpleIntensity * 100).toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            color = accent
                        )
                        Slider(
                            value = simpleIntensity,
                            onValueChange = onSetSimpleIntensity,
                            valueRange = 0f..1.5f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                            modifier = Modifier.testTag("simple_eq_intensity_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Remember EQ per Output Device", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                                Text("Auto-switches between Speakers, Headphones & USB DAC", style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                            }
                            Switch(checked = rememberEqPerDevice, onCheckedChange = onSetRememberEqPerDevice)
                        }
                    }
                }
            }
        } else {
            // ADVANCED MODE: Full Parametric EQ + Presets + JSON + Advanced DSP
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PARAMETRIC PRESETS", style = MaterialTheme.typography.labelLarge, color = SilverMist)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { showSaveDialog = true }, modifier = Modifier.size(44.dp)) {
                                Icon(Icons.Filled.Save, contentDescription = "Save custom preset", tint = accent)
                            }
                            IconButton(onClick = { showImportDialog = true }, modifier = Modifier.size(44.dp)) {
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
                                modifier = Modifier.size(44.dp)
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
                                    selectedContainerColor = primaryContainer,
                                    selectedLabelColor = accent,
                                    containerColor = surface,
                                    labelColor = SilverMist
                                )
                            )
                        }
                    }
                }
            }

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
                            Text(
                                text = "Preamp: ${String.format(Locale.US, "%+.1f", dspSettings.preampDb)} dB",
                                style = MaterialTheme.typography.labelLarge,
                                color = IceWhite
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("EQ On", style = MaterialTheme.typography.bodySmall, color = SilverMist)
                                Switch(checked = dspSettings.eqEnabled, onCheckedChange = onSetEqEnabled)
                            }
                        }
                        Slider(
                            value = dspSettings.preampDb,
                            onValueChange = onSetPreampDb,
                            valueRange = -15f..6f,
                            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto Headroom Protection", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                                Text(
                                    text = "Prevents distortion (${String.format(Locale.US, "%.1f", telemetry.appliedHeadroomDb)} dB)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
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

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dspSettings.bands, key = { it.index }) { band ->
                        val selected = band.index == safeBandIndex
                        Surface(
                            onClick = { selectedBandIndex = band.index },
                            color = if (selected) primaryContainer else surface,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (selected) accent else SlateBorder),
                            modifier = Modifier.width(78.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = formatHz(band.frequencyHz),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) accent else SilverMist
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

            if (activeBand != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = surface),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
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
                                    color = accent
                                )
                                Switch(
                                    checked = activeBand.enabled,
                                    onCheckedChange = { onUpdateBand(activeBand.copy(enabled = it)) }
                                )
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(FilterType.entries) { fType ->
                                    val isType = activeBand.filterType == fType
                                    FilterChip(
                                        selected = isType,
                                        onClick = { onUpdateBand(activeBand.copy(filterType = fType)) },
                                        label = { Text(fType.displayName) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = primaryContainer,
                                            selectedLabelColor = accent,
                                            containerColor = surfaceVariant,
                                            labelColor = SilverMist
                                        )
                                    )
                                }
                            }

                            Text(
                                text = "Gain: ${String.format(Locale.US, "%+.1f dB", activeBand.gainDb)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = IceWhite
                            )
                            Slider(
                                value = activeBand.gainDb,
                                onValueChange = { onUpdateBand(activeBand.copy(gainDb = it)) },
                                valueRange = -12f..12f,
                                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                            )

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
                                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                            )

                            Text(
                                text = "Q Factor (Width): ${String.format(Locale.US, "%.2f", activeBand.qFactor)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = IceWhite
                            )
                            Slider(
                                value = activeBand.qFactor,
                                onValueChange = { onUpdateBand(activeBand.copy(qFactor = it)) },
                                valueRange = 0.3f..10.0f,
                                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                            )
                        }
                    }
                }
            }

            // Advanced DSP Rack
            item {
                Text("HEADPHONE & GAIN TOOLS", style = MaterialTheme.typography.labelLarge, color = SilverMist)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = surface),
                    border = BorderStroke(1.dp, SlateBorder),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Headphone Crossfeed", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                                Text("Blends subtle left/right ambience like real speakers", style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                            }
                            Switch(
                                checked = dspSettings.crossfeedEnabled,
                                onCheckedChange = { onSetCrossfeed(it, dspSettings.crossfeedIntensity) }
                            )
                        }
                        if (dspSettings.crossfeedEnabled) {
                            Text("Amount: ${(dspSettings.crossfeedIntensity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = accent)
                            Slider(
                                value = dspSettings.crossfeedIntensity,
                                onValueChange = { onSetCrossfeed(true, it) },
                                valueRange = 0.05f..1f
                            )
                        }

                        Text("ReplayGain Volume Matching", style = MaterialTheme.typography.titleSmall, color = IceWhite)
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
                                color = accent
                            )
                            Slider(
                                value = dspSettings.replayGainPreampDb,
                                onValueChange = { onSetReplayGain(dspSettings.replayGainMode, it) },
                                valueRange = -12f..12f
                            )
                        }

                        Text(
                            "Digital Level Trim: ${String.format(Locale.US, "%+.1f dB", dspSettings.digitalGainDb)}",
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
                                Text("Positive gain without limiter can cause distortion.", style = MaterialTheme.typography.bodySmall, color = AmberPeak)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Peak Limiter Protection", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                                Text("Prevents sudden harsh clipping", style = MaterialTheme.typography.bodySmall, color = MutedSteel)
                            }
                            Switch(
                                checked = dspSettings.limiterEnabled,
                                onCheckedChange = { onSetLimiter(it, dspSettings.limiterThresholdDb) }
                            )
                        }
                        if (dspSettings.limiterEnabled) {
                            Text(
                                "Threshold: ${String.format(Locale.US, "%.1f dBFS", dspSettings.limiterThresholdDb)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                            Slider(
                                value = dspSettings.limiterThresholdDb,
                                onValueChange = { onSetLimiter(true, it) },
                                valueRange = -6f..-0.1f
                            )
                        }

                        Text(
                            "Left / Right Balance: ${
                                when {
                                    dspSettings.stereoBalance < -0.05f -> "Left ${(-dspSettings.stereoBalance * 100).toInt()}%"
                                    dspSettings.stereoBalance > 0.05f -> "Right ${(dspSettings.stereoBalance * 100).toInt()}%"
                                    else -> "Centered"
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
                            Text("Mono Sum (Combine L+R)", style = MaterialTheme.typography.titleSmall, color = IceWhite)
                            Switch(
                                checked = dspSettings.monoDownmix,
                                onCheckedChange = { onSetStereoControls(dspSettings.stereoBalance, it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = surface,
            title = { Text("Quick Equalizer Guide", color = IceWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Simple Mode: Pick a mood like Vocal Clarity or Warm, and adjust Effect Strength.", color = SilverMist, style = MaterialTheme.typography.bodySmall)
                    Text("• Frequency (Hz): Low numbers (31–125 Hz) are bass, middle (500–2k Hz) are voices, high (6k–16k Hz) are crisp shimmer.", color = SilverMist, style = MaterialTheme.typography.bodySmall)
                    Text("• Gain (dB): Raises (+) or lowers (-) that frequency range.", color = SilverMist, style = MaterialTheme.typography.bodySmall)
                    Text("• Q Factor: Higher Q targets a narrow slice; lower Q adjusts a wide gentle hill.", color = SilverMist, style = MaterialTheme.typography.bodySmall)
                    Text("• Pure Direct: Turns off all EQ and DSP so audio passes straight through untouched.", color = SilverMist, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) { Text("Got It", color = accent) }
            }
        )
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = surface,
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
                }) { Text("Save", color = accent) }
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
            containerColor = surface,
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
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ObsidianBlack)
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; importError = null }) { Text("Cancel", color = SilverMist) }
            }
        )
    }
}
