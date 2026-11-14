package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AppContainer
import com.example.audio.controller.PlaybackUiState
import com.example.audio.dsp.EqPresets
import com.example.audio.routing.AudioRoutingState
import com.example.audio.testlab.TestToneMode
import com.example.audio.testlab.ToneLabState
import com.example.data.db.PlaylistEntity
import com.example.data.db.ScanFolderEntity
import com.example.data.model.AccentColorOption
import com.example.data.model.AppThemeStyle
import com.example.data.model.ArtworkStyle
import com.example.data.model.BufferPreset
import com.example.data.model.DspSettings
import com.example.data.model.DspTelemetry
import com.example.data.model.EqBand
import com.example.data.model.EqPreset
import com.example.data.model.EqUiMode
import com.example.data.model.FilterType
import com.example.data.model.LibraryFormatFilter
import com.example.data.model.LibrarySortOrder
import com.example.data.model.MixerVerificationStatus
import com.example.data.model.OutputProfileType
import com.example.data.model.OutputRouteCategory
import com.example.data.model.PlayerLayoutMode
import com.example.data.model.QualityTestItem
import com.example.data.model.QualityTier
import com.example.data.model.ReplayGainMode
import com.example.data.model.SimpleEqPreset
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists")
}

private data class EqUndoEntry(
    val dsp: DspSettings,
    val uiMode: EqUiMode,
    val simplePreset: SimpleEqPreset,
    val simpleIntensity: Float
)

class SonicFlowViewModel(private val container: AppContainer) : ViewModel() {

    private val libraryRepo = container.libraryRepository
    private val prefsRepo = container.preferencesRepository
    private val playbackController = container.playbackController
    private val deviceMonitor = container.deviceMonitor
    private val audioProcessor = container.audioProcessor
    private val toneEngine = container.toneGeneratorEngine
    private val diagnostics = container.qualityDiagnostics

    private val eqUndoStack = ArrayDeque<EqUndoEntry>()
    private val _canUndoEq = MutableStateFlow(false)
    val canUndoEq: StateFlow<Boolean> = _canUndoEq.asStateFlow()

    private var lastObservedRouteCategory: OutputRouteCategory? = null

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

    private val _selectedLibraryTab = MutableStateFlow(LibraryTab.SONGS)
    val selectedLibraryTab: StateFlow<LibraryTab> = _selectedLibraryTab.asStateFlow()

    private val _formatFilter = MutableStateFlow(LibraryFormatFilter.ALL)
    val formatFilter: StateFlow<LibraryFormatFilter> = _formatFilter.asStateFlow()

    private val _selectedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTrackIds: StateFlow<Set<Long>> = _selectedTrackIds.asStateFlow()

    private val _mixerCheckResult = MutableStateFlow<MixerVerificationStatus?>(null)
    val mixerCheckResult: StateFlow<MixerVerificationStatus?> = _mixerCheckResult.asStateFlow()

    private val _qualityTests = MutableStateFlow<List<QualityTestItem>>(emptyList())
    val qualityTests: StateFlow<List<QualityTestItem>> = _qualityTests.asStateFlow()

