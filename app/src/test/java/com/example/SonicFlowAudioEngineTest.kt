package com.example

import com.example.audio.dsp.BiquadFilter
import com.example.audio.dsp.EqPresets
import com.example.data.model.FilterType
import com.example.data.model.QualityTier
import com.example.data.model.classifyQualityTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SonicFlowAudioEngineTest {

    @Test
    fun `quality tier classifies hi-res flac, cd flac, and lossy mp3 accurately`() {
        assertEquals(QualityTier.HI_RES, classifyQualityTier("FLAC", "audio/flac", 96000, 24))
        assertEquals(QualityTier.HI_RES, classifyQualityTier("FLAC", "audio/flac", 192000, 24))
        assertEquals(QualityTier.LOSSLESS_CD, classifyQualityTier("FLAC", "audio/flac", 44100, 16))
        assertEquals(QualityTier.LOSSY, classifyQualityTier("MP3", "audio/mpeg", 44100, 16))
        assertEquals(QualityTier.UNKNOWN, classifyQualityTier("", "", 0, 0))
    }

    @Test
    fun `biquad peaking filter matches target gain at center frequency`() {
        val filter = BiquadFilter()
        filter.configure(FilterType.PEAKING, 96000, 1000f, 6.0f, 1.41f)
        val magAtCenter = filter.magnitudeDbAt(1000.0, 96000)
        val magFarAway = filter.magnitudeDbAt(18000.0, 96000)

        assertTrue("Expected ~+6.0 dB at 1 kHz, got $magAtCenter", abs(magAtCenter - 6.0) < 0.15)
        assertTrue("Expected near 0 dB far from center, got $magFarAway", abs(magFarAway) < 0.3)
    }

    @Test
    fun `all 9 built-in presets exist with valid band counts`() {
        assertEquals(9, EqPresets.builtInPresets.size)
        EqPresets.builtInPresets.forEach { preset ->
            assertTrue(preset.bands.size in 10..15)
        }
    }
}
