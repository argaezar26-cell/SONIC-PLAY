package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.audio.dsp.EqPresets
import com.example.data.model.BufferPreset
import com.example.data.model.DspSettings
import com.example.data.model.EqBand
import com.example.data.model.OutputProfileType
import com.example.data.model.ReplayGainMode
import com.example.data.model.SoundProfileId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sonicflow_prefs")

data class AppPreferencesState(
    val dspSettings: DspSettings = DspSettings(bands = EqPresets.flatBands()),
    val gaplessPlayback: Boolean = true,
    val audioFocusEnabled: Boolean = true,
    val resumePlaybackOnRestart: Boolean = true,
    val visualizerEnabled: Boolean = true,
    val albumArtworkEnabled: Boolean = true,
    val playbackNotificationEnabled: Boolean = true,
    val bufferPreset: BufferPreset = BufferPreset.BALANCED,
    val outputProfile: OutputProfileType = OutputProfileType.AUTO,
    val soundProfile: SoundProfileId = SoundProfileId.NATURAL,
    val preferDirectBitPerfectRate: Boolean = true,
    val lastPlayedUri: String? = null,
    val lastPlayedPositionMs: Long = 0L,
    val sleepTimerEndEpochMs: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val BIT_TRANSPARENT = booleanPreferencesKey("bit_transparent_mode")
        val DSP_MASTER = booleanPreferencesKey("dsp_master_enabled")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_BYPASS_AB = booleanPreferencesKey("eq_bypass_ab")
        val ACTIVE_PRESET_ID = longPreferencesKey("active_preset_id")
        val PREAMP_DB = floatPreferencesKey("preamp_db")
        val AUTO_HEADROOM = booleanPreferencesKey("auto_headroom")
        val BANDS_JSON = stringPreferencesKey("eq_bands_json")
        val CROSSFEED_ENABLED = booleanPreferencesKey("crossfeed_enabled")
        val CROSSFEED_INTENSITY = floatPreferencesKey("crossfeed_intensity")
        val REPLAYGAIN_MODE = stringPreferencesKey("replaygain_mode")
        val REPLAYGAIN_PREAMP = floatPreferencesKey("replaygain_preamp")
        val DIGITAL_GAIN = floatPreferencesKey("digital_gain_db")
        val LIMITER_ENABLED = booleanPreferencesKey("limiter_enabled")
        val LIMITER_THRESHOLD = floatPreferencesKey("limiter_threshold_db")
        val STEREO_BALANCE = floatPreferencesKey("stereo_balance")
        val MONO_DOWNMIX = booleanPreferencesKey("mono_downmix")

        val GAPLESS = booleanPreferencesKey("gapless_playback")
        val AUDIO_FOCUS = booleanPreferencesKey("audio_focus")
        val RESUME_ON_RESTART = booleanPreferencesKey("resume_on_restart")
        val VISUALIZER_ENABLED = booleanPreferencesKey("visualizer_enabled")
        val ARTWORK_ENABLED = booleanPreferencesKey("artwork_enabled")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val BUFFER_PRESET = stringPreferencesKey("buffer_preset")
        val OUTPUT_PROFILE = stringPreferencesKey("output_profile")
        val SOUND_PROFILE = stringPreferencesKey("sound_profile")
        val PREFER_DIRECT_RATE = booleanPreferencesKey("prefer_direct_rate")
        val LAST_TRACK_URI = stringPreferencesKey("last_track_uri")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val SLEEP_TIMER_END = longPreferencesKey("sleep_timer_end_epoch")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.dataStore.data.map { prefs ->
        val bandsJson = prefs[Keys.BANDS_JSON]
        val bands = if (bandsJson.isNullOrBlank()) EqPresets.flatBands() else EqPresets.bandsFromJson(bandsJson)

        AppPreferencesState(
            dspSettings = DspSettings(
                bitTransparentMode = prefs[Keys.BIT_TRANSPARENT] ?: false,
                dspMasterEnabled = prefs[Keys.DSP_MASTER] ?: true,
                eqEnabled = prefs[Keys.EQ_ENABLED] ?: true,
                eqBypassBeforeAfter = prefs[Keys.EQ_BYPASS_AB] ?: false,
                activePresetId = prefs[Keys.ACTIVE_PRESET_ID] ?: 1L,
                preampDb = prefs[Keys.PREAMP_DB] ?: 0f,
                autoHeadroomEnabled = prefs[Keys.AUTO_HEADROOM] ?: true,
                bands = bands,
                crossfeedEnabled = prefs[Keys.CROSSFEED_ENABLED] ?: false,
                crossfeedIntensity = prefs[Keys.CROSSFEED_INTENSITY] ?: 0.35f,
                replayGainMode = runCatching {
                    ReplayGainMode.valueOf(prefs[Keys.REPLAYGAIN_MODE] ?: ReplayGainMode.OFF.name)
                }.getOrDefault(ReplayGainMode.OFF),
                replayGainPreampDb = prefs[Keys.REPLAYGAIN_PREAMP] ?: 0f,
                digitalGainDb = prefs[Keys.DIGITAL_GAIN] ?: 0f,
                limiterEnabled = prefs[Keys.LIMITER_ENABLED] ?: true,
                limiterThresholdDb = prefs[Keys.LIMITER_THRESHOLD] ?: -0.5f,
                stereoBalance = prefs[Keys.STEREO_BALANCE] ?: 0f,
                monoDownmix = prefs[Keys.MONO_DOWNMIX] ?: false
            ),
            gaplessPlayback = prefs[Keys.GAPLESS] ?: true,
            audioFocusEnabled = prefs[Keys.AUDIO_FOCUS] ?: true,
            resumePlaybackOnRestart = prefs[Keys.RESUME_ON_RESTART] ?: true,
            visualizerEnabled = prefs[Keys.VISUALIZER_ENABLED] ?: true,
            albumArtworkEnabled = prefs[Keys.ARTWORK_ENABLED] ?: true,
            playbackNotificationEnabled = prefs[Keys.NOTIFICATION_ENABLED] ?: true,
            bufferPreset = runCatching {
                BufferPreset.valueOf(prefs[Keys.BUFFER_PRESET] ?: BufferPreset.BALANCED.name)
            }.getOrDefault(BufferPreset.BALANCED),
            outputProfile = runCatching {
                OutputProfileType.valueOf(prefs[Keys.OUTPUT_PROFILE] ?: OutputProfileType.AUTO.name)
            }.getOrDefault(OutputProfileType.AUTO),
            soundProfile = runCatching {
                SoundProfileId.valueOf(prefs[Keys.SOUND_PROFILE] ?: SoundProfileId.NATURAL.name)
            }.getOrDefault(SoundProfileId.NATURAL),
            preferDirectBitPerfectRate = prefs[Keys.PREFER_DIRECT_RATE] ?: true,
            lastPlayedUri = prefs[Keys.LAST_TRACK_URI],
            lastPlayedPositionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
            sleepTimerEndEpochMs = prefs[Keys.SLEEP_TIMER_END] ?: 0L
        )
    }

    suspend fun updateDsp(transform: (DspSettings) -> DspSettings) {
        context.dataStore.edit { prefs ->
            val currentBands = prefs[Keys.BANDS_JSON]?.let { EqPresets.bandsFromJson(it) } ?: EqPresets.flatBands()
            val current = DspSettings(
                bitTransparentMode = prefs[Keys.BIT_TRANSPARENT] ?: false,
                dspMasterEnabled = prefs[Keys.DSP_MASTER] ?: true,
                eqEnabled = prefs[Keys.EQ_ENABLED] ?: true,
                eqBypassBeforeAfter = prefs[Keys.EQ_BYPASS_AB] ?: false,
                activePresetId = prefs[Keys.ACTIVE_PRESET_ID] ?: 1L,
                preampDb = prefs[Keys.PREAMP_DB] ?: 0f,
                autoHeadroomEnabled = prefs[Keys.AUTO_HEADROOM] ?: true,
                bands = currentBands,
                crossfeedEnabled = prefs[Keys.CROSSFEED_ENABLED] ?: false,
                crossfeedIntensity = prefs[Keys.CROSSFEED_INTENSITY] ?: 0.35f,
                replayGainMode = runCatching {
                    ReplayGainMode.valueOf(prefs[Keys.REPLAYGAIN_MODE] ?: ReplayGainMode.OFF.name)
                }.getOrDefault(ReplayGainMode.OFF),
                replayGainPreampDb = prefs[Keys.REPLAYGAIN_PREAMP] ?: 0f,
                digitalGainDb = prefs[Keys.DIGITAL_GAIN] ?: 0f,
                limiterEnabled = prefs[Keys.LIMITER_ENABLED] ?: true,
                limiterThresholdDb = prefs[Keys.LIMITER_THRESHOLD] ?: -0.5f,
                stereoBalance = prefs[Keys.STEREO_BALANCE] ?: 0f,
                monoDownmix = prefs[Keys.MONO_DOWNMIX] ?: false
            )
            val next = transform(current)
            prefs[Keys.BIT_TRANSPARENT] = next.bitTransparentMode
            prefs[Keys.DSP_MASTER] = next.dspMasterEnabled
            prefs[Keys.EQ_ENABLED] = next.eqEnabled
            prefs[Keys.EQ_BYPASS_AB] = next.eqBypassBeforeAfter
            prefs[Keys.ACTIVE_PRESET_ID] = next.activePresetId
            prefs[Keys.PREAMP_DB] = next.preampDb
            prefs[Keys.AUTO_HEADROOM] = next.autoHeadroomEnabled
            prefs[Keys.BANDS_JSON] = EqPresets.bandsToJson(next.bands)
            prefs[Keys.CROSSFEED_ENABLED] = next.crossfeedEnabled
            prefs[Keys.CROSSFEED_INTENSITY] = next.crossfeedIntensity
            prefs[Keys.REPLAYGAIN_MODE] = next.replayGainMode.name
            prefs[Keys.REPLAYGAIN_PREAMP] = next.replayGainPreampDb
            prefs[Keys.DIGITAL_GAIN] = next.digitalGainDb
            prefs[Keys.LIMITER_ENABLED] = next.limiterEnabled
            prefs[Keys.LIMITER_THRESHOLD] = next.limiterThresholdDb
            prefs[Keys.STEREO_BALANCE] = next.stereoBalance
            prefs[Keys.MONO_DOWNMIX] = next.monoDownmix
        }
    }

    suspend fun applySoundProfile(profile: SoundProfileId) {
        val preset = EqPresets.presetForSoundProfile(profile)
        context.dataStore.edit { prefs ->
            prefs[Keys.SOUND_PROFILE] = profile.name
            if (profile == SoundProfileId.PURE) {
                prefs[Keys.BIT_TRANSPARENT] = true
                prefs[Keys.DSP_MASTER] = false
                prefs[Keys.EQ_ENABLED] = false
                prefs[Keys.ACTIVE_PRESET_ID] = preset.id
                prefs[Keys.PREAMP_DB] = 0f
                prefs[Keys.BANDS_JSON] = EqPresets.bandsToJson(preset.bands)
            } else {
                prefs[Keys.BIT_TRANSPARENT] = false
                prefs[Keys.DSP_MASTER] = true
                prefs[Keys.EQ_ENABLED] = true
                prefs[Keys.EQ_BYPASS_AB] = false
                prefs[Keys.ACTIVE_PRESET_ID] = preset.id
                prefs[Keys.PREAMP_DB] = preset.preampDb
                prefs[Keys.BANDS_JSON] = EqPresets.bandsToJson(preset.bands)
            }
        }
    }

    suspend fun applyBandsAndPreset(presetId: Long, preampDb: Float, bands: List<EqBand>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PRESET_ID] = presetId
            prefs[Keys.PREAMP_DB] = preampDb
            prefs[Keys.BANDS_JSON] = EqPresets.bandsToJson(bands)
        }
    }

    suspend fun setOutputProfile(profile: OutputProfileType) {
        context.dataStore.edit { it[Keys.OUTPUT_PROFILE] = profile.name }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GAPLESS] = enabled }
    }

    suspend fun setAudioFocusEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUDIO_FOCUS] = enabled }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.RESUME_ON_RESTART] = enabled }
    }

    suspend fun setVisualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VISUALIZER_ENABLED] = enabled }
    }

    suspend fun setArtworkEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ARTWORK_ENABLED] = enabled }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_ENABLED] = enabled }
    }

    suspend fun setBufferPreset(preset: BufferPreset) {
        context.dataStore.edit { it[Keys.BUFFER_PRESET] = preset.name }
    }

    suspend fun setPreferDirectRate(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PREFER_DIRECT_RATE] = enabled }
    }

    suspend fun saveLastPlayback(uri: String?, positionMs: Long) {
        context.dataStore.edit { prefs ->
            if (uri != null) prefs[Keys.LAST_TRACK_URI] = uri
            prefs[Keys.LAST_POSITION_MS] = positionMs
        }
    }

    suspend fun setSleepTimerEnd(epochMs: Long) {
        context.dataStore.edit { it[Keys.SLEEP_TIMER_END] = epochMs }
    }
}
