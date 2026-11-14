package com.example.data.model

enum class QualityTier(val label: String) {
    HI_RES("Hi-Res"),
    LOSSLESS_CD("Lossless CD Quality"),
    LOSSY("Lossy"),
    UNKNOWN("Unknown")
}

enum class FilterType(val displayName: String, val shortLabel: String) {
    PEAKING("Peaking", "PK"),
    LOW_SHELF("Low Shelf", "LS"),
    HIGH_SHELF("High Shelf", "HS"),
    HIGH_PASS("High-Pass", "HP"),
    LOW_PASS("Low-Pass", "LP"),
    NOTCH("Notch", "NT")
}

data class EqBand(
    val index: Int,
    val frequencyHz: Float,
    val gainDb: Float,
    val qFactor: Float,
    val filterType: FilterType,
    val enabled: Boolean = true
)

data class EqPreset(
    val id: Long = 0,
    val name: String,
    val isBuiltIn: Boolean = false,
    val preampDb: Float = 0f,
    val bands: List<EqBand>
)

enum class ReplayGainMode(val displayName: String) {
    OFF("Off"),
    TRACK("Track Gain"),
    ALBUM("Album Gain")
}

enum class BufferPreset(val displayName: String, val minBufferMs: Int, val maxBufferMs: Int, val playbackMs: Int) {
    LOW_LATENCY("Low Latency", 1500, 5000, 500),
    BALANCED("Balanced (Default)", 5000, 20000, 1500),
    HIGH_STABILITY("Hi-Res Stability", 10000, 45000, 3000)
}

enum class OutputProfileType(val displayName: String, val subtitle: String) {
    AUTO("Auto Route", "Follow Android active audio device"),
    PHONE_SPEAKERS("Phone Speakers", "Infinix Note 40 Dual JBL Stereo"),
    WIRED_USB("Wired USB Audio", "USB Type-C adapter or wired IEM"),
    EXTERNAL_DAC("External USB DAC", "Dedicated USB-C Hi-Res DAC/Amp"),
    BLUETOOTH("Bluetooth", "Wireless A2DP / BLE Audio")
}

enum class SoundProfileId(
    val title: String,
    val tagline: String,
    val description: String
) {
    PURE("PURE", "Bit-Transparent / Minimal", "Zero coloration. Disables EQ & DSP coloring for original signal integrity."),
    NATURAL("NATURAL", "Balanced Timbre", "Gentle tonal balance preserving acoustic realism and instrument body."),
    VOCAL("VOCAL", "Vocal Presence", "Focuses 1–4 kHz midrange clarity without sibilance or harshness."),
    CLARITY("CLARITY", "Air & Separation", "Tightens sub-bass bloom and lifts upper presence for micro-detail."),
    WARM("WARM", "Analog Warmth", "Subtle low-mid fullness and smooth upper-treble relaxation."),
    REFERENCE("REFERENCE", "Studio Monitor", "Flat reference contour with precision headroom for critical listening.")
}

data class Track(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: Int,
    val trackNumber: Int,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val mimeType: String,
    val codec: String,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val bitrateKbps: Int,
    val channels: Int,
    val folderUri: String,
    val folderName: String,
    val dateAdded: Long,
    val lastPlayedAt: Long,
    val playCount: Int,
    val isFavorite: Boolean,
    val replayGainTrackDb: Float?,
    val replayGainAlbumDb: Float?,
    val artworkCachePath: String?
) {
    val qualityTier: QualityTier
        get() = classifyQualityTier(codec, mimeType, sampleRateHz, bitDepth)

    val badgeText: String
        get() = buildString {
            append(codec.uppercase().ifBlank { "AUDIO" })
            if (bitDepth > 0) append(" | ${bitDepth}-bit")
            if (sampleRateHz > 0) {
                val khz = sampleRateHz / 1000.0
                val formatted = if (khz % 1.0 == 0.0) "${khz.toInt()}" else String.format(java.util.Locale.US, "%.1f", khz)
                append(" | $formatted kHz")
            }
        }
}

