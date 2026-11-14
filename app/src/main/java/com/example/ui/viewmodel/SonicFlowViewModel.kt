package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.audio.controller.PlaybackUiState
import com.example.audio.dsp.EqPresets
import com.example.audio.routing.AudioRoutingState
import com.example.audio.testlab.ToneLabState
import com.example.audio.testlab.TestToneMode
import com.example.data.db.PlaylistEntity
import com.example.data.db.ScanFolderEntity
import com.example.data.model.BufferPreset
import com.example.data.model.DspSettings
import com.example.data.model.DspTelemetry
import com.example.data.model.EqBand
import com.example.data.model.EqPreset
import com.example.data.model.FilterType
import com.example.data.model.MixerVerificationStatus
import com.example.data.model.OutputProfileType
import com.example.data.model.QualityTestItem
import com.example.data.model.ReplayGainMode
import com.example.data.model.SoundProfileId
import com.example.data.model.Track
import com.example.data.model.VisualizerFrame
import com.example.data.prefs.AppPreferencesState
import com.example.data.repository.ScanProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab(val label: String) {
    TRACKS("Tracks"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists"),
    FAVORITES("Favorites"),
    RECENT("Recent")
}

class SonicFlowViewModel(private val container: AppContainer) : ViewModel() {

    private val libraryRepo = container.libraryRepository
    private val prefsRepo = container.preferencesRepository
    private val playbackController = container.playbackController
    private val deviceMonitor = container.deviceMonitor
    private val audioProcessor = container.audioProcessor
    private val toneEngine = container.toneGeneratorEngine
    private val diagnostics = container.qualityDiagnostics

    init {
        viewModelScope.launch {
            libraryRepo.ensureBuiltInPresetsSeeded()
        }
    }

    val allTracks: StateFlow<List<Track>> = libraryRepo.allTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteTracks: StateFlow<List<Track>> = libraryRepo.favoriteTracks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayed: StateFlow<List<Track>> = libraryRepo.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAdded: StateFlow<List<Track>> = libraryRepo.recentlyAdded
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistEntity>> = libraryRepo.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanFolders: StateFlow<List<ScanFolderEntity>> = libraryRepo.scanFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eqPresets: StateFlow<List<EqPreset>> = libraryRepo.eqPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EqPresets.builtInPresets)

    val preferences: StateFlow<AppPreferencesState> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferencesState())

    val playbackState: StateFlow<PlaybackUiState> = playbackController.uiState
    val routingState: StateFlow<AudioRoutingState> = deviceMonitor.routingState
    val scanProgress: StateFlow<ScanProgress> = libraryRepo.scanProgress
    val dspTelemetry: StateFlow<DspTelemetry> = audioProcessor.telemetry
    val visualizerFrame: StateFlow<VisualizerFrame> = audioProcessor.visualizerFrame
    val toneLabState: StateFlow<ToneLabState> = toneEngine.state

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLibraryTab = MutableStateFlow(LibraryTab.TRACKS)
    val selectedLibraryTab: StateFlow<LibraryTab> = _selectedLibraryTab.asStateFlow()

    private val _mixerCheckResult = MutableStateFlow<MixerVerificationStatus?>(null)
    val mixerCheckResult: StateFlow<MixerVerificationStatus?> = _mixerCheckResult.asStateFlow()

    private val _qualityTests = MutableStateFlow<List<QualityTestItem>>(emptyList())
    val qualityTests: StateFlow<List<QualityTestItem>> = _qualityTests.asStateFlow()

    private val _isRunningTests = MutableStateFlow(false)
    val isRunningTests: StateFlow<Boolean> = _isRunningTests.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val filteredTracks: StateFlow<List<Track>> = combine(allTracks, _searchQuery) { tracks, q ->
        val query = q.trim()
        if (query.isBlank()) tracks
        else tracks.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true) ||
                it.genre.contains(query, ignoreCase = true) ||
                it.codec.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setLibraryTab(tab: LibraryTab) {
        _selectedLibraryTab.value = tab
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // SAF Library Actions
    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            val count = libraryRepo.addAndScanFolder(uri)
            _statusMessage.value = "Scanned $count audio tracks"
        }
    }

    fun rescanFolders() {
        viewModelScope.launch {
            val count = libraryRepo.rescanAllFolders(scanFolders.value)
            _statusMessage.value = "Library refreshed ($count tracks)"
        }
    }

    fun removeFolder(treeUri: String) {
        viewModelScope.launch {
            libraryRepo.removeFolder(treeUri)
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            libraryRepo.toggleFavorite(track.id, track.isFavorite)
        }
    }

    // Playlists
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            libraryRepo.createPlaylist(name)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            libraryRepo.deletePlaylist(playlistId)
        }
    }

    fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            libraryRepo.addTrackToPlaylist(playlistId, trackId)
            _statusMessage.value = "Added to playlist"
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            libraryRepo.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun observePlaylistTracks(playlistId: Long) = libraryRepo.observePlaylistTracks(playlistId)

    // Playback Controls
    fun playTracks(tracks: List<Track>, startIndex: Int) {
        toneEngine.stop()
        playbackController.playTrackList(tracks, startIndex)
    }

    fun playNext(track: Track) = playbackController.playNext(track)
    fun addToQueue(track: Track) {
        playbackController.addToQueue(track)
        _statusMessage.value = "Added \"${track.title}\" to queue"
    }

    fun jumpToQueueIndex(index: Int) = playbackController.jumpToQueueIndex(index)
    fun removeFromQueue(index: Int) = playbackController.removeFromQueue(index)
    fun togglePlayPause() = playbackController.togglePlayPause()
    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)
    fun seekToNext() = playbackController.seekToNext()
    fun seekToPrevious() = playbackController.seekToPrevious()
    fun toggleShuffle() = playbackController.toggleShuffle()
    fun cycleRepeatMode() = playbackController.cycleRepeatMode()
    fun setSleepTimerMinutes(minutes: Int) = playbackController.setSleepTimerMinutes(minutes)
    fun clearPlaybackError() = playbackController.clearError()

    // DSP & EQ Actions
    fun setBitTransparentMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(bitTransparentMode = enabled, dspMasterEnabled = !enabled) }
            if (enabled) {
                val sr = playbackState.value.currentTrack?.sampleRateHz ?: 0
                deviceMonitor.configureBitPerfectMixerIfSupported(sr)
            } else {
                deviceMonitor.clearPreferredMixerAttributes()
            }
        }
    }

    fun setDspMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(dspMasterEnabled = enabled, bitTransparentMode = !enabled) }
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(eqEnabled = enabled) }
        }
    }

    fun toggleEqBypassBeforeAfter() {
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(eqBypassBeforeAfter = !it.eqBypassBeforeAfter) }
        }
    }

    fun setAutoHeadroom(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(autoHeadroomEnabled = enabled) }
        }
    }

    fun setPreampDb(db: Float) {
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(preampDb = db.coerceIn(-15f, 6f)) }
        }
    }

    fun updateEqBand(updated: EqBand) {
        viewModelScope.launch {
            prefsRepo.updateDsp { current ->
                val list = current.bands.map { if (it.index == updated.index) updated else it }
                current.copy(bands = list, activePresetId = 9L)
            }
        }
    }

    fun addEqBand() {
        viewModelScope.launch {
            prefsRepo.updateDsp { current ->
                if (current.bands.size >= 15) return@updateDsp current
                val nextIdx = current.bands.size
                val extraFreqs = listOf(45f, 3150f, 10000f)
                val freq = extraFreqs.getOrElse(nextIdx - 12) { 5000f }
                val nextBands = current.bands + EqBand(
                    index = nextIdx,
                    frequencyHz = freq,
                    gainDb = 0f,
                    qFactor = 1.41f,
                    filterType = FilterType.PEAKING,
                    enabled = true
                )
                current.copy(bands = nextBands, activePresetId = 9L)
            }
        }
    }

    fun removeLastEqBand() {
        viewModelScope.launch {
            prefsRepo.updateDsp { current ->
                if (current.bands.size <= 10) return@updateDsp current
                current.copy(bands = current.bands.dropLast(1), activePresetId = 9L)
            }
        }
    }

    fun selectEqPreset(preset: EqPreset) {
        viewModelScope.launch {
            prefsRepo.applyBandsAndPreset(preset.id, preset.preampDb, preset.bands)
        }
    }

    fun saveCustomPreset(name: String) {
        viewModelScope.launch {
            val current = preferences.value.dspSettings
            val newId = libraryRepo.saveCustomEqPreset(name, current.preampDb, current.bands)
            prefsRepo.applyBandsAndPreset(newId, current.preampDb, current.bands)
            _statusMessage.value = "Saved preset \"$name\""
        }
    }

    fun deleteCustomPreset(preset: EqPreset) {
        viewModelScope.launch {
            libraryRepo.deleteCustomEqPreset(preset.id)
        }
    }

    fun exportCurrentPresetJson(): String {
        val current = preferences.value.dspSettings
        val presetName = eqPresets.value.firstOrNull { it.id == current.activePresetId }?.name ?: "SONICFLOW Custom"
        return EqPresets.exportPresetJson(
            EqPreset(
                id = current.activePresetId,
                name = presetName,
                isBuiltIn = false,
                preampDb = current.preampDb,
                bands = current.bands
            )
        )
    }

    fun importPresetJson(rawJson: String): Boolean {
        val imported = EqPresets.importPresetJson(rawJson) ?: return false
        viewModelScope.launch {
            val id = libraryRepo.saveCustomEqPreset(imported.name, imported.preampDb, imported.bands)
            prefsRepo.applyBandsAndPreset(id, imported.preampDb, imported.bands)
            _statusMessage.value = "Imported \"${imported.name}\""
        }
        return true
    }

    fun setCrossfeed(enabled: Boolean, intensity: Float) {
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(crossfeedEnabled = enabled, crossfeedIntensity = intensity.coerceIn(0f, 1f)) }
        }
    }

    fun setReplayGain(mode: ReplayGainMode, preampDb: Float) {
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(replayGainMode = mode, replayGainPreampDb = preampDb.coerceIn(-12f, 12f)) }
        }
    }

    fun setDigitalGainDb(db: Float) {
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(digitalGainDb = db.coerceIn(-18f, 6f)) }
        }
    }

    fun setLimiter(enabled: Boolean, thresholdDb: Float) {
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(limiterEnabled = enabled, limiterThresholdDb = thresholdDb.coerceIn(-6f, -0.1f)) }
        }
    }

    fun setStereoControls(balance: Float, monoDownmix: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(stereoBalance = balance.coerceIn(-1f, 1f), monoDownmix = monoDownmix) }
        }
    }

    fun resetClipDetector() = audioProcessor.resetClipCounter()

    // Sound Profile Lab & Output Routing
    fun applySoundProfile(profile: SoundProfileId) {
        viewModelScope.launch {
            prefsRepo.applySoundProfile(profile)
            _statusMessage.value = "Sound Profile: ${profile.title}"
        }
    }

    fun setOutputProfile(profile: OutputProfileType) {
        viewModelScope.launch {
            prefsRepo.setOutputProfile(profile)
        }
    }

    fun refreshAudioDevices() = deviceMonitor.refreshDevices()
    fun clearDisconnectBanner() = deviceMonitor.clearDisconnectBanner()

    fun verifyBitPerfectMixer() {
        val sr = playbackState.value.currentTrack?.sampleRateHz ?: 48000
        _mixerCheckResult.value = deviceMonitor.configureBitPerfectMixerIfSupported(sr)
    }

    // Settings
    fun setGapless(enabled: Boolean) = viewModelScope.launch { prefsRepo.setGaplessPlayback(enabled) }
    fun setAudioFocus(enabled: Boolean) = viewModelScope.launch { prefsRepo.setAudioFocusEnabled(enabled) }
    fun setResumeOnRestart(enabled: Boolean) = viewModelScope.launch { prefsRepo.setResumePlayback(enabled) }
    fun setVisualizerEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setVisualizerEnabled(enabled) }
    fun setArtworkEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setArtworkEnabled(enabled) }
    fun setNotificationEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setNotificationEnabled(enabled) }
    fun setBufferPreset(preset: BufferPreset) = viewModelScope.launch { prefsRepo.setBufferPreset(preset) }
    fun setPreferDirectRate(enabled: Boolean) = viewModelScope.launch { prefsRepo.setPreferDirectRate(enabled) }

    // Audio Test Lab
    fun startTestTone(mode: TestToneMode) {
        if (playbackState.value.isPlaying) {
            playbackController.togglePlayPause()
        }
        val sr = routingState.value.activeDevice?.supportedSampleRates?.firstOrNull { it >= 44100 } ?: 48000
        toneEngine.startTone(mode, sr)
    }

    fun stopTestTone() = toneEngine.stop()
    fun setSineFrequency(hz: Float) = toneEngine.setSineFrequency(hz)
    fun setToneLevelDb(db: Float) = toneEngine.setOutputLevelDb(db)
    fun setToneBalance(bal: Float) = toneEngine.setChannelBalance(bal)

    // Audio Quality Testing
    fun runQualityDiagnostics() {
        if (_isRunningTests.value) return
        viewModelScope.launch {
            _isRunningTests.value = true
            val results = diagnostics.runDiagnostics(
                libraryTracks = allTracks.value,
                isServiceConnected = playbackState.value.isServiceConnected,
                isGaplessEnabled = preferences.value.gaplessPlayback
            )
            _qualityTests.value = results
            _isRunningTests.value = false
        }
    }

    override fun onCleared() {
        toneEngine.stop()
        super.onCleared()
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SonicFlowViewModel(container) as T
        }
    }
}
