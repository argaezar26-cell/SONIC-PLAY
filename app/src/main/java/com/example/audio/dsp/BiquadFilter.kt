package com.example.audio.dsp

import com.example.data.model.EqBand
import com.example.data.model.FilterType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 64-bit Direct Form II Transposed Biquad Filter implementing the Audio EQ Cookbook formulas.
 */
class BiquadFilter {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    // State per channel (supports up to 8 channels; standard stereo uses 0 and 1)
    private val z1 = DoubleArray(8)
    private val z2 = DoubleArray(8)

    fun reset() {
        z1.fill(0.0)
        z2.fill(0.0)
    }

    fun configure(
        type: FilterType,
        sampleRateHz: Int,
        frequencyHz: Float,
        gainDb: Float,
        qFactor: Float
    ) {
        if (sampleRateHz <= 0) {
            setBypass()
            return
        }
        val nyquist = sampleRateHz * 0.495
        val f0 = frequencyHz.toDouble().coerceIn(15.0, nyquist)
        val q = qFactor.toDouble().coerceIn(0.2, 12.0)
        val w0 = 2.0 * PI * f0 / sampleRateHz.toDouble()
        val cosW0 = cos(w0)
        val sinW0 = sin(w0)
        val alpha = sinW0 / (2.0 * q)
        val aGain = 10.0.pow(gainDb.toDouble() / 40.0)

        var rawB0 = 1.0
        var rawB1 = 0.0
        var rawB2 = 0.0
        var rawA0 = 1.0
        var rawA1 = 0.0
        var rawA2 = 0.0

        when (type) {
            FilterType.PEAKING -> {
                if (kotlin.math.abs(gainDb) < 0.01f) {
                    setBypass()
                    return
                }
                rawB0 = 1.0 + alpha * aGain
                rawB1 = -2.0 * cosW0
                rawB2 = 1.0 - alpha * aGain
                rawA0 = 1.0 + alpha / aGain
                rawA1 = -2.0 * cosW0
                rawA2 = 1.0 - alpha / aGain
            }
            FilterType.LOW_SHELF -> {
                if (kotlin.math.abs(gainDb) < 0.01f) {
                    setBypass()
                    return
                }
                val beta = sqrt(aGain) / q
                rawB0 = aGain * ((aGain + 1.0) - (aGain - 1.0) * cosW0 + beta * sinW0)
                rawB1 = 2.0 * aGain * ((aGain - 1.0) - (aGain + 1.0) * cosW0)
                rawB2 = aGain * ((aGain + 1.0) - (aGain - 1.0) * cosW0 - beta * sinW0)
                rawA0 = (aGain + 1.0) + (aGain - 1.0) * cosW0 + beta * sinW0
                rawA1 = -2.0 * ((aGain - 1.0) + (aGain + 1.0) * cosW0)
                rawA2 = (aGain + 1.0) + (aGain - 1.0) * cosW0 - beta * sinW0
            }
            FilterType.HIGH_SHELF -> {
                if (kotlin.math.abs(gainDb) < 0.01f) {
                    setBypass()
                    return
                }
                val beta = sqrt(aGain) / q
                rawB0 = aGain * ((aGain + 1.0) + (aGain - 1.0) * cosW0 + beta * sinW0)
                rawB1 = -2.0 * aGain * ((aGain - 1.0) + (aGain + 1.0) * cosW0)
                rawB2 = aGain * ((aGain + 1.0) + (aGain - 1.0) * cosW0 - beta * sinW0)
                rawA0 = (aGain + 1.0) - (aGain - 1.0) * cosW0 + beta * sinW0
                rawA1 = 2.0 * ((aGain - 1.0) - (aGain + 1.0) * cosW0)
                rawA2 = (aGain + 1.0) - (aGain - 1.0) * cosW0 - beta * sinW0
            }
            FilterType.HIGH_PASS -> {
                rawB0 = (1.0 + cosW0) / 2.0
                rawB1 = -(1.0 + cosW0)
                rawB2 = (1.0 + cosW0) / 2.0
                rawA0 = 1.0 + alpha
                rawA1 = -2.0 * cosW0
                rawA2 = 1.0 - alpha
            }
            FilterType.LOW_PASS -> {
                rawB0 = (1.0 - cosW0) / 2.0
                rawB1 = 1.0 - cosW0
                rawB2 = (1.0 - cosW0) / 2.0
                rawA0 = 1.0 + alpha
                rawA1 = -2.0 * cosW0
                rawA2 = 1.0 - alpha
            }
            FilterType.NOTCH -> {
                rawB0 = 1.0
                rawB1 = -2.0 * cosW0
                rawB2 = 1.0
                rawA0 = 1.0 + alpha
                rawA1 = -2.0 * cosW0
                rawA2 = 1.0 - alpha
            }
        }

        if (rawA0 == 0.0) {
            setBypass()
            return
        }

        b0 = rawB0 / rawA0
        b1 = rawB1 / rawA0
        b2 = rawB2 / rawA0
        a1 = rawA1 / rawA0
        a2 = rawA2 / rawA0
    }

    private fun setBypass() {
        b0 = 1.0
        b1 = 0.0
        b2 = 0.0
        a1 = 0.0
        a2 = 0.0
    }

    fun processSample(channel: Int, x: Double): Double {
        val ch = channel.coerceIn(0, z1.lastIndex)
        val y = b0 * x + z1[ch]
        z1[ch] = b1 * x - a1 * y + z2[ch]
        z2[ch] = b2 * x - a2 * y
        return y
    }

    /**
     * Exact transfer function magnitude |H(e^jw)| in dB at [freqHz] for plotting the real-time graph.
     */
    fun magnitudeDbAt(freqHz: Double, sampleRateHz: Int): Double {
        if (sampleRateHz <= 0) return 0.0
        val w = 2.0 * PI * freqHz.coerceIn(10.0, sampleRateHz * 0.495) / sampleRateHz.toDouble()
        val cosW = cos(w)
        val sinW = sin(w)
        val cos2W = cos(2.0 * w)
        val sin2W = sin(2.0 * w)

        val numRe = b0 + b1 * cosW + b2 * cos2W
        val numIm = -(b1 * sinW + b2 * sin2W)
        val denRe = 1.0 + a1 * cosW + a2 * cos2W
        val denIm = -(a1 * sinW + a2 * sin2W)

        val numMag = hypot(numRe, numIm)
        val denMag = hypot(denRe, denIm).coerceAtLeast(1e-12)
        val mag = (numMag / denMag).coerceAtLeast(1e-9)
        return 20.0 * log10(mag)
    }

    companion object {
        fun computeCombinedCurveDb(
            bands: List<EqBand>,
            preampDb: Float,
            sampleRateHz: Int,
            frequenciesHz: List<Double>
        ): List<Double> {
            val sr = if (sampleRateHz > 0) sampleRateHz else 48000
            val activeFilters = bands.filter { it.enabled }.map { band ->
                BiquadFilter().apply {
                    configure(band.filterType, sr, band.frequencyHz, band.gainDb, band.qFactor)
                }
            }
            return frequenciesHz.map { freq ->
                var totalDb = preampDb.toDouble()
                for (filter in activeFilters) {
                    totalDb += filter.magnitudeDbAt(freq, sr)
                }
                totalDb.coerceIn(-24.0, 24.0)
            }
        }
    }
}