    private val _isRunningTests = MutableStateFlow(false)
    val isRunningTests: StateFlow<Boolean> = _isRunningTests.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            libraryRepo.ensureBuiltInPresetsSeeded()
        }
        observeOutputRouteForPerDeviceEq()
    }

    private fun observeOutputRouteForPerDeviceEq() {
        viewModelScope.launch {
            routingState.collect { routing ->
                val newCategory = routing.activeDevice?.routeCategory ?: return@collect
                val prevCategory = lastObservedRouteCategory
                if (prevCategory == null) {
                    lastObservedRouteCategory = newCategory
                    return@collect
                }
                if (prevCategory != newCategory) {
                    lastObservedRouteCategory = newCategory
                    val prefs = prefsRepo.preferencesFlow.first()
                    if (prefs.rememberEqPerDevice) {
                        prefsRepo.saveDeviceEqSnapshot(
                            routeCategory = prevCategory,
                            dsp = prefs.dspSettings,
                            uiMode = prefs.eqUiMode,
                            simplePreset = prefs.simpleEqPreset,
                            intensity = prefs.simpleEqIntensity
                        )
                        val saved = prefsRepo.parseDeviceEqSnapshot(prefs.deviceEqProfilesJson, newCategory)
                        if (saved != null) {
                            prefsRepo.applyDeviceEqSnapshot(saved)
                            _statusMessage.value = "Restored EQ profile for ${newCategory.displayName}"
                        }
                    }
                }
            }
        }
    }

    val filteredTracks: StateFlow<List<Track>> = combine(
        allTracks,
        _searchQuery,
        _formatFilter,
        preferences
    ) { tracks, q, filter, prefs ->
        val query = q.trim()
        val afterFilter = when (filter) {
            LibraryFormatFilter.ALL -> tracks
            LibraryFormatFilter.FAVORITES -> tracks.filter { it.isFavorite }
            LibraryFormatFilter.HI_RES -> tracks.filter { it.qualityTier == QualityTier.HI_RES }
            LibraryFormatFilter.LOSSLESS -> tracks.filter {
                it.qualityTier == QualityTier.HI_RES || it.qualityTier == QualityTier.LOSSLESS_CD
            }
            LibraryFormatFilter.LOSSY -> tracks.filter { it.qualityTier == QualityTier.LOSSY }
        }
        val afterSearch = if (query.isBlank()) {
            afterFilter
        } else {
            afterFilter.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true) ||
                    it.genre.contains(query, ignoreCase = true) ||
                    it.codec.contains(query, ignoreCase = true)
            }
        }
        when (prefs.librarySortOrder) {
            LibrarySortOrder.TITLE_ASC -> afterSearch.sortedBy { it.title.lowercase() }
            LibrarySortOrder.ARTIST_ASC -> afterSearch.sortedWith(compareBy({ it.artist.lowercase() }, { it.album.lowercase() }, { it.trackNumber }))
            LibrarySortOrder.DATE_ADDED_DESC -> afterSearch.sortedByDescending { it.dateAdded }
            LibrarySortOrder.DURATION_DESC -> afterSearch.sortedByDescending { it.durationMs }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Onboarding
    fun completeOnboarding() {
        viewModelScope.launch {
            prefsRepo.setHasCompletedOnboarding(true)
        }
    }

    // Search / Filter / Sort / Multi-select
    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun setLibraryTab(tab: LibraryTab) {
        _selectedLibraryTab.value = tab
    }

    fun setFormatFilter(filter: LibraryFormatFilter) {
        _formatFilter.value = filter
    }

    fun setLibrarySortOrder(order: LibrarySortOrder) {
        viewModelScope.launch {
            prefsRepo.setLibrarySortOrder(order)
        }
    }

    fun toggleTrackSelection(trackId: Long) {
        val current = _selectedTrackIds.value.toMutableSet()
        if (!current.add(trackId)) current.remove(trackId)
        _selectedTrackIds.value = current
    }

    fun clearTrackSelection() {
        _selectedTrackIds.value = emptySet()
    }

    fun selectAllVisibleTracks() {
        _selectedTrackIds.value = filteredTracks.value.map { it.id }.toSet()
    }

    fun addSelectedToQueue() {
        val ids = _selectedTrackIds.value
        val selected = allTracks.value.filter { it.id in ids }
        if (selected.isNotEmpty()) {
            playbackController.addTracksToQueue(selected)
            _statusMessage.value = "Added ${selected.size} tracks to queue"
        }
        clearTrackSelection()
    }

    fun favoriteSelectedTracks() {
        val ids = _selectedTrackIds.value
        viewModelScope.launch {
            val targets = allTracks.value.filter { it.id in ids && !it.isFavorite }
            targets.forEach { libraryRepo.toggleFavorite(it.id, false) }
            _statusMessage.value = "Marked ${ids.size} tracks as favorite"
            clearTrackSelection()
        }
    }

    fun addSelectedToPlaylist(playlistId: Long) {
        val ids = _selectedTrackIds.value.toList()
        viewModelScope.launch {
            ids.forEach { trackId -> libraryRepo.addTrackToPlaylist(playlistId, trackId) }
            _statusMessage.value = "Added ${ids.size} tracks to playlist"
            clearTrackSelection()
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // SAF Library Actions
    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            val count = libraryRepo.addAndScanFolder(uri)
            _statusMessage.value = if (count > 0) "Added $count songs" else "No audio files found in that folder"
        }
    }

    fun rescanFolders() {
        viewModelScope.launch {
            val count = libraryRepo.rescanAllFolders(scanFolders.value)
            _statusMessage.value = "Library updated ($count songs)"
        }
    }

    fun removeFolder(treeUri: String) {
        viewModelScope.launch {
            libraryRepo.removeFolder(treeUri)
            _statusMessage.value = "Folder removed from library"
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            libraryRepo.toggleFavorite(track.id, track.isFavorite)
            _statusMessage.value = if (!track.isFavorite) "Added to Favorites" else "Removed from Favorites"
        }
    }

    // Playlists
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            libraryRepo.createPlaylist(name)
            _statusMessage.value = "Created playlist \"$name\""
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
        val autoPlay = preferences.value.autoPlayOnSelect
        playbackController.playTrackList(tracks, startIndex, autoPlay = autoPlay)
    }

    fun shuffleAndPlay(tracks: List<Track>) {
        toneEngine.stop()
        playbackController.shuffleAndPlay(tracks)
    }

    fun shuffleCurrentQueue() = playbackController.shuffleCurrentQueue()
    fun clearQueue() = playbackController.clearQueue()

    fun playNext(track: Track) {
        playbackController.playNext(track)
        _statusMessage.value = "\"${track.title}\" will play next"
    }

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
    fun setSleepTimerMinutes(minutes: Int) {
        playbackController.setSleepTimerMinutes(minutes)
        _statusMessage.value = if (minutes > 0) "Sleep timer set for $minutes minutes" else "Sleep timer turned off"
    }
    fun clearPlaybackError() = playbackController.clearError()

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            prefsRepo.setPlaybackSpeed(speed)
        }
    }

    fun setFadeInOut(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setFadeInOutEnabled(enabled) }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch { prefsRepo.setCrossfadeSeconds(seconds) }
    }

    // EQ Undo / Reset / Per-Device Persistence
    private fun pushEqUndoSnapshot() {
        val prefs = preferences.value
        if (eqUndoStack.size >= 15) eqUndoStack.removeFirst()
        eqUndoStack.addLast(
            EqUndoEntry(
                dsp = prefs.dspSettings,
                uiMode = prefs.eqUiMode,
                simplePreset = prefs.simpleEqPreset,
                simpleIntensity = prefs.simpleEqIntensity
            )
        )
        _canUndoEq.value = eqUndoStack.isNotEmpty()
    }

    private suspend fun persistCurrentDeviceEqIfEnabled() {
        val prefs = prefsRepo.preferencesFlow.first()
        val route = routingState.value.activeDevice?.routeCategory ?: return
        if (prefs.rememberEqPerDevice) {
            prefsRepo.saveDeviceEqSnapshot(
                routeCategory = route,
                dsp = prefs.dspSettings,
                uiMode = prefs.eqUiMode,
                simplePreset = prefs.simpleEqPreset,
                intensity = prefs.simpleEqIntensity
            )
        }
    }

    fun undoEqChange() {
        val prev = eqUndoStack.removeLastOrNull() ?: return
        _canUndoEq.value = eqUndoStack.isNotEmpty()
        viewModelScope.launch {
            prefsRepo.restoreFullDsp(prev.dsp)
            prefsRepo.setEqUiMode(prev.uiMode)
            if (prev.simplePreset != SimpleEqPreset.CUSTOM) {
                // Keep exact restored DSP without overriding bands
            }
            persistCurrentDeviceEqIfEnabled()
            _statusMessage.value = "Undid last equalizer change"
        }
    }

    fun resetEqToNeutral() {
        pushEqUndoSnapshot()
        viewModelScope.launch {
            val neutral = EqPresets.builtInPresets.first()
            prefsRepo.updateDsp {
                it.copy(
                    bitTransparentMode = false,
                    dspMasterEnabled = true,
                    eqEnabled = true,
                    eqBypassBeforeAfter = false,
                    activePresetId = neutral.id,
                    preampDb = 0f,
                    bands = neutral.bands
                )
            }
            prefsRepo.applySimpleEq(SimpleEqPreset.NATURAL, 0f)
            persistCurrentDeviceEqIfEnabled()
            _statusMessage.value = "Equalizer reset to neutral"
        }
    }

    fun setEqUiMode(mode: EqUiMode) {
        viewModelScope.launch {
            prefsRepo.setEqUiMode(mode)
            persistCurrentDeviceEqIfEnabled()
        }
    }

    fun selectSimpleEqPreset(preset: SimpleEqPreset) {
        pushEqUndoSnapshot()
        viewModelScope.launch {
            val intensity = preferences.value.simpleEqIntensity.let { if (it <= 0.05f) 1.0f else it }
            prefsRepo.applySimpleEq(preset, intensity)
            persistCurrentDeviceEqIfEnabled()
        }
    }

    fun setSimpleEqIntensity(intensity: Float) {
        viewModelScope.launch {
            val currentPreset = preferences.value.simpleEqPreset
            if (currentPreset == SimpleEqPreset.CUSTOM) {
                // Scale current bands around neutral lightly or mark intensity
                val prevIntensity = preferences.value.simpleEqIntensity.coerceAtLeast(0.1f)
                val ratio = (intensity / prevIntensity).coerceIn(0f, 2f)
                prefsRepo.updateDsp { dsp ->
                    dsp.copy(
                        bands = dsp.bands.map { b -> b.copy(gainDb = (b.gainDb * ratio).coerceIn(-12f, 12f)) },
                        preampDb = (dsp.preampDb * ratio).coerceIn(-15f, 6f)
                    )
                }
            } else {
                prefsRepo.applySimpleEq(currentPreset, intensity)
            }
            persistCurrentDeviceEqIfEnabled()
        }
    }

    fun setRememberEqPerDevice(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setRememberEqPerDevice(enabled)
        }
    }

    // DSP & EQ Actions
    fun setBitTransparentMode(enabled: Boolean) {
        pushEqUndoSnapshot()
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(bitTransparentMode = enabled, dspMasterEnabled = !enabled) }
            if (enabled) {
                val sr = playbackState.value.currentTrack?.sampleRateHz ?: 0
                deviceMonitor.configureBitPerfectMixerIfSupported(sr)
            } else {
                deviceMonitor.clearPreferredMixerAttributes()
            }
            persistCurrentDeviceEqIfEnabled()
        }
    }

    fun setDspMasterEnabled(enabled: Boolean) {
        pushEqUndoSnapshot()
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(dspMasterEnabled = enabled, bitTransparentMode = !enabled) }
            persistCurrentDeviceEqIfEnabled()
        }
    }

    fun setEqEnabled(enabled: Boolean) {
        pushEqUndoSnapshot()
        viewModelScope.launch {
            prefsRepo.updateDsp { it.copy(eqEnabled = enabled) }
            persistCurrentDeviceEqIfEnabled()
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
            persistCurrentDeviceEqIfEnabled()
        }
    }

    fun updateEqBand(updated: EqBand) {
        viewModelScope.launch {
            prefsRepo.updateDsp { current ->
                val list = current.bands.map { if (it.index == updated.index) updated else it }
                current.copy(bands = list, activePresetId = 9L)
            }
            prefsRepo.markSimpleEqCustom()
            persistCurrentDeviceEqIfEnabled()
        }
    }

    fun addEqBand() {
        pushEqUndoSnapshot()
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
            prefsRepo.markSimpleEqCustom()
        }
    }

    fun removeLastEqBand() {
        pushEqUndoSnapshot()
        viewModelScope.launch {
            prefsRepo.updateDsp { current ->
                if (current.bands.size <= 10) return@updateDsp current
                current.copy(bands = current.bands.dropLast(1), activePresetId = 9L)
            }
            prefsRepo.markSimpleEqCustom()
        }
    }

    fun selectEqPreset(preset: EqPreset) {
        pushEqUndoSnapshot()
        viewModelScope.launch {
            prefsRepo.applyBandsAndPreset(preset.id, preset.preampDb, preset.bands)
            persistCurrentDeviceEqIfEnabled()
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
        pushEqUndoSnapshot()
        viewModelScope.launch {
            val id = libraryRepo.saveCustomEqPreset(imported.name, imported.preampDb, imported.bands)
            prefsRepo.applyBandsAndPreset(id, imported.preampDb, imported.bands)
            prefsRepo.markSimpleEqCustom()
            persistCurrentDeviceEqIfEnabled()
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
        pushEqUndoSnapshot()
        viewModelScope.launch {
            prefsRepo.applySoundProfile(profile)
            persistCurrentDeviceEqIfEnabled()
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

    // Settings & Personalization
    fun setGapless(enabled: Boolean) = viewModelScope.launch { prefsRepo.setGaplessPlayback(enabled) }
    fun setAudioFocus(enabled: Boolean) = viewModelScope.launch { prefsRepo.setAudioFocusEnabled(enabled) }
    fun setResumeOnRestart(enabled: Boolean) = viewModelScope.launch { prefsRepo.setResumePlayback(enabled) }
    fun setVisualizerEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setVisualizerEnabled(enabled) }
    fun setArtworkEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setArtworkEnabled(enabled) }
    fun setNotificationEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setNotificationEnabled(enabled) }
    fun setBufferPreset(preset: BufferPreset) = viewModelScope.launch { prefsRepo.setBufferPreset(preset) }
    fun setPreferDirectRate(enabled: Boolean) = viewModelScope.launch { prefsRepo.setPreferDirectRate(enabled) }
    fun setThemeStyle(style: AppThemeStyle) = viewModelScope.launch { prefsRepo.setThemeStyle(style) }
    fun setAccentColor(option: AccentColorOption) = viewModelScope.launch { prefsRepo.setAccentColor(option) }
    fun setArtworkStyle(style: ArtworkStyle) = viewModelScope.launch { prefsRepo.setArtworkStyle(style) }
    fun setPlayerLayout(layout: PlayerLayoutMode) = viewModelScope.launch { prefsRepo.setPlayerLayout(layout) }
    fun setGesturesEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setGesturesEnabled(enabled) }
    fun setAutoPlayOnSelect(enabled: Boolean) = viewModelScope.launch { prefsRepo.setAutoPlayOnSelect(enabled) }

    fun resetAllSettingsToDefault() {
        viewModelScope.launch {
            prefsRepo.resetAllToDefaults()
            eqUndoStack.clear()
            _canUndoEq.value = false
            _statusMessage.value = "All settings restored to defaults"
        }
    }

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