fun classifyQualityTier(codec: String, mimeType: String, sampleRateHz: Int, bitDepth: Int): QualityTier {
    val normalized = "${codec.lowercase()} ${mimeType.lowercase()}"
    val isLossless = normalized.contains("flac") ||
        normalized.contains("wav") ||
        normalized.contains("pcm") ||
        normalized.contains("alac") ||
        normalized.contains("aiff")
    val isLossy = normalized.contains("mp3") ||
        normalized.contains("mpeg") ||
        normalized.contains("aac") ||
        normalized.contains("mp4a") ||
        normalized.contains("vorbis") ||
        normalized.contains("ogg") ||
        normalized.contains("opus")
    return when {
        isLossless && (sampleRateHz > 48000 || bitDepth > 16) -> QualityTier.HI_RES
        isLossless -> QualityTier.LOSSLESS_CD
        isLossy -> QualityTier.LOSSY
        else -> QualityTier.UNKNOWN
    }
}

enum class OutputRouteCategory(val displayName: String) {
    BUILT_IN_SPEAKERS("Built-in JBL Stereo Speakers"),
    USB_TYPE_C_ADAPTER("USB Type-C Audio Adapter"),
    EXTERNAL_USB_DAC("External USB DAC"),
    BLUETOOTH("Bluetooth Output"),
    WIRED_HEADPHONES("Wired Headphones"),
    UNKNOWN("Unknown Output")
}

enum class MixerVerificationStatus(val label: String) {
    VERIFIED_ANDROID_MIXER("Verified Android mixer configuration"),
    COMPATIBLE_NOT_VERIFIED("Compatible, not verified"),
    UNSUPPORTED("Unsupported"),
    UNKNOWN("Unknown")
}

data class OutputDeviceSummary(
    val id: Int,
    val productName: String,
    val routeCategory: OutputRouteCategory,
    val deviceType: Int,
    val supportedSampleRates: List<Int>,
    val supportedEncodings: List<String>,
    val supportedChannelCounts: List<Int>,
    val bluetoothCodecHint: String? = null,
    val mixerStatus: MixerVerificationStatus = MixerVerificationStatus.UNKNOWN,
    val bitPerfectSupportedByMixer: Boolean = false,
    val activeMixerFormatSummary: String? = null
)

data class PlaybackAudioInfo(
    val sourceCodec: String = "—",
    val sourceSampleRateHz: Int = 0,
    val sourceBitDepth: Int = 0,
    val sourceBitrateKbps: Int = 0,
    val sourceChannels: Int = 0,
    val sourceDurationMs: Long = 0L,
    val sourceFileSizeBytes: Long = 0L,
    val qualityTier: QualityTier = QualityTier.UNKNOWN,
    val decoderUsed: String = "Media3 ExoPlayer",
    val decodedPcmEncoding: String = "—",
    val decodedSampleRateHz: Int = 0,
    val decodedChannelCount: Int = 0,
    val requestedSampleRateHz: Int = 0,
    val outputRouteLabel: String = "Detecting...",
    val activeDspSummary: String = "Bypassed",
    val processingState: String = "Idle",
    val isBitTransparentRequested: Boolean = true,
    val isBitTransparentActive: Boolean = false
)

data class DspSettings(
    val bitTransparentMode: Boolean = false,
    val dspMasterEnabled: Boolean = true,
    val eqEnabled: Boolean = true,
    val eqBypassBeforeAfter: Boolean = false,
    val activePresetId: Long = 1L,
    val preampDb: Float = 0f,
    val autoHeadroomEnabled: Boolean = true,
    val bands: List<EqBand> = emptyList(),
    val crossfeedEnabled: Boolean = false,
    val crossfeedIntensity: Float = 0.35f,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    val replayGainPreampDb: Float = 0f,
    val digitalGainDb: Float = 0f,
    val limiterEnabled: Boolean = true,
    val limiterThresholdDb: Float = -0.5f,
    val stereoBalance: Float = 0f,
    val monoDownmix: Boolean = false
)

data class DspTelemetry(
    val peakLeftDbfs: Float = -96f,
    val peakRightDbfs: Float = -96f,
    val isClipping: Boolean = false,
    val clippedSampleCount: Long = 0L,
    val appliedHeadroomDb: Float = 0f,
    val isDspActuallyActive: Boolean = false
)

data class VisualizerFrame(
    val spectrumMagnitudes: List<Float> = List(48) { 0f },
    val waveformSamples: List<Float> = List(128) { 0f },
    val isActive: Boolean = false
)

enum class TestResultStatus(val label: String) {
    PASS("PASS"),
    FAIL("FAIL"),
    NOT_AVAILABLE("NOT AVAILABLE"),
    PENDING("PENDING")
}

data class QualityTestItem(
    val id: String,
    val title: String,
    val status: TestResultStatus,
    val detail: String
)
