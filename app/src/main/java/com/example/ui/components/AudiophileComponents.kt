package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.audio.dsp.BiquadFilter
import com.example.data.model.ArtworkStyle
import com.example.data.model.EqBand
import com.example.data.model.ErrorTranslator
import com.example.data.model.PlaybackAudioInfo
import com.example.data.model.QualityTier
import com.example.data.model.Track
import com.example.data.model.UserFriendlyError
import com.example.data.model.VisualizerFrame
import com.example.ui.theme.ClipCrimson
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.HiResGold
import com.example.ui.theme.IceWhite
import com.example.ui.theme.LosslessCyan
import com.example.ui.theme.LossySlate
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverMist
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.VerifiedEmerald
import java.io.File
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

enum class VisualizerMode(val label: String) {
    SPECTRUM_BARS("Spectrum Bars"),
    SMOOTH_WAVEFORM("Smooth Waveform"),
    FREQUENCY_CURVE("Frequency Spectrum")
}

data class LyricLine(
    val timestampMs: Long?,
    val text: String
)

private val LRC_TAG_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")

fun parseLyricsLines(rawLyrics: String?): List<LyricLine> {
    if (rawLyrics.isNullOrBlank()) return emptyList()
    val result = mutableListOf<LyricLine>()
    rawLyrics.lines().forEach { line ->
        val matches = LRC_TAG_REGEX.findAll(line).toList()
        if (matches.isEmpty()) {
            val cleaned = line.trim()
            if (cleaned.isNotEmpty() && !cleaned.startsWith("[")) {
                result += LyricLine(timestampMs = null, text = cleaned)
            }
        } else {
            val lyricText = LRC_TAG_REGEX.replace(line, "").trim()
            if (lyricText.isNotEmpty()) {
                matches.forEach { m ->
                    val min = m.groupValues[1].toLongOrNull() ?: 0L
                    val sec = m.groupValues[2].toLongOrNull() ?: 0L
                    val fracStr = m.groupValues[3]
                    val msPart = when (fracStr.length) {
                        1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                        2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                        3 -> fracStr.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    result += LyricLine(timestampMs = min * 60_000L + sec * 1000L + msPart, text = lyricText)
                }
            }
        }
    }
    return if (result.any { it.timestampMs != null }) {
        result.sortedBy { it.timestampMs ?: Long.MAX_VALUE }
    } else {
        result
    }
}

@Composable
fun TrackArtworkImage(
    artworkPath: String?,
    artworkEnabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (artworkEnabled && !artworkPath.isNullOrBlank() && File(artworkPath).exists()) {
        AsyncImage(
            model = File(artworkPath),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            error = painterResource(R.drawable.img_default_vinyl_art),
            placeholder = painterResource(R.drawable.img_default_vinyl_art),
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(R.drawable.img_default_vinyl_art),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun StyledPlayerArtwork(
    artworkPath: String?,
    artworkEnabled: Boolean,
    artworkStyle: ArtworkStyle,
    isPlaying: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val shape = when (artworkStyle) {
        ArtworkStyle.ROUNDED_SQUARE -> RoundedCornerShape(24.dp)
        ArtworkStyle.VINYL_DISC -> CircleShape
        ArtworkStyle.FULL_BLEED -> RoundedCornerShape(14.dp)
    }

    val rotation = if (artworkStyle == ArtworkStyle.VINYL_DISC && isPlaying) {
        val infinite = rememberInfiniteTransition(label = "vinyl_spin")
        val angle by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 14000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "vinyl_angle"
        )
        angle
    } else 0f

    Box(
        modifier = modifier
            .clip(shape)
            .border(1.5.dp, accent.copy(alpha = 0.4f), shape)
            .background(surface),
        contentAlignment = Alignment.Center
    ) {
        TrackArtworkImage(
            artworkPath = artworkPath,
            artworkEnabled = artworkEnabled,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
        )
        if (artworkStyle == ArtworkStyle.VINYL_DISC) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ObsidianBlack.copy(alpha = 0.9f))
                    .border(2.dp, accent.copy(alpha = 0.7f), CircleShape)
            )
        }
    }
}

