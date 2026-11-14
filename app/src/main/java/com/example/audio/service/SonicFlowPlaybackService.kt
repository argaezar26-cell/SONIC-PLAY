package com.example.audio.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.SonicFlowApplication
import com.example.data.model.BufferPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DecoderRuntimeTelemetry(
    val decoderName: String = "Media3 ExoPlayer",
    val decodedMimeType: String = "—",
    val decodedSampleRateHz: Int = 0,
    val decodedChannelCount: Int = 0,
    val decodedPcmEncoding: String = "—",
    val audioTrackSampleRateHz: Int = 0,
    val audioTrackEncoding: String = "—",
    val lastError: String? = null
)

@OptIn(UnstableApi::class)
class SonicFlowPlaybackService : MediaSessionService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as SonicFlowApplication
        val container = app.container

        var initialBuffer = BufferPreset.BALANCED
        var initialAudioFocus = true
        serviceScope.launch {
            val prefs = container.preferencesRepository.preferencesFlow.first()
            initialBuffer = prefs.bufferPreset
            initialAudioFocus = prefs.audioFocusEnabled
            container.audioProcessor.updateSettings(prefs.dspSettings)
            container.audioProcessor.setVisualizerEnabled(prefs.visualizerEnabled)
        }

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(arrayOf(container.audioProcessor))
                    .setEnableFloatOutput(true)
                    .setEnableAudioTrackPlaybackParams(false)
                    .build()
            }
        }.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                initialBuffer.minBufferMs,
                initialBuffer.maxBufferMs,
                initialBuffer.playbackMs,
                initialBuffer.playbackMs
            )
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this, extractorsFactory))
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, initialAudioFocus)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                updateTelemetry { it.copy(decoderName = decoderName, lastError = null) }
            }

            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                updateTelemetry {
                    it.copy(
                        decodedMimeType = format.sampleMimeType ?: "audio/pcm",
                        decodedSampleRateHz = if (format.sampleRate != Format.NO_VALUE) format.sampleRate else it.decodedSampleRateHz,
                        decodedChannelCount = if (format.channelCount != Format.NO_VALUE) format.channelCount else it.decodedChannelCount,
                        decodedPcmEncoding = pcmEncodingLabel(format.pcmEncoding)
                    )
                }
            }

            override fun onAudioTrackInitialized(
                eventTime: AnalyticsListener.EventTime,
                audioTrackConfig: AudioSink.AudioTrackConfig
            ) {
                updateTelemetry {
                    it.copy(
                        audioTrackSampleRateHz = audioTrackConfig.sampleRate,
                        audioTrackEncoding = pcmEncodingLabel(audioTrackConfig.encoding)
                    )
                }
            }
        })

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                updateTelemetry {
                    it.copy(lastError = error.localizedMessage ?: "Playback error (${error.errorCodeName})")
                }
            }
        })

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()

        exoPlayer = player
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        exoPlayer = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun pcmEncodingLabel(encoding: Int): String {
        return when (encoding) {
            C.ENCODING_PCM_16BIT -> "PCM 16-bit"
            C.ENCODING_PCM_24BIT, android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED -> "PCM 24-bit"
            C.ENCODING_PCM_32BIT -> "PCM 32-bit"
            C.ENCODING_PCM_FLOAT -> "PCM 32-bit Float"
            C.ENCODING_PCM_8BIT -> "PCM 8-bit"
            Format.NO_VALUE -> "Decoder Native"
            else -> "PCM ($encoding)"
        }
    }

    companion object {
        private val _decoderTelemetry = MutableStateFlow(DecoderRuntimeTelemetry())
        val decoderTelemetry: StateFlow<DecoderRuntimeTelemetry> = _decoderTelemetry.asStateFlow()

        private fun updateTelemetry(transform: (DecoderRuntimeTelemetry) -> DecoderRuntimeTelemetry) {
            _decoderTelemetry.value = transform(_decoderTelemetry.value)
        }
    }
}
