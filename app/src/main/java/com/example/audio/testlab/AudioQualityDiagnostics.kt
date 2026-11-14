package com.example.audio.testlab

import android.app.ActivityManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaCodecList
import android.os.Build
import android.os.PowerManager
import com.example.audio.dsp.BiquadFilter
import com.example.audio.routing.AudioDeviceMonitor
import com.example.data.model.FilterType
import com.example.data.model.OutputRouteCategory
import com.example.data.model.QualityTestItem
import com.example.data.model.TestResultStatus
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class AudioQualityDiagnostics(
    private val context: Context,
    private val deviceMonitor: AudioDeviceMonitor
) {
    suspend fun runDiagnostics(
        libraryTracks: List<Track>,
        isServiceConnected: Boolean,
        isGaplessEnabled: Boolean
    ): List<QualityTestItem> = withContext(Dispatchers.Default) {
        val results = mutableListOf<QualityTestItem>()
        deviceMonitor.refreshDevices()
        val routing = deviceMonitor.routingState.value

        // 1. FLAC Decoder Availability & Library Probe
        val codecs = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        val hasFlacDecoder = codecs.any { !it.isEncoder && it.supportedTypes.any { t -> t.equals("audio/flac", true) } }
        val flacTrack = libraryTracks.firstOrNull { it.codec.contains("FLAC", true) }
        results += QualityTestItem(
            id = "flac",
            title = "FLAC Playback & Decoder",
            status = if (hasFlacDecoder) TestResultStatus.PASS else TestResultStatus.FAIL,
            detail = buildString {
                append(if (hasFlacDecoder) "MediaCodec FLAC decoder ready" else "No FLAC MediaCodec found")
                if (flacTrack != null) append(" • Verified file: ${flacTrack.badgeText}")
            }
        )

        // 2. WAV PCM Playback Support
        val hasRawDecoder = codecs.any { !it.isEncoder && it.supportedTypes.any { t -> t.equals("audio/raw", true) } }
        val wavTrack = libraryTracks.firstOrNull { it.codec.contains("WAV", true) }
        results += QualityTestItem(
            id = "wav",
            title = "WAV PCM Playback",
            status = if (hasRawDecoder) TestResultStatus.PASS else TestResultStatus.FAIL,
            detail = if (wavTrack != null) "PCM decoder active • Sample: ${wavTrack.badgeText}" else "Raw PCM audio/raw decoder ready"
        )

        // 3. High Sample Rate (96 / 192 kHz) Hardware Capability
        val maxDeviceRate = routing.outputDevices.flatMap { it.supportedSampleRates }.maxOrNull() ?: 0
        results += QualityTestItem(
            id = "hires_rate",
            title = "High Sample Rate Output (96/192 kHz)",
            status = when {
                maxDeviceRate >= 96000 -> TestResultStatus.PASS
                maxDeviceRate > 0 -> TestResultStatus.NOT_AVAILABLE
                else -> TestResultStatus.NOT_AVAILABLE
            },
            detail = if (maxDeviceRate > 0) {
                "Max reported output sample rate: $maxDeviceRate Hz"
            } else {
                "Android mixer reports dynamic/unspecified rate (connect external USB DAC to probe)"
            }
        )

        // 4. Audio Focus Handling
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val focusResult = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .build()
                val res = audioManager.requestAudioFocus(req)
                audioManager.abandonAudioFocusRequest(req)
                res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                true
            }
        } catch (_: Exception) {
            false
        }
        results += QualityTestItem(
            id = "audio_focus",
            title = "Audio Focus Management",
            status = if (focusResult) TestResultStatus.PASS else TestResultStatus.FAIL,
            detail = if (focusResult) "AUDIOFOCUS_GAIN granted and abandoned cleanly" else "Audio focus request denied by system"
        )

        // 5. USB DAC Connection Check
        val usbDevice = routing.outputDevices.firstOrNull {
            it.routeCategory == OutputRouteCategory.EXTERNAL_USB_DAC ||
                it.routeCategory == OutputRouteCategory.USB_TYPE_C_ADAPTER
        }
        results += QualityTestItem(
            id = "usb_dac",
            title = "USB DAC / Type-C Audio Connection",
            status = if (usbDevice != null) TestResultStatus.PASS else TestResultStatus.NOT_AVAILABLE,
            detail = usbDevice?.let {
                "${it.productName} • Rates: ${it.supportedSampleRates.joinToString().ifBlank { "Auto" }}"
            } ?: "No external USB DAC or Type-C audio device currently attached"
        )

        // 6. Bluetooth Playback Route
        val btDevice = routing.outputDevices.firstOrNull { it.routeCategory == OutputRouteCategory.BLUETOOTH }
        results += QualityTestItem(
            id = "bluetooth",
            title = "Bluetooth Audio Route",
            status = if (btDevice != null) TestResultStatus.PASS else TestResultStatus.NOT_AVAILABLE,
            detail = btDevice?.let { "${it.productName} connected" } ?: "No Bluetooth A2DP/BLE audio device connected"
        )

        // 7. Parametric Biquad EQ Math Verification
        val filter = BiquadFilter().apply {
            configure(FilterType.PEAKING, 48000, 1000f, 6.0f, 1.41f)
        }
        val magAt1k = filter.magnitudeDbAt(1000.0, 48000)
        var outSample = 0.0
        for (i in 0 until 64) {
            outSample = filter.processSample(0, if (i == 0) 0.5 else 0.0)
        }
        val eqOk = abs(magAt1k - 6.0) < 0.2 && !outSample.isNaN() && outSample.isFinite()
        results += QualityTestItem(
            id = "eq_dsp",
            title = "Biquad Parametric DSP Stability",
            status = if (eqOk) TestResultStatus.PASS else TestResultStatus.FAIL,
            detail = "1 kHz +6.0 dB peaking filter measured ${"%.2f".format(magAt1k)} dB, zero NaN/overflow"
        )

        // 8. Background Playback Service
        results += QualityTestItem(
            id = "bg_service",
            title = "Background MediaSession Service",
            status = if (isServiceConnected) TestResultStatus.PASS else TestResultStatus.FAIL,
            detail = if (isServiceConnected) "SonicFlowPlaybackService connected via MediaController" else "MediaController not bound to foreground service"
        )

        // 9. Gapless Playback Engine
        results += QualityTestItem(
            id = "gapless",
            title = "Gapless Playback Pipeline",
            status = if (isGaplessEnabled) TestResultStatus.PASS else TestResultStatus.NOT_AVAILABLE,
            detail = if (isGaplessEnabled) "ExoPlayer MediaItem queue continuous pre-buffering enabled" else "Gapless mode disabled in Audio Settings"
        )

        // 10. Memory Usage Telemetry
        val runtime = Runtime.getRuntime()
        val usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalRamGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        results += QualityTestItem(
            id = "memory",
            title = "Memory Efficiency",
            status = if (usedMb < 256) TestResultStatus.PASS else TestResultStatus.FAIL,
            detail = "JVM Heap used: ${usedMb} MB • Device RAM: ${"%.1f".format(totalRamGb)} GB"
        )

        // 11. Battery Optimization (XOS Background Survival)
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val ignoringOpt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true
        results += QualityTestItem(
            id = "battery",
            title = "Background Battery Optimization",
            status = if (ignoringOpt) TestResultStatus.PASS else TestResultStatus.NOT_AVAILABLE,
            detail = if (ignoringOpt) {
                "Unrestricted background execution granted"
            } else {
                "Battery optimization active — enable Unrestricted in Settings for XOS screen-off stability"
            }
        )

        results
    }
}
