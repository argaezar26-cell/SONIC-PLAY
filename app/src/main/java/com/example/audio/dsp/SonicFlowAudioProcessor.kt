package com.example.audio.dsp

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.data.model.DspSettings
import com.example.data.model.DspTelemetry
import com.example.data.model.ReplayGainMode
import com.example.data.model.VisualizerFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Real-time Media3 AudioProcessor providing:
 * - Bit-transparent passthrough when DSP is disabled or Bit-Transparent mode is active
 * - 64-bit Biquad Parametric EQ (10-15 bands)
 * - Auto-headroom & Preamp compensation
 * - ReplayGain Track/Album gain
 * - Bauer-style Stereo Crossfeed for headphones
 * - Stereo balance & Mono downmix
 * - Soft-knee Peak Limiter & real-time clipping telemetry
 * - Real-time 512-point Hann-windowed FFT spectrum & waveform tap from the actual audio stream
 */
@OptIn(UnstableApi::class)
class SonicFlowAudioProcessor : BaseAudioProcessor() {

    @Volatile
    private var settings: DspSettings = DspSettings(bands = EqPresets.flatBands())

    @Volatile
    private var currentTrackGainDb: Float? = null

    @Volatile
    private var currentAlbumGainDb: Float? = null

    @Volatile
    private var visualizerEnabled: Boolean = true

    @Volatile
    private var playbackActive: Boolean = false

    @Volatile
    private var configDirty: Boolean = true

    private var activeSampleRate = 48000
    private var activeChannelCount = 2
    private var activeEncoding = C.ENCODING_PCM_16BIT

    // Biquad chain rebuilt only when dirty on the audio thread
    private var filters: List<BiquadFilter> = emptyList()

    // Crossfeed delay line & lowpass filter state
    private val crossfeedDelaySamples = 14 // ~0.3ms at 48kHz
    private val leftDelayRing = DoubleArray(64)
    private val rightDelayRing = DoubleArray(64)
    private var delayWritePos = 0
    private var lpLeft = 0.0
    private var lpRight = 0.0

    // Telemetry state
    private val _telemetry = MutableStateFlow(DspTelemetry())
    val telemetry: StateFlow<DspTelemetry> = _telemetry.asStateFlow()

    private val _visualizerFrame = MutableStateFlow(VisualizerFrame())
    val visualizerFrame: StateFlow<VisualizerFrame> = _visualizerFrame.asStateFlow()

    private val _currentFormat = MutableStateFlow(AudioFormat.NOT_SET)
    val currentFormat: StateFlow<AudioFormat> = _currentFormat.asStateFlow()

    private var clippedCount = 0L
    private var peakHoldL = 0.0
    private var peakHoldR = 0.0
    private var samplesSinceTelemetry = 0

