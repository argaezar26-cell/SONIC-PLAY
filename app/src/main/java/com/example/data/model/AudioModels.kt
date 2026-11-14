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

enum class EqUiMode(val label: String) {
    SIMPLE("Simple"),
    ADVANCED("Advanced")
}

enum class SimpleEqPreset(
    val title: String,
    val description: String,
    val baseGains: List<Float>,
    val basePreampDb: Float
) {
    NATURAL(
        title = "Natural",
        description = "Gentle, lifelike balance close to the original recording.",
        baseGains = listOf(0f, 0f, 0f, 0f, 0f, 0.3f, 0.5f, 0.3f, 0f, 0f, 0.3f, 0.3f),
        basePreampDb = -0.5f
    ),
    VOCAL_CLARITY(
        title = "Vocal Clarity",
        description = "Brings voices forward cleanly without harshness.",
        baseGains = listOf(-1.0f, -0.8f, -0.4f, -0.5f, 0.5f, 1.8f, 2.5f, 1.8f, 0.8f, 0.4f, 0.8f, 0.5f),
        basePreampDb = -2.2f
    ),
    WARM(
        title = "Warm",
        description = "Adds smooth low-end body and relaxes bright treble.",
        baseGains = listOf(1.4f, 1.8f, 1.2f, 0.6f, 0.2f, 0f, -0.4f, -0.8f, -0.8f, -0.4f, 0f, 0.2f),
        basePreampDb = -1.8f
    ),
    BRIGHT(
        title = "Bright",
        description = "Lifts crisp acoustic detail, cymbals, and airiness.",
        baseGains = listOf(-0.4f, -0.4f, 0f, 0f, 0.2f, 0.6f, 1.2f, 1.8f, 2.2f, 2.0f, 2.2f, 1.6f),
        basePreampDb = -2.0f
    ),
    BALANCED(
        title = "Balanced",
        description = "Even across bass, mids, and highs for long listening.",
        baseGains = listOf(0.6f, 0.4f, 0f, -0.3f, 0f, 0.4f, 0.6f, 0.6f, 0.4f, 0.2f, 0.5f, 0.5f),
        basePreampDb = -0.8f
    ),
    BASS_REDUCED(
        title = "Bass Reduced",
        description = "Reduces sub-bass boom for clearer speakers or late-night listening.",
        baseGains = listOf(-4.0f, -3.2f, -2.0f, -1.0f, 0f, 0.4f, 0.6f, 0.5f, 0.3f, 0f, 0.2f, 0.2f),
        basePreampDb = -0.5f
    ),
    CUSTOM(
        title = "Custom",
        description = "Your personalized tuning curve.",
        baseGains = List(12) { 0f },
        basePreampDb = 0f
    )
}

enum class AppThemeStyle(val label: String) {
    AMOLED_BLACK("AMOLED True Black"),
    GRAPHITE_DARK("Graphite Studio")
}

enum class AccentColorOption(val label: String) {
    ELECTRIC_CYAN("Electric Cyan"),
    COBALT_BLUE("Cobalt Blue"),
    EMERALD_STUDIO("Studio Emerald"),
    WARM_AMBER("Analog Amber")
}

enum class ArtworkStyle(val label: String) {
    ROUNDED_SQUARE("Rounded Frame"),
    VINYL_DISC("Vinyl Disc"),
    FULL_BLEED("Edge Card")
}

enum class PlayerLayoutMode(val label: String) {
    STANDARD("Standard"),
    COMPACT_ONE_HAND("One-Handed")
}

enum class LibrarySortOrder(val label: String) {
    TITLE_ASC("Title (A–Z)"),
    ARTIST_ASC("Artist (A–Z)"),
    DATE_ADDED_DESC("Date Added"),
    DURATION_DESC("Duration")
}

