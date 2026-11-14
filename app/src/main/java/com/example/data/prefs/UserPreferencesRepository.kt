package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.audio.dsp.EqPresets
import com.example.data.model.AccentColorOption
import com.example.data.model.AppThemeStyle
import com.example.data.model.ArtworkStyle
import com.example.data.model.BufferPreset
import com.example.data.model.DspSettings
import com.example.data.model.EqBand
import com.example.data.model.EqUiMode
import com.example.data.model.LibrarySortOrder
import com.example.data.model.OutputProfileType
import com.example.data.model.OutputRouteCategory
import com.example.data.model.PlayerLayoutMode
import com.example.data.model.ReplayGainMode
import com.example.data.model.SimpleEqPreset
import com.example.data.model.SoundProfileId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sonicflow_prefs")

data class DeviceEqSnapshot(
    val routeCategory: OutputRouteCategory,
    val eqUiMode: EqUiMode,
    val simplePreset: SimpleEqPreset,
    val simpleIntensity: Float,
    val bitTransparentMode: Boolean,
    val eqEnabled: Boolean,
    val activePresetId: Long,
    val preampDb: Float,
    val bands: List<EqBand>
)

data class AppPreferencesState(
    val hasCompletedOnboarding: Boolean = false,
    val dspSettings: DspSettings = DspSettings(bands = EqPresets.flatBands()),
    val eqUiMode: EqUiMode = EqUiMode.SIMPLE,
    val simpleEqPreset: SimpleEqPreset = SimpleEqPreset.NATURAL,
    val simpleEqIntensity: Float = 1.0f,
    val rememberEqPerDevice: Boolean = true,
    val deviceEqProfilesJson: String = "{}",
    val gaplessPlayback: Boolean = true,
    val fadeInOutEnabled: Boolean = true,
    val crossfadeSeconds: Int = 0,
    val playbackSpeed: Float = 1.0f,
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
    val savedQueueUrisJson: String = "[]",
    val savedQueueIndex: Int = 0,
    val sleepTimerEndEpochMs: Long = 0L,
    val themeStyle: AppThemeStyle = AppThemeStyle.AMOLED_BLACK,
    val accentColor: AccentColorOption = AccentColorOption.ELECTRIC_CYAN,
    val artworkStyle: ArtworkStyle = ArtworkStyle.ROUNDED_SQUARE,
    val playerLayout: PlayerLayoutMode = PlayerLayoutMode.STANDARD,
    val librarySortOrder: LibrarySortOrder = LibrarySortOrder.TITLE_ASC,
    val gesturesEnabled: Boolean = true,
    val autoPlayOnSelect: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
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

        val EQ_UI_MODE = stringPreferencesKey("eq_ui_mode")
        val SIMPLE_EQ_PRESET = stringPreferencesKey("simple_eq_preset")
        val SIMPLE_EQ_INTENSITY = floatPreferencesKey("simple_eq_intensity")
        val REMEMBER_EQ_PER_DEVICE = booleanPreferencesKey("remember_eq_per_device")
        val DEVICE_EQ_PROFILES_JSON = stringPreferencesKey("device_eq_profiles_json")

        val GAPLESS = booleanPreferencesKey("gapless_playback")
        val FADE_IN_OUT = booleanPreferencesKey("fade_in_out")
        val CROSSFADE_SEC = intPreferencesKey("crossfade_sec")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
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
        val SAVED_QUEUE_URIS_JSON = stringPreferencesKey("saved_queue_uris_json")
        val SAVED_QUEUE_INDEX = intPreferencesKey("saved_queue_index")
        val SLEEP_TIMER_END = longPreferencesKey("sleep_timer_end_epoch")

        val THEME_STYLE = stringPreferencesKey("theme_style")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val ARTWORK_STYLE = stringPreferencesKey("artwork_style")
        val PLAYER_LAYOUT = stringPreferencesKey("player_layout")
        val LIBRARY_SORT = stringPreferencesKey("library_sort")
        val GESTURES_ENABLED = booleanPreferencesKey("gestures_enabled")
        val AUTO_PLAY_ON_SELECT = booleanPreferencesKey("auto_play_on_select")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.dataStore.data.map { prefs ->
        val bandsJson = prefs[Keys.BANDS_JSON]
        val bands = if (bandsJson.isNullOrBlank()) EqPresets.flatBands() else EqPresets.bandsFromJson(bandsJson)

        AppPreferencesState(
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_DONE] ?: false,
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
                replayGainMode = enumOrDefault(prefs[Keys.REPLAYGAIN_MODE], ReplayGainMode.OFF),
                replayGainPreampDb = prefs[Keys.REPLAYGAIN_PREAMP] ?: 0f,
                digitalGainDb = prefs[Keys.DIGITAL_GAIN] ?: 0f,
                limiterEnabled = prefs[Keys.LIMITER_ENABLED] ?: true,
                limiterThresholdDb = prefs[Keys.LIMITER_THRESHOLD] ?: -0.5f,
                stereoBalance = prefs[Keys.STEREO_BALANCE] ?: 0f,
                monoDownmix = prefs[Keys.MONO_DOWNMIX] ?: false
            ),
            eqUiMode = enumOrDefault(prefs[Keys.EQ_UI_MODE], EqUiMode.SIMPLE),
            simpleEqPreset = enumOrDefault(prefs[Keys.SIMPLE_EQ_PRESET], SimpleEqPreset.NATURAL),
            simpleEqIntensity = (prefs[Keys.SIMPLE_EQ_INTENSITY] ?: 1.0f).coerceIn(0f, 1.5f),
            rememberEqPerDevice = prefs[Keys.REMEMBER_EQ_PER_DEVICE] ?: true,
            deviceEqProfilesJson = prefs[Keys.DEVICE_EQ_PROFILES_JSON] ?: "{}",
            gaplessPlayback = prefs[Keys.GAPLESS] ?: true,
            fadeInOutEnabled = prefs[Keys.FADE_IN_OUT] ?: true,
            crossfadeSeconds = (prefs[Keys.CROSSFADE_SEC] ?: 0).coerceIn(0, 8),
            playbackSpeed = (prefs[Keys.PLAYBACK_SPEED] ?: 1.0f).coerceIn(0.75f, 1.5f),
            audioFocusEnabled = prefs[Keys.AUDIO_FOCUS] ?: true,
            resumePlaybackOnRestart = prefs[Keys.RESUME_ON_RESTART] ?: true,
            visualizerEnabled = prefs[Keys.VISUALIZER_ENABLED] ?: true,
            albumArtworkEnabled = prefs[Keys.ARTWORK_ENABLED] ?: true,
            playbackNotificationEnabled = prefs[Keys.NOTIFICATION_ENABLED] ?: true,
            bufferPreset = enumOrDefault(prefs[Keys.BUFFER_PRESET], BufferPreset.BALANCED),
            outputProfile = enumOrDefault(prefs[Keys.OUTPUT_PROFILE], OutputProfileType.AUTO),
            soundProfile = enumOrDefault(prefs[Keys.SOUND_PROFILE], SoundProfileId.NATURAL),
            preferDirectBitPerfectRate = prefs[Keys.PREFER_DIRECT_RATE] ?: true,
            lastPlayedUri = prefs[Keys.LAST_TRACK_URI],
            lastPlayedPositionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
            savedQueueUrisJson = prefs[Keys.SAVED_QUEUE_URIS_JSON] ?: "[]",
            savedQueueIndex = prefs[Keys.SAVED_QUEUE_INDEX] ?: 0,
            sleepTimerEndEpochMs = prefs[Keys.SLEEP_TIMER_END] ?: 0L,
            themeStyle = enumOrDefault(prefs[Keys.THEME_STYLE], AppThemeStyle.AMOLED_BLACK),
            accentColor = enumOrDefault(prefs[Keys.ACCENT_COLOR], AccentColorOption.ELECTRIC_CYAN),
            artworkStyle = enumOrDefault(prefs[Keys.ARTWORK_STYLE], ArtworkStyle.ROUNDED_SQUARE),
            playerLayout = enumOrDefault(prefs[Keys.PLAYER_LAYOUT], PlayerLayoutMode.STANDARD),
            librarySortOrder = enumOrDefault(prefs[Keys.LIBRARY_SORT], LibrarySortOrder.TITLE_ASC),
            gesturesEnabled = prefs[Keys.GESTURES_ENABLED] ?: true,
            autoPlayOnSelect = prefs[Keys.AUTO_PLAY_ON_SELECT] ?: true
        )
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
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
                replayGainMode = enumOrDefault(prefs[Keys.REPLAYGAIN_MODE], ReplayGainMode.OFF),
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

    suspend fun setEqUiMode(mode: EqUiMode) {
        context.dataStore.edit { it[Keys.EQ_UI_MODE] = mode.name }
    }

    suspend fun applySimpleEq(preset: SimpleEqPreset, intensity: Float) {
        val clamped = intensity.coerceIn(0f, 1.5f)
        val scaledGains = preset.baseGains.map { (it * clamped).coerceIn(-12f, 12f) }
        val bands = EqPresets.flatBands(scaledGains)
        val preamp = (preset.basePreampDb * clamped).coerceIn(-15f, 6f)
        context.dataStore.edit { prefs ->
            prefs[Keys.SIMPLE_EQ_PRESET] = preset.name
            prefs[Keys.SIMPLE_EQ_INTENSITY] = clamped
            prefs[Keys.BIT_TRANSPARENT] = false
            prefs[Keys.DSP_MASTER] = true
            prefs[Keys.EQ_ENABLED] = true
            prefs[Keys.EQ_BYPASS_AB] = false
            prefs[Keys.PREAMP_DB] = preamp
            prefs[Keys.BANDS_JSON] = EqPresets.bandsToJson(bands)
            prefs[Keys.ACTIVE_PRESET_ID] = when (preset) {
                SimpleEqPreset.NATURAL -> 2L
                SimpleEqPreset.VOCAL_CLARITY -> 3L
                SimpleEqPreset.WARM -> 5L
                SimpleEqPreset.BRIGHT -> 6L
                SimpleEqPreset.BALANCED -> 8L
                SimpleEqPreset.BASS_REDUCED -> 7L
                SimpleEqPreset.CUSTOM -> 9L
            }
        }
    }

    suspend fun markSimpleEqCustom() {
        context.dataStore.edit {
            it[Keys.SIMPLE_EQ_PRESET] = SimpleEqPreset.CUSTOM.name
        }
    }

    suspend fun setRememberEqPerDevice(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REMEMBER_EQ_PER_DEVICE] = enabled }
    }

    suspend fun saveDeviceEqSnapshot(
        routeCategory: OutputRouteCategory,
        dsp: DspSettings,
        uiMode: EqUiMode,
        simplePreset: SimpleEqPreset,
        intensity: Float
    ) {
        context.dataStore.edit { prefs ->
            val root = runCatching {
                JSONObject(prefs[Keys.DEVICE_EQ_PROFILES_JSON] ?: "{}")
            }.getOrElse { JSONObject() }
            val entry = JSONObject().apply {
                put("uiMode", uiMode.name)
                put("simplePreset", simplePreset.name)
                put("intensity", intensity.toDouble())
                put("bitTransparent", dsp.bitTransparentMode)
                put("eqEnabled", dsp.eqEnabled)
                put("activePresetId", dsp.activePresetId)
                put("preampDb", dsp.preampDb.toDouble())
                put("bandsJson", EqPresets.bandsToJson(dsp.bands))
            }
            root.put(routeCategory.name, entry)
            prefs[Keys.DEVICE_EQ_PROFILES_JSON] = root.toString()
        }
    }

    suspend fun getDeviceEqSnapshot(routeCategory: OutputRouteCategory): DeviceEqSnapshot? {
        val raw = preferencesFlow.first().deviceEqProfilesJson
        return parseDeviceEqSnapshot(raw, routeCategory)
    }

    fun parseDeviceEqSnapshot(rawJson: String, routeCategory: OutputRouteCategory): DeviceEqSnapshot? {
        return runCatching {
            val root = JSONObject(rawJson)
            val entry = root.optJSONObject(routeCategory.name) ?: return null
            DeviceEqSnapshot(
                routeCategory = routeCategory,
                eqUiMode = enumOrDefault(entry.optString("uiMode"), EqUiMode.SIMPLE),
                simplePreset = enumOrDefault(entry.optString("simplePreset"), SimpleEqPreset.NATURAL),
                simpleIntensity = entry.optDouble("intensity", 1.0).toFloat().coerceIn(0f, 1.5f),
                bitTransparentMode = entry.optBoolean("bitTransparent", false),
                eqEnabled = entry.optBoolean("eqEnabled", true),
                activePresetId = entry.optLong("activePresetId", 1L),
                preampDb = entry.optDouble("preampDb", 0.0).toFloat(),
                bands = EqPresets.bandsFromJson(entry.optString("bandsJson", "[]"))
            )
        }.getOrNull()
    }

    suspend fun applyDeviceEqSnapshot(snapshot: DeviceEqSnapshot) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EQ_UI_MODE] = snapshot.eqUiMode.name
            prefs[Keys.SIMPLE_EQ_PRESET] = snapshot.simplePreset.name
            prefs[Keys.SIMPLE_EQ_INTENSITY] = snapshot.simpleIntensity
            prefs[Keys.BIT_TRANSPARENT] = snapshot.bitTransparentMode
            prefs[Keys.DSP_MASTER] = !snapshot.bitTransparentMode
            prefs[Keys.EQ_ENABLED] = snapshot.eqEnabled
            prefs[Keys.ACTIVE_PRESET_ID] = snapshot.activePresetId
            prefs[Keys.PREAMP_DB] = snapshot.preampDb
            prefs[Keys.BANDS_JSON] = EqPresets.bandsToJson(snapshot.bands)
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

    suspend fun restoreFullDsp(dsp: DspSettings) {
        updateDsp { dsp }
    }

    suspend fun setOutputProfile(profile: OutputProfileType) {
        context.dataStore.edit { it[Keys.OUTPUT_PROFILE] = profile.name }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GAPLESS] = enabled }
    }

    suspend fun setFadeInOutEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FADE_IN_OUT] = enabled }
    }

    suspend fun setCrossfadeSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.CROSSFADE_SEC] = seconds.coerceIn(0, 8) }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.PLAYBACK_SPEED] = speed.coerceIn(0.75f, 1.5f) }
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

    suspend fun saveQueueState(uris: List<String>, index: Int) {
        val arr = JSONArray()
        uris.forEach { arr.put(it) }
        context.dataStore.edit { prefs ->
            prefs[Keys.SAVED_QUEUE_URIS_JSON] = arr.toString()
            prefs[Keys.SAVED_QUEUE_INDEX] = index.coerceAtLeast(0)
        }
    }

    fun parseSavedQueueUris(json: String): List<String> {
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i -> arr.optString(i).takeIf { it.isNotBlank() } }
        }.getOrElse { emptyList() }
    }

    suspend fun setSleepTimerEnd(epochMs: Long) {
        context.dataStore.edit { it[Keys.SLEEP_TIMER_END] = epochMs }
    }

    suspend fun setThemeStyle(style: AppThemeStyle) {
        context.dataStore.edit { it[Keys.THEME_STYLE] = style.name }
    }

    suspend fun setAccentColor(option: AccentColorOption) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = option.name }
    }

    suspend fun setArtworkStyle(style: ArtworkStyle) {
        context.dataStore.edit { it[Keys.ARTWORK_STYLE] = style.name }
    }

    suspend fun setPlayerLayout(layout: PlayerLayoutMode) {
        context.dataStore.edit { it[Keys.PLAYER_LAYOUT] = layout.name }
    }

    suspend fun setLibrarySortOrder(order: LibrarySortOrder) {
        context.dataStore.edit { it[Keys.LIBRARY_SORT] = order.name }
    }

    suspend fun setGesturesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GESTURES_ENABLED] = enabled }
    }

    suspend fun setAutoPlayOnSelect(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_PLAY_ON_SELECT] = enabled }
    }

    suspend fun resetAllToDefaults() {
        context.dataStore.edit { prefs ->
            val onboarding = prefs[Keys.ONBOARDING_DONE] ?: true
            val lastUri = prefs[Keys.LAST_TRACK_URI]
            val lastPos = prefs[Keys.LAST_POSITION_MS] ?: 0L
            val queueJson = prefs[Keys.SAVED_QUEUE_URIS_JSON] ?: "[]"
            val queueIdx = prefs[Keys.SAVED_QUEUE_INDEX] ?: 0
            prefs.clear()
            prefs[Keys.ONBOARDING_DONE] = onboarding
            if (lastUri != null) prefs[Keys.LAST_TRACK_URI] = lastUri
            prefs[Keys.LAST_POSITION_MS] = lastPos
            prefs[Keys.SAVED_QUEUE_URIS_JSON] = queueJson
            prefs[Keys.SAVED_QUEUE_INDEX] = queueIdx
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }
}
