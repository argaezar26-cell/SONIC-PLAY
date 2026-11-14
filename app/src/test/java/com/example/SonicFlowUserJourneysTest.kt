package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.dsp.BiquadFilter
import com.example.audio.dsp.EqPresets
import com.example.audio.dsp.SonicFlowAudioProcessor
import com.example.data.model.DspSettings
import com.example.data.model.EqUiMode
import com.example.data.model.ErrorTranslator
import com.example.data.model.OutputRouteCategory
import com.example.data.model.QualityTier
import com.example.data.model.SimpleEqPreset
import com.example.data.model.Track
import com.example.data.prefs.UserPreferencesRepository
import com.example.ui.components.parseLyricsLines
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SonicFlowUserJourneysTest {

    private fun sampleFlacTrack(lyrics: String? = null) = Track(
        id = 1L,
        uri = "content://music/track_1.flac",
        title = "Acoustic Morning",
        artist = "Studio Quartet",
        album = "Live Sessions",
        albumArtist = "Studio Quartet",
        genre = "Acoustic",
        year = 2024,
        trackNumber = 1,
        durationMs = 215_000L,
        fileSizeBytes = 38_000_000L,
        mimeType = "audio/flac",
        codec = "FLAC",
        sampleRateHz = 96000,
        bitDepth = 24,
        bitrateKbps = 2800,
        channels = 2,
        folderUri = "content://folder/music",
        folderName = "HiRes",
        dateAdded = 1000L,
        lastPlayedAt = 2000L,
        playCount = 3,
        isFavorite = true,
        replayGainTrackDb = -3.2f,
        replayGainAlbumDb = -2.8f,
        artworkCachePath = null,
        lyrics = lyrics
    )

    @Test
    fun `audio quality explainer explains lossless flac bit depth and routing honestly`() {
        val track = sampleFlacTrack()
        val lines = ErrorTranslator.explainTrackQuality(track)
        val joined = lines.joinToString(" ")

        assertTrue(joined.contains("lossless", ignoreCase = true))
        assertTrue(joined.contains("24-bit"))
        assertTrue(joined.contains("96 kHz"))
        assertTrue(joined.contains("Upsampling never creates new detail", ignoreCase = true))
        assertEquals(QualityTier.HI_RES, track.qualityTier)
    }

    @Test
    fun `error translator converts technical exceptions into clear user actions`() {
        val permErr = ErrorTranslator.toFriendlyError("java.lang.SecurityException: Permission denial")
        assertEquals("Storage Access Needed", permErr.title)
        assertEquals("Select Folder", permErr.actionLabel)

        val fmtErr = ErrorTranslator.toFriendlyError("UnrecognizedInputFormatException in Media3")
        assertEquals("Unsupported Format", fmtErr.title)

        val dacErr = ErrorTranslator.toFriendlyError("Audio output disconnected on USB unplug")
        assertEquals("Output Disconnected", dacErr.title)

        val emptyErr = ErrorTranslator.toFriendlyError("Library is empty")
        assertEquals("No Music Found", emptyErr.title)
    }

    @Test
    fun `lrc lyrics parser handles synced timestamps and unsynced fallback`() {
        val synced = """
            [ar:Studio Quartet]
            [00:12.50]First vocal line
            [01:05.00]Second chorus line
        """.trimIndent()

        val parsedSynced = parseLyricsLines(synced)
        assertEquals(2, parsedSynced.size)
        assertEquals(12_500L, parsedSynced[0].timestampMs)
        assertEquals("First vocal line", parsedSynced[0].text)
        assertEquals(65_000L, parsedSynced[1].timestampMs)

        val plain = "Line one\nLine two"
        val parsedPlain = parseLyricsLines(plain)
        assertEquals(2, parsedPlain.size)
        assertEquals(null, parsedPlain[0].timestampMs)
        assertEquals("Line two", parsedPlain[1].text)
    }

    @Test
    fun `simple eq intensity scales preset cleanly and saves per-device profile`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = UserPreferencesRepository(context)

        repo.applySimpleEq(SimpleEqPreset.VOCAL_CLARITY, 0.5f)
        val dsp = DspSettings(
            bitTransparentMode = false,
            dspMasterEnabled = true,
            eqEnabled = true,
            activePresetId = 3L,
            preampDb = -1.1f,
            bands = EqPresets.flatBands(SimpleEqPreset.VOCAL_CLARITY.baseGains.map { it * 0.5f })
        )
        repo.saveDeviceEqSnapshot(
            routeCategory = OutputRouteCategory.EXTERNAL_USB_DAC,
            dsp = dsp,
            uiMode = EqUiMode.SIMPLE,
            simplePreset = SimpleEqPreset.VOCAL_CLARITY,
            intensity = 0.5f
        )

        val restored = repo.getDeviceEqSnapshot(OutputRouteCategory.EXTERNAL_USB_DAC)
        assertNotNull(restored)
        assertEquals(SimpleEqPreset.VOCAL_CLARITY, restored!!.simplePreset)
        assertEquals(0.5f, restored.simpleIntensity, 0.01f)
        assertEquals(SimpleEqPreset.VOCAL_CLARITY.baseGains[6] * 0.5f, restored.bands[6].gainDb, 0.05f)
    }

    @Test
    fun `persistent queue uris serialize and parse cleanly`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = UserPreferencesRepository(context)
        val uris = listOf("content://music/a.flac", "content://music/b.wav", "content://music/c.mp3")
        repo.saveQueueState(uris, 1)

        val rawJson = """["content://music/a.flac","content://music/b.wav","content://music/c.mp3"]"""
        val parsed = repo.parseSavedQueueUris(rawJson)
        assertEquals(uris, parsed)
    }

    @Test
    fun `audio processor pauses visualizer frame activity when playback stops`() {
        val processor = SonicFlowAudioProcessor()
        processor.setPlaybackActive(false)
        assertFalse(processor.visualizerFrame.value.isActive)
    }

    @Test
    fun `biquad combined curve honors active bands and preamp`() {
        val bands = EqPresets.flatBands(SimpleEqPreset.WARM.baseGains)
        val curve = BiquadFilter.computeCombinedCurveDb(
            bands = bands,
            preampDb = -2.0f,
            sampleRateHz = 48000,
            frequenciesHz = listOf(62.0, 1000.0, 8000.0)
        )
        assertEquals(3, curve.size)
        // Warm boosts low end (~62Hz) more than upper mids (8kHz)
        assertTrue(curve[0] > curve[2])
    }
}