enum class LibraryFormatFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    HI_RES("Hi-Res"),
    LOSSLESS("FLAC / WAV"),
    LOSSY("MP3 / AAC / OGG")
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
    WIRED_USB("Wired Headphones", "USB-C adapter or wired earphones"),
    EXTERNAL_DAC("USB DAC", "External USB-C Hi-Res DAC/Amp"),
    BLUETOOTH("Bluetooth Headphones", "Wireless A2DP / BLE Audio")
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
    val artworkCachePath: String?,
    val lyrics: String? = null
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
    BUILT_IN_SPEAKERS("Built-in Speakers"),
    USB_TYPE_C_ADAPTER("Wired USB-C Headphones"),
    EXTERNAL_USB_DAC("External USB DAC"),
    BLUETOOTH("Bluetooth Headphones"),
    WIRED_HEADPHONES("Wired Headphones"),
    UNKNOWN("System Audio Output")
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

data class UserFriendlyError(
    val title: String,
    val message: String,
    val actionLabel: String? = null
)

object ErrorTranslator {
    fun toFriendlyError(raw: String?): UserFriendlyError {
        val text = raw.orEmpty().lowercase()
        return when {
            text.contains("permission") || text.contains("security") || text.contains("denied") -> UserFriendlyError(
                title = "Storage Access Needed",
                message = "Storage access is needed to open this music folder.",
                actionLabel = "Select Folder"
            )
            text.contains("unrecognizedinputformat") || text.contains("decoder") || text.contains("unsupported") || text.contains("format") -> UserFriendlyError(
                title = "Unsupported Format",
                message = "This audio format is not supported by the current decoder.",
                actionLabel = "Check Quality Tests"
            )
            text.contains("disconnected") || text.contains("unplug") -> UserFriendlyError(
                title = "Output Disconnected",
                message = "The audio output was disconnected. Playback paused safely or switched to an available output.",
                actionLabel = "Check Output"
            )
            text.contains("empty") || text.contains("no music") -> UserFriendlyError(
                title = "No Music Found",
                message = "Your music library is empty. Select a music folder to begin.",
                actionLabel = "Add Folder"
            )
            else -> UserFriendlyError(
                title = "Playback Notice",
                message = "Unable to open this audio file. Make sure the file still exists in its folder and try rescanning.",
                actionLabel = "Rescan Library"
            )
        }
    }

    fun explainTrackQuality(track: Track): List<String> {
        val codecUpper = track.codec.uppercase().ifBlank { "Audio" }
        val lines = mutableListOf<String>()
        when {
            codecUpper.contains("FLAC") -> lines += "FLAC uses lossless compression — every bit of the original studio audio is preserved without quality loss."
            codecUpper.contains("WAV") || codecUpper.contains("PCM") -> lines += "WAV stores uncompressed PCM audio directly, preserving the original waveform."
            codecUpper.contains("ALAC") -> lines += "ALAC is Apple Lossless compression, preserving 100% of original audio data."
            codecUpper.contains("MP3") || codecUpper.contains("AAC") || codecUpper.contains("OGG") || codecUpper.contains("OPUS") ->
                lines += "$codecUpper is a space-saving lossy format designed for smaller file sizes."
            else -> lines += "$codecUpper audio stream decoded natively by Media3."
        }
        if (track.bitDepth > 0) {
            val depthNote = if (track.bitDepth >= 24) "high dynamic range studio resolution" else "standard CD dynamic range"
            lines += "${track.bitDepth}-bit describes the source bit depth ($depthNote)."
        }
        if (track.sampleRateHz > 0) {
            val khz = String.format(java.util.Locale.US, "%.1f", track.sampleRateHz / 1000.0).removeSuffix(".0")
            lines += "$khz kHz describes how many audio snapshots per second are stored in the file."
        }
        lines += "Actual playback output may differ depending on Android audio routing and your connected speakers, headphones, or USB DAC. Upsampling never creates new detail that wasn't in the original recording."
        return lines
    }
}
