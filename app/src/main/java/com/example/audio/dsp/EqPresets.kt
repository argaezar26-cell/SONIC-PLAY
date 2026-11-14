package com.example.audio.dsp

import com.example.data.model.EqBand
import com.example.data.model.EqPreset
import com.example.data.model.FilterType
import com.example.data.model.SoundProfileId
import org.json.JSONArray
import org.json.JSONObject

object EqPresets {
    val defaultFrequencies = listOf(
        31f, 62f, 125f, 250f, 500f, 1000f,
        2000f, 4000f, 6300f, 8000f, 12500f, 16000f
    )

    fun flatBands(gains: List<Float> = List(12) { 0f }): List<EqBand> {
        return defaultFrequencies.mapIndexed { idx, freq ->
            val type = when (idx) {
                0 -> FilterType.LOW_SHELF
                11 -> FilterType.HIGH_SHELF
                else -> FilterType.PEAKING
            }
            EqBand(
                index = idx,
                frequencyHz = freq,
                gainDb = gains.getOrElse(idx) { 0f },
                qFactor = if (type == FilterType.PEAKING) 1.41f else 0.707f,
                filterType = type,
                enabled = true
            )
        }
    }

    val builtInPresets: List<EqPreset> = listOf(
        EqPreset(
            id = 1L,
            name = "Pure Neutral",
            isBuiltIn = true,
            preampDb = 0f,
            bands = flatBands(listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
        ),
        EqPreset(
            id = 2L,
            name = "Natural Vocal",
            isBuiltIn = true,
            preampDb = -2.0f,
            bands = flatBands(listOf(-0.5f, -0.5f, 0f, -0.5f, 0.8f, 1.8f, 2.2f, 1.5f, 0.5f, 0f, 0.5f, 0.5f))
        ),
        EqPreset(
            id = 3L,
            name = "Vocal Clarity",
            isBuiltIn = true,
            preampDb = -2.5f,
            bands = flatBands(listOf(-1.5f, -1.0f, -0.5f, -0.8f, 0.5f, 2.0f, 2.8f, 2.2f, 1.0f, 0.5f, 1.0f, 0.8f))
        ),
        EqPreset(
            id = 4L,
            name = "Instrument Separation",
            isBuiltIn = true,
            preampDb = -2.5f,
            bands = flatBands(listOf(0.5f, 0f, -1.0f, -1.2f, 0.5f, 1.2f, 1.8f, 2.4f, 1.8f, 1.5f, 2.2f, 1.8f))
        ),
        EqPreset(
            id = 5L,
            name = "Warm Natural",
            isBuiltIn = true,
            preampDb = -2.0f,
            bands = flatBands(listOf(1.5f, 2.0f, 1.5f, 0.8f, 0.2f, 0f, -0.4f, -0.8f, -1.0f, -0.5f, 0f, 0.5f))
        ),
        EqPreset(
            id = 6L,
            name = "Detail Focus",
            isBuiltIn = true,
            preampDb = -2.8f,
            bands = flatBands(listOf(0f, -0.5f, -0.8f, -0.5f, 0f, 0.8f, 1.5f, 2.5f, 2.8f, 2.2f, 2.6f, 2.0f))
        ),
        EqPreset(
            id = 7L,
            name = "Bass Controlled",
            isBuiltIn = true,
            preampDb = -2.5f,
            bands = flatBands(listOf(2.4f, 1.8f, 0.5f, -1.2f, -0.5f, 0.2f, 0.8f, 1.0f, 0.8f, 0.5f, 0.8f, 0.5f))
        ),
        EqPreset(
            id = 8L,
            name = "Studio Reference",
            isBuiltIn = true,
            preampDb = -1.0f,
            bands = flatBands(listOf(0.5f, 0.2f, 0f, -0.4f, 0f, 0.3f, 0.6f, 0.5f, 0.2f, 0f, 0.4f, 0.6f))
        ),
        EqPreset(
            id = 9L,
            name = "Custom",
            isBuiltIn = true,
            preampDb = 0f,
            bands = flatBands()
        )
    )

    fun presetForSoundProfile(profile: SoundProfileId): EqPreset {
        return when (profile) {
            SoundProfileId.PURE -> builtInPresets[0]
            SoundProfileId.NATURAL -> builtInPresets[1]
            SoundProfileId.VOCAL -> builtInPresets[2]
            SoundProfileId.CLARITY -> builtInPresets[3]
            SoundProfileId.WARM -> builtInPresets[4]
            SoundProfileId.REFERENCE -> builtInPresets[7]
        }
    }

    fun bandsToJson(bands: List<EqBand>): String {
        val arr = JSONArray()
        for (band in bands) {
            arr.put(
                JSONObject().apply {
                    put("index", band.index)
                    put("frequencyHz", band.frequencyHz.toDouble())
                    put("gainDb", band.gainDb.toDouble())
                    put("qFactor", band.qFactor.toDouble())
                    put("filterType", band.filterType.name)
                    put("enabled", band.enabled)
                }
            )
        }
        return arr.toString()
    }

    fun bandsFromJson(json: String): List<EqBand> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                EqBand(
                    index = obj.optInt("index", i),
                    frequencyHz = obj.optDouble("frequencyHz", 1000.0).toFloat(),
                    gainDb = obj.optDouble("gainDb", 0.0).toFloat(),
                    qFactor = obj.optDouble("qFactor", 1.41).toFloat(),
                    filterType = runCatching {
                        FilterType.valueOf(obj.optString("filterType", "PEAKING"))
                    }.getOrDefault(FilterType.PEAKING),
                    enabled = obj.optBoolean("enabled", true)
                )
            }
        } catch (_: Exception) {
            flatBands()
        }
    }

    fun exportPresetJson(preset: EqPreset): String {
        return JSONObject().apply {
            put("app", "SONICFLOW")
            put("version", 1)
            put("name", preset.name)
            put("preampDb", preset.preampDb.toDouble())
            put("bands", JSONArray(bandsToJson(preset.bands)))
        }.toString(2)
    }

    fun importPresetJson(rawJson: String): EqPreset? {
        return try {
            val obj = JSONObject(rawJson)
            val name = obj.optString("name", "Imported Preset").ifBlank { "Imported Preset" }
            val preamp = obj.optDouble("preampDb", 0.0).toFloat().coerceIn(-15f, 6f)
            val bandsArr = obj.getJSONArray("bands")
            val bands = bandsFromJson(bandsArr.toString())
            if (bands.isEmpty()) null
            else EqPreset(
                id = 0L,
                name = name,
                isBuiltIn = false,
                preampDb = preamp,
                bands = bands
            )
        } catch (_: Exception) {
            null
        }
    }
}
