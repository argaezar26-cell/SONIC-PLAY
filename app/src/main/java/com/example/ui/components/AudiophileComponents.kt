package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.audio.dsp.BiquadFilter
import com.example.data.model.EqBand
import com.example.data.model.QualityTier
import com.example.data.model.Track
import com.example.data.model.VisualizerFrame
import com.example.ui.theme.CyanMuted
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.GraphiteHighlight
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.HiResGold
import com.example.ui.theme.IceWhite
import com.example.ui.theme.LosslessCyan
import com.example.ui.theme.LossySlate
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverMist
import com.example.ui.theme.SlateBorder
import java.io.File
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

enum class VisualizerMode(val label: String) {
    SPECTRUM_BARS("Spectrum Bars"),
    SMOOTH_WAVEFORM("Smooth Waveform"),
    FREQUENCY_CURVE("Frequency Spectrum")
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
fun QualityTierBadge(tier: QualityTier, modifier: Modifier = Modifier) {
    val (bg, fg) = when (tier) {
        QualityTier.HI_RES -> HiResGold.copy(alpha = 0.16f) to HiResGold
        QualityTier.LOSSLESS_CD -> LosslessCyan.copy(alpha = 0.16f) to LosslessCyan
        QualityTier.LOSSY -> LossySlate.copy(alpha = 0.16f) to LossySlate
        QualityTier.UNKNOWN -> GraphiteHighlight to MutedSteel
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
    Surface(
        modifier = modifier,
        color = if (highlighted) CyanMuted else GraphiteElevated,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (highlighted) ElectricCyan.copy(alpha = 0.55f) else SlateBorder)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) ElectricCyan else SilverMist,
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
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("mini_player_bar")
            .clickable(onClick = onClick),
        color = GraphiteElevated.copy(alpha = 0.96f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
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
                        color = ElectricCyan,
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
                    tint = ElectricCyan
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
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GraphiteSurface)
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
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
                                colors = listOf(ElectricCyan, ElectricBlue),
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
                            brush = Brush.horizontalGradient(listOf(ElectricBlue, ElectricCyan, ElectricBlue)),
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
                                listOf(ElectricCyan.copy(alpha = 0.38f), Color.Transparent)
                            )
                        )
                        drawPath(
                            path = linePath,
                            color = ElectricCyan,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }
        }
        if (!frame.isActive) {
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = MutedSteel, modifier = Modifier.size(16.dp))
                Text(
                    text = "Awaiting audio stream",
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
        targetValue = if (eqActive) ElectricCyan else MutedSteel,
        label = "eq_curve_color"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianBlack)
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

            // Horizontal dB grid lines (-12, -6, 0, +6, +12)
            listOf(-12f, -6f, 0f, 6f, 12f).forEach { db ->
                val y = dbToY(db)
                drawLine(
                    color = if (db == 0f) SlateBorder else SlateBorder.copy(alpha = 0.45f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (db == 0f) 1.5.dp.toPx() else 0.8.dp.toPx()
                )
            }

            // Vertical log frequency markers
            listOf(31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0).forEach { f ->
                val x = freqToX(f)
                drawLine(
                    color = SlateBorder.copy(alpha = 0.4f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 0.8.dp.toPx()
                )
            }

            // Curve path
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

            // Band control dots
            bands.forEach { band ->
                val x = freqToX(band.frequencyHz.toDouble())
                val y = dbToY(if (eqActive && band.enabled) band.gainDb else 0f)
                val isSelected = band.index == selectedBandIndex
                drawCircle(
                    color = if (isSelected) ElectricCyan else ElectricBlue,
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