@Composable
fun LyricsPanel(
    lyrics: String?,
    positionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val lines = remember(lyrics) { parseLyricsLines(lyrics) }
    val activeIdx = remember(lines, positionMs) {
        if (lines.none { it.timestampMs != null }) -1
        else lines.indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= positionMs }
    }
    val listState = rememberLazyListState()
    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(activeIdx) {
        if (activeIdx >= 0 && activeIdx < lines.size) {
            listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (lines.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Lyrics, contentDescription = null, tint = MutedSteel, modifier = Modifier.size(40.dp))
                Text(
                    text = "No Lyrics Available",
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite
                )
                Text(
                    text = "SONICFLOW automatically reads embedded FLAC lyrics and matching .lrc files in your music folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSteel,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(lines) { idx, line ->
                    val isActive = idx == activeIdx
                    Text(
                        text = line.text,
                        style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                        color = if (isActive) accent else SilverMist.copy(alpha = 0.78f),
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = line.timestampMs != null) {
                                line.timestampMs?.let(onSeekTo)
                            }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AudioQualityExplainerSheetContent(
    track: Track,
    audioInfo: PlaybackAudioInfo
) {
    val lines = remember(track) { ErrorTranslator.explainTrackQuality(track) }
    var showTechnical by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.HighQuality, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
            Column {
                Text("Audio Quality Explained", style = MaterialTheme.typography.titleLarge, color = IceWhite)
                Text(track.badgeText, style = MaterialTheme.typography.labelMedium, color = accent)
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lines.forEach { line ->
                    Text(
                        text = "• $line",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SilverMist
                    )
                }
            }
        }

        TextButton(onClick = { showTechnical = !showTechnical }) {
            Text(
                text = if (showTechnical) "Hide Technical Signal Details" else "Show Technical Signal Details",
                color = accent
            )
        }

        if (showTechnical) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, SlateBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TechRow("Source Format", "${audioInfo.sourceCodec} (${track.qualityTier.label})")
                    TechRow("Source Sample Rate", if (audioInfo.sourceSampleRateHz > 0) "${audioInfo.sourceSampleRateHz} Hz" else "—")
                    TechRow("Source Bit Depth", if (audioInfo.sourceBitDepth > 0) "${audioInfo.sourceBitDepth}-bit" else "Variable")
                    TechRow("Decoder & PCM", "${audioInfo.decoderUsed} • ${audioInfo.decodedPcmEncoding}")
                    TechRow("Decoded Rate", if (audioInfo.decodedSampleRateHz > 0) "${audioInfo.decodedSampleRateHz} Hz" else "—")
                    TechRow("Active Processing", audioInfo.activeDspSummary)
                    TechRow("Output Route", audioInfo.outputRouteLabel)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TechRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedSteel, modifier = Modifier.weight(0.42f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = IceWhite, modifier = Modifier.weight(0.58f))
    }
}

@Composable
fun UserFriendlyErrorBanner(
    error: UserFriendlyError,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ClipCrimson.copy(alpha = 0.14f)),
        border = BorderStroke(1.dp, ClipCrimson.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = ClipCrimson)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(error.title, style = MaterialTheme.typography.titleSmall, color = IceWhite)
                    Text(error.message, style = MaterialTheme.typography.bodySmall, color = SilverMist)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = MutedSteel)
                }
            }
            if (error.actionLabel != null) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = ObsidianBlack
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(error.actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QualityTierBadge(tier: QualityTier, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tier) {
        QualityTier.HI_RES -> HiResGold.copy(alpha = 0.16f) to HiResGold
        QualityTier.LOSSLESS_CD -> LosslessCyan.copy(alpha = 0.16f) to LosslessCyan
        QualityTier.LOSSY -> LossySlate.copy(alpha = 0.16f) to LossySlate
        QualityTier.UNKNOWN -> MaterialTheme.colorScheme.surfaceContainerHighest to MutedSteel
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.4f))
    ) {
        Text(
            text = tier.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun AudioSpecPill(text: String, modifier: Modifier = Modifier, highlighted: Boolean = false) {
    val accent = MaterialTheme.colorScheme.primary
    val accentContainer = MaterialTheme.colorScheme.primaryContainer
    Surface(
        modifier = modifier,
        color = if (highlighted) accentContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (highlighted) accent.copy(alpha = 0.55f) else SlateBorder)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) accent else SilverMist,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun MiniPlayerBar(
    track: Track,
    isPlaying: Boolean,
    artworkEnabled: Boolean,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("mini_player_bar")
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TrackArtworkImage(
                artworkPath = track.artworkCachePath,
                artworkEnabled = artworkEnabled,
                contentDescription = track.album,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = IceWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSteel,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = track.badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        maxLines = 1
                    )
                }
            }
            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("mini_play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = accent
                )
            }
            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("mini_next_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next track",
                    tint = IceWhite
                )
            }
        }
    }
}

