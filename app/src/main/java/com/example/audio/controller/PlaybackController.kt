package com.example.audio.controller

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.audio.dsp.SonicFlowAudioProcessor
import com.example.audio.routing.AudioDeviceMonitor
import com.example.audio.service.SonicFlowPlaybackService
import com.example.data.model.ErrorTranslator
import com.example.data.model.PlaybackAudioInfo
import com.example.data.model.QualityTier
import com.example.data.model.Track
import com.example.data.model.UserFriendlyError
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.repository.MusicLibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val isServiceConnected: Boolean = false,
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerRemainingMs: Long = 0L,
    val audioInfo: PlaybackAudioInfo = PlaybackAudioInfo(),
    val errorMessage: String? = null,
    val friendlyError: UserFriendlyError? = null
)

class PlaybackController(
    private val context: Context,
    private val libraryRepository: MusicLibraryRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val audioProcessor: SonicFlowAudioProcessor,
    private val deviceMonitor: AudioDeviceMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaController: MediaController? = null
    private var positionTickerJob: Job? = null
    private var fadeJob: Job? = null
    private var restoredOnce = false
    private var crossfadeTriggeredForUri: String? = null

    private var fadeInOutEnabled: Boolean = true
    private var crossfadeSeconds: Int = 0
    private var isBitTransparent: Boolean = false

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    init {
        connectToService()
        observeDspAndRouting()
        deviceMonitor.setDisconnectListener { _ ->
            mediaController?.pause()
            audioProcessor.setPlaybackActive(false)
        }
    }

    private fun connectToService() {
        val sessionToken = SessionToken(context, ComponentName(context, SonicFlowPlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                try {
                    val controller = controllerFuture.get()
                    mediaController = controller
                    attachPlayerListener(controller)
                    _uiState.value = _uiState.value.copy(isServiceConnected = true)
                    startPositionTicker()
                    restoreLastSessionIfNeeded()
                } catch (e: Exception) {
                    val friendly = ErrorTranslator.toFriendlyError(e.localizedMessage)
                    _uiState.value = _uiState.value.copy(
                        isServiceConnected = false,
                        errorMessage = friendly.message,
                        friendlyError = friendly
                    )
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun observeDspAndRouting() {
        scope.launch {
            combine(
                preferencesRepository.preferencesFlow,
                deviceMonitor.routingState,
                SonicFlowPlaybackService.decoderTelemetry,
                audioProcessor.telemetry
            ) { prefs, routing, decoder, dspTel ->
                audioProcessor.updateSettings(prefs.dspSettings)
                audioProcessor.setVisualizerEnabled(prefs.visualizerEnabled)
                fadeInOutEnabled = prefs.fadeInOutEnabled
                crossfadeSeconds = prefs.crossfadeSeconds
                isBitTransparent = prefs.dspSettings.bitTransparentMode

                val effectiveSpeed = if (prefs.dspSettings.bitTransparentMode) 1.0f else prefs.playbackSpeed
                mediaController?.let { ctrl ->
                    if (ctrl.playbackParameters.speed != effectiveSpeed) {
                        ctrl.setPlaybackSpeed(effectiveSpeed)
                    }
                }
                if (_uiState.value.playbackSpeed != effectiveSpeed) {
                    _uiState.value = _uiState.value.copy(playbackSpeed = effectiveSpeed)
                }

                val track = _uiState.value.currentTrack
                if (prefs.preferDirectBitPerfectRate && prefs.dspSettings.bitTransparentMode && track != null) {
                    deviceMonitor.configureBitPerfectMixerIfSupported(track.sampleRateHz)
                }

                val activeRoute = routing.activeDevice
                val activeDspText = when {
                    prefs.dspSettings.bitTransparentMode -> "Bypassed (Bit-Transparent Mode)"
                    !prefs.dspSettings.dspMasterEnabled -> "Bypassed (DSP Off)"
                    else -> buildList {
                        if (prefs.dspSettings.eqEnabled && !prefs.dspSettings.eqBypassBeforeAfter) add("Parametric EQ")
                        if (prefs.dspSettings.crossfeedEnabled) add("Crossfeed")
                        if (prefs.dspSettings.limiterEnabled) add("Limiter")
                        if (prefs.dspSettings.replayGainMode.name != "OFF") add("ReplayGain")
                        if (effectiveSpeed != 1.0f) add("Speed ${effectiveSpeed}x")
                    }.joinToString(" + ").ifBlank { "Unity Gain" }
                }

                val isBitTransparentActive = prefs.dspSettings.bitTransparentMode && !dspTel.isDspActuallyActive && effectiveSpeed == 1.0f

                PlaybackAudioInfo(
                    sourceCodec = track?.codec ?: "—",
                    sourceSampleRateHz = track?.sampleRateHz ?: 0,
                    sourceBitDepth = track?.bitDepth ?: 0,
                    sourceBitrateKbps = track?.bitrateKbps ?: 0,
                    sourceChannels = track?.channels ?: 0,
                    sourceDurationMs = track?.durationMs ?: 0L,
                    sourceFileSizeBytes = track?.fileSizeBytes ?: 0L,
                    qualityTier = track?.qualityTier ?: QualityTier.UNKNOWN,
                    decoderUsed = decoder.decoderName,
                    decodedPcmEncoding = decoder.audioTrackEncoding.takeIf { it != "—" } ?: decoder.decodedPcmEncoding,
                    decodedSampleRateHz = decoder.audioTrackSampleRateHz.takeIf { it > 0 } ?: decoder.decodedSampleRateHz,
                    decodedChannelCount = decoder.decodedChannelCount,
                    requestedSampleRateHz = track?.sampleRateHz ?: 0,
                    outputRouteLabel = activeRoute?.let { "${it.routeCategory.displayName} (${it.productName})" } ?: "System Default",
                    activeDspSummary = activeDspText,
                    processingState = when {
                        isBitTransparentActive -> "Bit-Transparent Pass-Through"
                        dspTel.isDspActuallyActive -> "64-bit Float DSP Active"
                        else -> "Standby"
                    },
                    isBitTransparentRequested = prefs.dspSettings.bitTransparentMode,
                    isBitTransparentActive = isBitTransparentActive
                )
            }.collect { audioInfo ->
                _uiState.value = _uiState.value.copy(audioInfo = audioInfo)
            }
        }
    }

    private fun attachPlayerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                audioProcessor.setPlaybackActive(isPlaying)
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = controller.currentMediaItemIndex
                val queue = _uiState.value.queue
                val track = queue.getOrNull(idx)
                crossfadeTriggeredForUri = null
                audioProcessor.updateReplayGainMetadata(track?.replayGainTrackDb, track?.replayGainAlbumDb)
                _uiState.value = _uiState.value.copy(
                    currentIndex = idx,
                    currentTrack = track,
                    durationMs = controller.duration.coerceAtLeast(track?.durationMs ?: 0L)
                )
                if (fadeInOutEnabled && !isBitTransparent && controller.isPlaying) {
                    rampVolume(0.25f, 1.0f, 240L)
                } else {
                    controller.volume = 1.0f
                }
                if (track != null) {
                    scope.launch {
                        libraryRepository.markTrackPlayed(track.uri)
                        preferencesRepository.saveLastPlayback(track.uri, 0L)
                        preferencesRepository.saveQueueState(queue.map { it.uri }, idx)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val dur = controller.duration
                if (dur > 0L) {
                    _uiState.value = _uiState.value.copy(durationMs = dur)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _uiState.value = _uiState.value.copy(shuffleEnabled = shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _uiState.value = _uiState.value.copy(repeatMode = repeatMode)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                audioProcessor.setPlaybackActive(false)
                val friendly = ErrorTranslator.toFriendlyError(error.localizedMessage ?: error.errorCodeName)
                _uiState.value = _uiState.value.copy(
                    errorMessage = friendly.message,
                    friendlyError = friendly
                )
            }
        })
    }

    private fun restoreLastSessionIfNeeded() {
        if (restoredOnce) return
        restoredOnce = true
        scope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            if (!prefs.resumePlaybackOnRestart) return@launch
            val tracks = libraryRepository.getAllTracksSnapshot()
            if (tracks.isEmpty()) return@launch
            val byUri = tracks.associateBy { it.uri }
            val savedUris = preferencesRepository.parseSavedQueueUris(prefs.savedQueueUrisJson)
            val restoredQueue = savedUris.mapNotNull { byUri[it] }
            if (restoredQueue.isNotEmpty()) {
                val idx = prefs.savedQueueIndex.coerceIn(0, restoredQueue.lastIndex)
                prepareQueue(restoredQueue, idx, autoPlay = false, startPositionMs = prefs.lastPlayedPositionMs)
            } else if (prefs.lastPlayedUri != null) {
                val idx = tracks.indexOfFirst { it.uri == prefs.lastPlayedUri }
                if (idx >= 0) {
                    prepareQueue(tracks, idx, autoPlay = false, startPositionMs = prefs.lastPlayedPositionMs)
                }
            }
        }
    }

    fun playTrackList(tracks: List<Track>, startIndex: Int, autoPlay: Boolean = true) {
        if (tracks.isEmpty()) return
        val safeIdx = startIndex.coerceIn(0, tracks.lastIndex)
        prepareQueue(tracks, safeIdx, autoPlay = autoPlay, startPositionMs = 0L)
    }

    fun shuffleAndPlay(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val shuffled = tracks.shuffled()
        prepareQueue(shuffled, 0, autoPlay = true, startPositionMs = 0L)
    }

    fun shuffleCurrentQueue() {
        val currentQueue = _uiState.value.queue
        if (currentQueue.size <= 1) return
        val current = _uiState.value.currentTrack
        val remaining = currentQueue.filter { it.id != current?.id }.shuffled()
        val nextQueue = if (current != null) listOf(current) + remaining else remaining
        val controller = mediaController ?: return
        val wasPlaying = controller.isPlaying
        val pos = controller.currentPosition
        _uiState.value = _uiState.value.copy(queue = nextQueue, currentIndex = 0)
        controller.setMediaItems(nextQueue.map { it.toMediaItem() }, 0, pos)
        controller.prepare()
        if (wasPlaying) controller.play()
        scope.launch { preferencesRepository.saveQueueState(nextQueue.map { it.uri }, 0) }
    }

    fun clearQueue() {
        val controller = mediaController ?: return
        controller.stop()
        controller.clearMediaItems()
        audioProcessor.setPlaybackActive(false)
        _uiState.value = _uiState.value.copy(
            queue = emptyList(),
            currentTrack = null,
            currentIndex = -1,
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L
        )
        scope.launch { preferencesRepository.saveQueueState(emptyList(), 0) }
    }

    private fun prepareQueue(
        tracks: List<Track>,
        startIndex: Int,
        autoPlay: Boolean,
        startPositionMs: Long
    ) {
        val controller = mediaController ?: return
        val items = tracks.map { it.toMediaItem() }
        val selected = tracks[startIndex]
        crossfadeTriggeredForUri = null
        audioProcessor.updateReplayGainMetadata(selected.replayGainTrackDb, selected.replayGainAlbumDb)
        _uiState.value = _uiState.value.copy(
            queue = tracks,
            currentIndex = startIndex,
            currentTrack = selected,
            durationMs = selected.durationMs,
            errorMessage = null,
            friendlyError = null
        )
        controller.setMediaItems(items, startIndex, startPositionMs)
        controller.prepare()
        if (autoPlay) {
            if (fadeInOutEnabled && !isBitTransparent) {
                controller.volume = 0.2f
                controller.play()
                rampVolume(0.2f, 1.0f, 260L)
            } else {
                controller.volume = 1.0f
                controller.play()
            }
        }
        scope.launch {
            if (autoPlay) libraryRepository.markTrackPlayed(selected.uri)
            preferencesRepository.saveLastPlayback(selected.uri, startPositionMs)
            preferencesRepository.saveQueueState(tracks.map { it.uri }, startIndex)
        }
    }

    fun addToQueue(track: Track) {
        val controller = mediaController ?: return
        val currentQueue = _uiState.value.queue.toMutableList()
        if (currentQueue.isEmpty()) {
            playTrackList(listOf(track), 0)
            return
        }
        currentQueue.add(track)
        _uiState.value = _uiState.value.copy(queue = currentQueue)
        controller.addMediaItem(track.toMediaItem())
        scope.launch { preferencesRepository.saveQueueState(currentQueue.map { it.uri }, _uiState.value.currentIndex.coerceAtLeast(0)) }
    }

    fun addTracksToQueue(tracks: List<Track>) {
        if (tracks.isEmpty()) return
        val controller = mediaController ?: return
        val currentQueue = _uiState.value.queue.toMutableList()
        if (currentQueue.isEmpty()) {
            playTrackList(tracks, 0)
            return
        }
        currentQueue.addAll(tracks)
        _uiState.value = _uiState.value.copy(queue = currentQueue)
        controller.addMediaItems(tracks.map { it.toMediaItem() })
        scope.launch { preferencesRepository.saveQueueState(currentQueue.map { it.uri }, _uiState.value.currentIndex.coerceAtLeast(0)) }
    }

    fun playNext(track: Track) {
        val controller = mediaController ?: return
        val currentQueue = _uiState.value.queue.toMutableList()
        if (currentQueue.isEmpty()) {
            playTrackList(listOf(track), 0)
            return
        }
        val insertIndex = (controller.currentMediaItemIndex + 1).coerceIn(0, currentQueue.size)
        currentQueue.add(insertIndex, track)
        _uiState.value = _uiState.value.copy(queue = currentQueue)
        controller.addMediaItem(insertIndex, track.toMediaItem())
        scope.launch { preferencesRepository.saveQueueState(currentQueue.map { it.uri }, _uiState.value.currentIndex.coerceAtLeast(0)) }
    }

    fun jumpToQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in _uiState.value.queue.indices) {
            controller.seekToDefaultPosition(index)
            controller.play()
        }
    }

    fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        val currentQueue = _uiState.value.queue.toMutableList()
        if (index !in currentQueue.indices) return
        currentQueue.removeAt(index)
        val newIdx = controller.currentMediaItemIndex.coerceAtMost((currentQueue.size - 1).coerceAtLeast(0))
        _uiState.value = _uiState.value.copy(queue = currentQueue, currentIndex = newIdx)
        controller.removeMediaItem(index)
        scope.launch { preferencesRepository.saveQueueState(currentQueue.map { it.uri }, newIdx) }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            if (fadeInOutEnabled && !isBitTransparent) {
                fadeJob?.cancel()
                fadeJob = scope.launch {
                    val steps = 8
                    for (i in steps downTo 1) {
                        controller.volume = i / steps.toFloat()
                        delay(22L)
                    }
                    controller.pause()
                    controller.volume = 1.0f
                }
            } else {
                controller.pause()
            }
        } else {
            if (fadeInOutEnabled && !isBitTransparent) {
                controller.volume = 0.2f
                controller.play()
                rampVolume(0.2f, 1.0f, 240L)
            } else {
                controller.volume = 1.0f
                controller.play()
            }
        }
    }

    private fun rampVolume(from: Float, to: Float, durationMs: Long) {
        val controller = mediaController ?: return
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 10
            val stepDelay = (durationMs / steps).coerceAtLeast(12L)
            for (i in 1..steps) {
                val t = i / steps.toFloat()
                controller.volume = (from + (to - from) * t).coerceIn(0f, 1f)
                delay(stepDelay)
            }
            controller.volume = to
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs.coerceAtLeast(0L))
        _uiState.value = _uiState.value.copy(positionMs = positionMs)
    }

    fun seekToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun seekToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3500L) {
            controller.seekTo(0L)
        } else {
            controller.seekToPreviousMediaItem()
        }
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSleepTimerMinutes(minutes: Int) {
        scope.launch {
            if (minutes <= 0) {
                preferencesRepository.setSleepTimerEnd(0L)
                _uiState.value = _uiState.value.copy(sleepTimerRemainingMs = 0L)
            } else {
                val end = System.currentTimeMillis() + minutes * 60_000L
                preferencesRepository.setSleepTimerEnd(end)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null, friendlyError = null)
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = scope.launch {
            var persistTicks = 0
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0L } ?: (_uiState.value.currentTrack?.durationMs ?: 0L)
                    _uiState.value = _uiState.value.copy(positionMs = pos, durationMs = dur)

                    // Gentle Crossfade near end of track when enabled and not Bit-Transparent
                    val currentUri = _uiState.value.currentTrack?.uri
                    if (crossfadeSeconds > 0 &&
                        !isBitTransparent &&
                        controller.isPlaying &&
                        controller.repeatMode != Player.REPEAT_MODE_ONE &&
                        controller.hasNextMediaItem() &&
                        dur > (crossfadeSeconds + 3) * 1000L &&
                        currentUri != null
                    ) {
                        val remainingMs = dur - pos
                        val fadeWindowMs = crossfadeSeconds * 1000L
                        if (remainingMs in 1L..fadeWindowMs) {
                            val ratio = (remainingMs.toFloat() / fadeWindowMs.toFloat()).coerceIn(0.15f, 1.0f)
                            controller.volume = ratio
                            if (remainingMs <= 450L && crossfadeTriggeredForUri != currentUri) {
                                crossfadeTriggeredForUri = currentUri
                                controller.seekToNextMediaItem()
                            }
                        }
                    }

                    persistTicks++
                    if (persistTicks >= 10) {
                        persistTicks = 0
                        if (currentUri != null && controller.isPlaying) {
                            preferencesRepository.saveLastPlayback(currentUri, pos)
                        }
                    }
                }

                // Check sleep timer
                val prefs = preferencesRepository.preferencesFlow.first()
                if (prefs.sleepTimerEndEpochMs > 0L) {
                    val rem = prefs.sleepTimerEndEpochMs - System.currentTimeMillis()
                    if (rem <= 0L) {
                        mediaController?.pause()
                        audioProcessor.setPlaybackActive(false)
                        preferencesRepository.setSleepTimerEnd(0L)
                        _uiState.value = _uiState.value.copy(sleepTimerRemainingMs = 0L)
                    } else {
                        _uiState.value = _uiState.value.copy(sleepTimerRemainingMs = rem)
                    }
                } else if (_uiState.value.sleepTimerRemainingMs != 0L) {
                    _uiState.value = _uiState.value.copy(sleepTimerRemainingMs = 0L)
                }

                delay(250L)
            }
        }
    }

    private fun Track.toMediaItem(): MediaItem {
        val builder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setGenre(genre)
        if (!artworkCachePath.isNullOrBlank()) {
            builder.setArtworkUri(Uri.fromFile(java.io.File(artworkCachePath)))
        }
        return MediaItem.Builder()
            .setMediaId(uri)
            .setUri(Uri.parse(uri))
            .setMediaMetadata(builder.build())
            .build()
    }
}