    // FFT buffers (512 samples)
    private val fftSize = 512
    private val fftBuffer = FloatArray(fftSize)
    private var fftWriteIdx = 0
    private val fftReal = DoubleArray(fftSize)
    private val fftImag = DoubleArray(fftSize)
    private val hannWindow = DoubleArray(fftSize) { i ->
        0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))
    }
    private val smoothedSpectrum = FloatArray(48)

    fun updateSettings(newSettings: DspSettings) {
        settings = newSettings
        configDirty = true
    }

    fun updateReplayGainMetadata(trackGainDb: Float?, albumGainDb: Float?) {
        currentTrackGainDb = trackGainDb
        currentAlbumGainDb = albumGainDb
    }

    fun setVisualizerEnabled(enabled: Boolean) {
        visualizerEnabled = enabled
        if (!enabled) {
            _visualizerFrame.value = VisualizerFrame(isActive = false)
        }
    }

    fun setPlaybackActive(active: Boolean) {
        playbackActive = active
        if (!active) {
            _visualizerFrame.value = _visualizerFrame.value.copy(isActive = false)
        }
    }

    fun resetClipCounter() {
        clippedCount = 0L
        _telemetry.value = _telemetry.value.copy(isClipping = false, clippedSampleCount = 0L)
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val enc = inputAudioFormat.encoding
        if (enc != C.ENCODING_PCM_16BIT &&
            enc != C.ENCODING_PCM_24BIT &&
            enc != C.ENCODING_PCM_32BIT &&
            enc != C.ENCODING_PCM_FLOAT
        ) {
            throw UnhandledAudioFormatException(inputAudioFormat)
        }
        activeSampleRate = inputAudioFormat.sampleRate
        activeChannelCount = inputAudioFormat.channelCount
        activeEncoding = enc
        _currentFormat.value = inputAudioFormat
        configDirty = true
        return inputAudioFormat
    }

    override fun onFlush() {
        filters.forEach { it.reset() }
        leftDelayRing.fill(0.0)
        rightDelayRing.fill(0.0)
        delayWritePos = 0
        lpLeft = 0.0
        lpRight = 0.0
        peakHoldL = 0.0
        peakHoldR = 0.0
    }

    override fun onReset() {
        onFlush()
        _currentFormat.value = AudioFormat.NOT_SET
        _visualizerFrame.value = VisualizerFrame(isActive = false)
        _telemetry.value = DspTelemetry()
    }

    private fun rebuildFiltersIfNeeded(snapshot: DspSettings) {
        if (!configDirty) return
        configDirty = false
        val newFilters = snapshot.bands.filter { it.enabled }.map { band ->
            BiquadFilter().apply {
                configure(
                    type = band.filterType,
                    sampleRateHz = activeSampleRate,
                    frequencyHz = band.frequencyHz,
                    gainDb = band.gainDb,
                    qFactor = band.qFactor
                )
            }
        }
        filters = newFilters
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val snapshot = settings
        val dspActive = !snapshot.bitTransparentMode && snapshot.dspMasterEnabled
        val eqActive = dspActive && snapshot.eqEnabled && !snapshot.eqBypassBeforeAfter

        val outputBuffer = replaceOutputBuffer(remaining)

        val runVisualizer = visualizerEnabled && playbackActive

        // Fast bit-identical copy if bit-transparent or DSP bypassed and visualizer disabled/paused
        if (!dspActive && !runVisualizer) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            _telemetry.value = DspTelemetry(isDspActuallyActive = false)
            return
        }

        if (eqActive) {
            rebuildFiltersIfNeeded(snapshot)
        }

        val bytesPerSample = when (activeEncoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_24BIT -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4
            else -> 2
        }
        val frameSize = bytesPerSample * activeChannelCount
        if (frameSize <= 0) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        // Compute total gain (Preamp + Auto Headroom + ReplayGain + Digital Gain)
        var headroomCompDb = 0f
        var totalGainLinear = 1.0
        if (dspActive) {
            val maxPositiveBandDb = if (eqActive) {
                snapshot.bands.filter { it.enabled && it.gainDb > 0f }.maxOfOrNull { it.gainDb } ?: 0f
            } else 0f
            headroomCompDb = if (snapshot.autoHeadroomEnabled && maxPositiveBandDb > 0f) {
                -maxPositiveBandDb * 0.7f
            } else 0f

            val rgDb = when (snapshot.replayGainMode) {
                ReplayGainMode.OFF -> 0f
                ReplayGainMode.TRACK -> (currentTrackGainDb ?: 0f) + snapshot.replayGainPreampDb
                ReplayGainMode.ALBUM -> (currentAlbumGainDb ?: currentTrackGainDb ?: 0f) + snapshot.replayGainPreampDb
            }
            val preamp = if (eqActive) snapshot.preampDb else 0f
            val totalDb = (preamp + headroomCompDb + rgDb + snapshot.digitalGainDb).coerceIn(-30f, 18f)
            totalGainLinear = 10.0.pow(totalDb / 20.0)
        }

        val balance = snapshot.stereoBalance.coerceIn(-1f, 1f)
        val leftBalGain = if (balance > 0f) (1f - balance).toDouble() else 1.0
        val rightBalGain = if (balance < 0f) (1f + balance).toDouble() else 1.0

        val crossfeedActive = dspActive && snapshot.crossfeedEnabled && activeChannelCount >= 2
        val crossfeedMix = snapshot.crossfeedIntensity.coerceIn(0f, 1f).toDouble() * 0.45
        val lpCoeff = expSmoothingCoeff(700.0, activeSampleRate)

        val limiterActive = dspActive && snapshot.limiterEnabled
        val limiterThresh = 10.0.pow(snapshot.limiterThresholdDb.coerceIn(-12f, -0.05f) / 20.0)

        val frameCount = remaining / frameSize
        val currentFilters = if (eqActive) filters else emptyList()

        for (f in 0 until frameCount) {
            if (!dspActive) {
                // Bit-transparent: read for visualizer only, write exact original bytes!
                var monoSum = 0.0
                for (ch in 0 until activeChannelCount) {
                    val sample = readSampleAsDoubleAndCopy(inputBuffer, outputBuffer, activeEncoding)
                    monoSum += sample
                }
                val mono = monoSum / activeChannelCount
                peakHoldL = max(peakHoldL, abs(mono))
                peakHoldR = max(peakHoldR, abs(mono))
                if (runVisualizer) {
                    pushVisualizerSample(mono.toFloat())
                }
            } else {
                // Read frame
                val frameSamples = DoubleArray(activeChannelCount)
                for (ch in 0 until activeChannelCount) {
                    frameSamples[ch] = readSampleAsDouble(inputBuffer, activeEncoding)
                }

                // Gain + EQ per channel
                for (ch in 0 until activeChannelCount) {
                    var s = frameSamples[ch] * totalGainLinear
                    for (filter in currentFilters) {
                        s = filter.processSample(ch, s)
                    }
                    frameSamples[ch] = s
                }

                // Stereo Crossfeed / Balance / Mono on channels 0 & 1
                if (activeChannelCount >= 2) {
                    var l = frameSamples[0]
                    var r = frameSamples[1]

                    if (crossfeedActive) {
                        leftDelayRing[delayWritePos] = l
                        rightDelayRing[delayWritePos] = r
                        val readPos = (delayWritePos - crossfeedDelaySamples + leftDelayRing.size) % leftDelayRing.size
                        val delayedL = leftDelayRing[readPos]
                        val delayedR = rightDelayRing[readPos]
                        delayWritePos = (delayWritePos + 1) % leftDelayRing.size

                        lpLeft += lpCoeff * (delayedL - lpLeft)
                        lpRight += lpCoeff * (delayedR - lpRight)

                        val directGain = 1.0 - crossfeedMix * 0.35
                        val newL = l * directGain + lpRight * crossfeedMix
                        val newR = r * directGain + lpLeft * crossfeedMix
                        l = newL
                        r = newR
                    }

                    if (snapshot.monoDownmix) {
                        val mono = (l + r) * 0.5
                        l = mono
                        r = mono
                    } else {
                        l *= leftBalGain
                        r *= rightBalGain
                    }

                    frameSamples[0] = l
                    frameSamples[1] = r
                }

                // Soft-knee limiter + clip detection + write back
                for (ch in 0 until activeChannelCount) {
                    var s = frameSamples[ch]
                    if (limiterActive && abs(s) > limiterThresh) {
                        val sign = if (s >= 0.0) 1.0 else -1.0
                        val excess = abs(s) - limiterThresh
                        val headroom = (1.0 - limiterThresh).coerceAtLeast(1e-4)
                        s = sign * (limiterThresh + headroom * tanh(excess / headroom))
                    }

                    if (abs(s) > 0.999) {
                        clippedCount++
                    }
                    if (ch == 0) peakHoldL = max(peakHoldL, abs(s))
                    if (ch == 1) peakHoldR = max(peakHoldR, abs(s))

                    writeDoubleSample(outputBuffer, s.coerceIn(-1.0, 1.0), activeEncoding)
                }

                if (runVisualizer) {
                    val mono = if (activeChannelCount >= 2) {
                        ((frameSamples[0] + frameSamples[1]) * 0.5).toFloat()
                    } else {
                        frameSamples[0].toFloat()
                    }
                    pushVisualizerSample(mono)
                }
            }

            samplesSinceTelemetry++
            if (samplesSinceTelemetry >= (activeSampleRate / 20).coerceAtLeast(1024)) {
                samplesSinceTelemetry = 0
                val lDb = (20.0 * log10(peakHoldL.coerceAtLeast(1e-5))).toFloat().coerceIn(-96f, 0f)
                val rDb = (20.0 * log10(peakHoldR.coerceAtLeast(1e-5))).toFloat().coerceIn(-96f, 0f)
                _telemetry.value = DspTelemetry(
                    peakLeftDbfs = lDb,
                    peakRightDbfs = rDb,
                    isClipping = clippedCount > 0L,
                    clippedSampleCount = clippedCount,
                    appliedHeadroomDb = headroomCompDb,
                    isDspActuallyActive = dspActive
                )
                peakHoldL *= 0.65
                peakHoldR *= 0.65
            }
        }

        // Copy any trailing partial bytes if present
        while (inputBuffer.hasRemaining()) {
            outputBuffer.put(inputBuffer.get())
        }
        outputBuffer.flip()
    }

    private fun pushVisualizerSample(sample: Float) {
        fftBuffer[fftWriteIdx++] = sample
        if (fftWriteIdx >= fftSize) {
            fftWriteIdx = 0
            computeAndEmitVisualizerFrame()
        }
    }

    private fun computeAndEmitVisualizerFrame() {
        for (i in 0 until fftSize) {
            fftReal[i] = fftBuffer[i].toDouble() * hannWindow[i]
            fftImag[i] = 0.0
        }
        fftInPlace(fftReal, fftImag)

        val bars = 48
        val half = fftSize / 2
        val newBars = FloatArray(bars)
        val minBin = 1.0
        val maxBin = half.toDouble()

        for (b in 0 until bars) {
            val t0 = b.toDouble() / bars
            val t1 = (b + 1).toDouble() / bars
            val startBin = (minBin * (maxBin / minBin).pow(t0)).toInt().coerceIn(1, half - 1)
            val endBin = max(startBin + 1, (minBin * (maxBin / minBin).pow(t1)).toInt().coerceIn(startBin + 1, half))
            var maxMag = 0.0
            for (k in startBin until endBin) {
                val mag = hypot(fftReal[k], fftImag[k]) / (fftSize * 0.5)
                if (mag > maxMag) maxMag = mag
            }
            val db = 20.0 * log10(maxMag.coerceAtLeast(1e-5))
            val norm = ((db + 64.0) / 64.0).toFloat().coerceIn(0f, 1f)
            val prev = smoothedSpectrum[b]
            val smooth = if (norm > prev) norm * 0.75f + prev * 0.25f else prev * 0.78f + norm * 0.22f
            smoothedSpectrum[b] = smooth
            newBars[b] = smooth
        }

        val waveStep = fftSize / 128
        val waveform = List(128) { idx ->
            fftBuffer[(idx * waveStep).coerceIn(0, fftSize - 1)].coerceIn(-1f, 1f)
        }

        _visualizerFrame.value = VisualizerFrame(
            spectrumMagnitudes = newBars.toList(),
            waveformSamples = waveform,
            isActive = true
        )
    }

    private fun fftInPlace(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = real[i]; real[i] = real[j]; real[j] = tr
                val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wLenRe = cos(ang)
            val wLenIm = sin(ang)
            var i = 0
            while (i < n) {
                var wRe = 1.0
                var wIm = 0.0
                for (k in 0 until len / 2) {
                    val uRe = real[i + k]
                    val uIm = imag[i + k]
                    val vRe = real[i + k + len / 2] * wRe - imag[i + k + len / 2] * wIm
                    val vIm = real[i + k + len / 2] * wIm + imag[i + k + len / 2] * wRe
                    real[i + k] = uRe + vRe
                    imag[i + k] = uIm + vIm
                    real[i + k + len / 2] = uRe - vRe
                    imag[i + k + len / 2] = uIm - vIm
                    val nextWRe = wRe * wLenRe - wIm * wLenIm
                    wIm = wRe * wLenIm + wIm * wLenRe
                    wRe = nextWRe
                }
                i += len
            }
            len = len shl 1
        }
    }

    private fun expSmoothingCoeff(cutoffHz: Double, sampleRate: Int): Double {
        if (sampleRate <= 0) return 0.1
        return 1.0 - kotlin.math.exp(-2.0 * PI * cutoffHz / sampleRate.toDouble())
    }

    private fun readSampleAsDoubleAndCopy(inBuf: ByteBuffer, outBuf: ByteBuffer, encoding: Int): Double {
        return when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                val s = inBuf.short
                outBuf.putShort(s)
                s.toDouble() / 32768.0
            }
            C.ENCODING_PCM_24BIT -> {
                val b0 = inBuf.get()
                val b1 = inBuf.get()
                val b2 = inBuf.get()
                outBuf.put(b0); outBuf.put(b1); outBuf.put(b2)
                val v = ((b0.toInt() and 0xFF) or ((b1.toInt() and 0xFF) shl 8) or (b2.toInt() shl 16))
                v.toDouble() / 8388608.0
            }
            C.ENCODING_PCM_32BIT -> {
                val i = inBuf.int
                outBuf.putInt(i)
                i.toDouble() / 2147483648.0
            }
            C.ENCODING_PCM_FLOAT -> {
                val f = inBuf.float
                outBuf.putFloat(f)
                f.toDouble()
            }
            else -> 0.0
        }
    }

    private fun readSampleAsDouble(inBuf: ByteBuffer, encoding: Int): Double {
        return when (encoding) {
            C.ENCODING_PCM_16BIT -> inBuf.short.toDouble() / 32768.0
            C.ENCODING_PCM_24BIT -> {
                val b0 = inBuf.get().toInt() and 0xFF
                val b1 = inBuf.get().toInt() and 0xFF
                val b2 = inBuf.get().toInt()
                ((b0 or (b1 shl 8) or (b2 shl 16))).toDouble() / 8388608.0
            }
            C.ENCODING_PCM_32BIT -> inBuf.int.toDouble() / 2147483648.0
            C.ENCODING_PCM_FLOAT -> inBuf.float.toDouble()
            else -> 0.0
        }
    }

    private fun writeDoubleSample(outBuf: ByteBuffer, sample: Double, encoding: Int) {
        when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                val v = (sample * 32767.0).toInt().coerceIn(-32768, 32767).toShort()
                outBuf.putShort(v)
            }
            C.ENCODING_PCM_24BIT -> {
                val v = (sample * 8388607.0).toInt().coerceIn(-8388608, 8388607)
                outBuf.put((v and 0xFF).toByte())
                outBuf.put(((v shr 8) and 0xFF).toByte())
                outBuf.put(((v shr 16) and 0xFF).toByte())
            }
            C.ENCODING_PCM_32BIT -> {
                val v = (sample * 2147483647.0).toLong().coerceIn(-2147483648L, 2147483647L).toInt()
                outBuf.putInt(v)
            }
            C.ENCODING_PCM_FLOAT -> {
                outBuf.putFloat(sample.toFloat())
            }
        }
    }
}