@Composable
fun RealTimeVisualizer(
    frame: VisualizerFrame,
    mode: VisualizerMode,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (isPlaying && frame.isActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                when (mode) {
                    VisualizerMode.SPECTRUM_BARS -> {
                        val bars = frame.spectrumMagnitudes
                        if (bars.isNotEmpty()) {
                            val gap = 3.dp.toPx()
                            val barW = ((w - gap * (bars.size - 1)) / bars.size).coerceAtLeast(2f)
                            bars.forEachIndexed { i, mag ->
                                val norm = mag.coerceIn(0.03f, 1f)
                                val barH = norm * h
                                val x = i * (barW + gap)
                                val y = h - barH
                                val brush = Brush.verticalGradient(
                                    colors = listOf(accent, ElectricBlue),
                                    startY = y,
                                    endY = h
                                )
                                drawRoundRect(
                                    brush = brush,
                                    topLeft = Offset(x, y),
                                    size = Size(barW, barH),
                                    cornerRadius = CornerRadius(barW / 2f, barW / 2f)
                                )
                            }
                        }
                    }
                    VisualizerMode.SMOOTH_WAVEFORM -> {
                        val samples = frame.waveformSamples
                        if (samples.size > 1) {
                            val midY = h / 2f
                            val path = Path()
                            val step = w / (samples.size - 1)
                            samples.forEachIndexed { idx, s ->
                                val x = idx * step
                                val y = midY - (s.coerceIn(-1f, 1f) * midY * 0.9f)
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawLine(
                                color = SlateBorder,
                                start = Offset(0f, midY),
                                end = Offset(w, midY),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawPath(
                                path = path,
                                brush = Brush.horizontalGradient(listOf(ElectricBlue, accent, ElectricBlue)),
                                style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    VisualizerMode.FREQUENCY_CURVE -> {
                        val bars = frame.spectrumMagnitudes
                        if (bars.size > 1) {
                            val linePath = Path()
                            val fillPath = Path()
                            val step = w / (bars.size - 1)
                            fillPath.moveTo(0f, h)
                            bars.forEachIndexed { idx, mag ->
                                val x = idx * step
                                val y = h - (mag.coerceIn(0.02f, 1f) * h * 0.92f)
                                if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                            fillPath.lineTo(w, h)
                            fillPath.close()
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    listOf(accent.copy(alpha = 0.38f), Color.Transparent)
                                )
                            )
                            drawPath(
                                path = linePath,
                                color = accent,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = MutedSteel, modifier = Modifier.size(16.dp))
                Text(
                    text = if (isPlaying) "Listening to audio stream" else "Visualizer paused to save battery",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedSteel
                )
            }
        }
    }
}

@Composable
fun ParametricEqResponseGraph(
    bands: List<EqBand>,
    preampDb: Float,
    selectedBandIndex: Int,
    sampleRateHz: Int,
    eqActive: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val freqPoints = remember {
        val count = 110
        val minF = 20.0
        val maxF = 20000.0
        List(count) { i ->
            minF * (maxF / minF).pow(i.toDouble() / (count - 1))
        }
    }

    val curveDb = remember(bands, preampDb, sampleRateHz, eqActive) {
        if (!eqActive) List(freqPoints.size) { 0.0 }
        else BiquadFilter.computeCombinedCurveDb(bands, preampDb, sampleRateHz, freqPoints)
    }

    val lineColor by animateColorAsState(
        targetValue = if (eqActive) accent else MutedSteel,
        label = "eq_curve_color"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minDb = -16f
            val maxDb = 16f

            fun dbToY(db: Float): Float {
                val norm = ((db - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
                return h - norm * h
            }

            fun freqToX(freq: Double): Float {
                val norm = (ln(freq.coerceIn(20.0, 20000.0) / 20.0) / ln(20000.0 / 20.0)).toFloat()
                return norm * w
            }

            listOf(-12f, -6f, 0f, 6f, 12f).forEach { db ->
                val y = dbToY(db)
                drawLine(
                    color = if (db == 0f) SlateBorder else SlateBorder.copy(alpha = 0.45f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (db == 0f) 1.5.dp.toPx() else 0.8.dp.toPx()
                )
            }

            listOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0).forEach { f ->
                val x = freqToX(f)
                drawLine(
                    color = SlateBorder.copy(alpha = 0.4f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 0.8.dp.toPx()
                )
            }

            val linePath = Path()
            val fillPath = Path()
            val zeroY = dbToY(0f)
            fillPath.moveTo(0f, zeroY)
            curveDb.forEachIndexed { idx, db ->
                val x = freqToX(freqPoints[idx])
                val y = dbToY(db.toFloat())
                if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
            fillPath.lineTo(w, zeroY)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
                )
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )

            bands.forEach { band ->
                val x = freqToX(band.frequencyHz.toDouble())
                val y = dbToY(if (eqActive && band.enabled) band.gainDb else 0f)
                val isSelected = band.index == selectedBandIndex
                drawCircle(
                    color = if (isSelected) accent else ElectricBlue,
                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                    center = Offset(x, y)
                )
                if (isSelected) {
                    drawCircle(
                        color = IceWhite,
                        radius = 2.5.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("20Hz", "100Hz", "1kHz", "10kHz", "20kHz").forEach { lbl ->
                Text(text = lbl, style = MaterialTheme.typography.labelSmall, color = MutedSteel)
            }
        }
    }
}

fun formatDurationMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000L
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(Locale.US, "%d:%02d", mins, secs)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) String.format(Locale.US, "%.1f MB", mb)
    else String.format(Locale.US, "%.0f KB", bytes / 1024.0)
}

fun formatHz(hz: Float): String {
    return if (hz >= 1000f) {
        val k = hz / 1000f
        if (k % 1f == 0f) "${k.toInt()} kHz" else String.format(Locale.US, "%.1f kHz", k)
    } else {
        "${hz.toInt()} Hz"
    }
}
