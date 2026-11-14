package com.example.audio.testlab

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

enum class TestToneMode(val label: String, val description: String) {
    LEFT_ONLY("Left Channel", "440 Hz tone on Left channel only"),
    RIGHT_ONLY("Right Channel", "440 Hz tone on Right channel only"),
    STEREO_CHECK("Stereo Separation", "Alternating L (440 Hz) and R (660 Hz) stereo check"),
    MONO_SUM("Mono Sum", "Identical in-phase 440 Hz center image"),
    SINE_SINGLE("Sine Generator", "Precision single-frequency sine tone"),
    FREQ_SWEEP("20 Hz–20 kHz Sweep", "Logarithmic full-range frequency sweep"),
    CLIPPING_TEST("Clipping Headroom Test", "0 dBFS vs -0.5 dBFS peak headroom verification")
}

data class ToneLabState(
    val isPlaying: Boolean = false,
    val activeMode: TestToneMode? = null,
    val sineFrequencyHz: Float = 1000f,
    val outputLevelDb: Float = -18f,
    val channelBalance: Float = 0f,
    val currentSweepHz: Float = 20f
)

class ToneGeneratorEngine {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var playJob: Job? = null

    private val _state = MutableStateFlow(ToneLabState())
    val state: StateFlow<ToneLabState> = _state.asStateFlow()

    fun setSineFrequency(hz: Float) {
        _state.value = _state.value.copy(sineFrequencyHz = hz.coerceIn(20f, 20000f))
    }

    fun setOutputLevelDb(db: Float) {
        _state.value = _state.value.copy(outputLevelDb = db.coerceIn(-40f, -1f))
    }

    fun setChannelBalance(balance: Float) {
        _state.value = _state.value.copy(channelBalance = balance.coerceIn(-1f, 1f))
    }

    fun startTone(mode: TestToneMode, sampleRateHz: Int = 48000) {
        stop()
        _state.value = _state.value.copy(isPlaying = true, activeMode = mode)

        playJob = scope.launch {
            val sr = sampleRateHz.coerceAtLeast(44100)
            val minBuf = AudioTrack.getMinBufferSize(
                sr,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sr)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val framesPerChunk = 1024
            val pcm = ShortArray(framesPerChunk * 2)
            var phaseL = 0.0
            var phaseR = 0.0
            var elapsedFrames = 0L

            try {
                track.play()
                while (isActive) {
                    val snapshot = _state.value
                    val baseAmp = if (mode == TestToneMode.CLIPPING_TEST) {
                        0.995
                    } else {
                        Math.pow(10.0, snapshot.outputLevelDb / 20.0).coerceIn(0.005, 0.85)
                    }
                    val bal = snapshot.channelBalance
                    val balL = if (bal > 0f) 1.0 - bal else 1.0
                    val balR = if (bal < 0f) 1.0 + bal else 1.0

                    for (i in 0 until framesPerChunk) {
                        val tSec = elapsedFrames.toDouble() / sr.toDouble()
                        var sampleL = 0.0
                        var sampleR = 0.0

                        when (mode) {
                            TestToneMode.LEFT_ONLY -> {
                                phaseL += 2.0 * PI * 440.0 / sr
                                sampleL = sin(phaseL) * baseAmp
                            }
                            TestToneMode.RIGHT_ONLY -> {
                                phaseR += 2.0 * PI * 440.0 / sr
                                sampleR = sin(phaseR) * baseAmp
                            }
                            TestToneMode.STEREO_CHECK -> {
                                val secSlot = (tSec % 2.0)
                                if (secSlot < 1.0) {
                                    phaseL += 2.0 * PI * 440.0 / sr
                                    sampleL = sin(phaseL) * baseAmp
                                } else {
                                    phaseR += 2.0 * PI * 660.0 / sr
                                    sampleR = sin(phaseR) * baseAmp
                                }
                            }
                            TestToneMode.MONO_SUM -> {
                                phaseL += 2.0 * PI * 440.0 / sr
                                val s = sin(phaseL) * baseAmp
                                sampleL = s
                                sampleR = s
                            }
                            TestToneMode.SINE_SINGLE -> {
                                val freq = snapshot.sineFrequencyHz.toDouble()
                                phaseL += 2.0 * PI * freq / sr
                                val s = sin(phaseL) * baseAmp
                                sampleL = s * balL
                                sampleR = s * balR
                            }
                            TestToneMode.FREQ_SWEEP -> {
                                val sweepDur = 8.0
                                val pos = (tSec % sweepDur) / sweepDur
                                val freq = 20.0 * exp(ln(1000.0) * pos)
                                if (i == 0) {
                                    _state.value = _state.value.copy(currentSweepHz = freq.toFloat())
                                }
                                phaseL += 2.0 * PI * freq / sr
                                val s = sin(phaseL) * baseAmp
                                sampleL = s * balL
                                sampleR = s * balR
                            }
                            TestToneMode.CLIPPING_TEST -> {
                                phaseL += 2.0 * PI * 1000.0 / sr
                                val s = (sin(phaseL) * 1.15).coerceIn(-1.0, 1.0) * baseAmp
                                sampleL = s
                                sampleR = s
                            }
                        }

                        if (phaseL > 2.0 * PI) phaseL -= 2.0 * PI
                        if (phaseR > 2.0 * PI) phaseR -= 2.0 * PI

                        pcm[i * 2] = (sampleL * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                        pcm[i * 2 + 1] = (sampleR * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                        elapsedFrames++
                    }
                    track.write(pcm, 0, pcm.size)
                }
            } finally {
                runCatching {
                    track.pause()
                    track.flush()
                    track.release()
                }
            }
        }
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
        _state.value = _state.value.copy(isPlaying = false, activeMode = null)
    }
}
