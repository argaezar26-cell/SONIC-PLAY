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
import com.example.audio.service.DecoderRuntimeTelemetry
import com.example.audio.service.SonicFlowPlaybackService
import com.example.data.model.PlaybackAudioInfo
import com.example.data.model.QualityTier
import com.example.data.model.Track
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
    val sleepTimerRemainingMs: Long = 0L,
    val audioInfo: PlaybackAudioInfo = PlaybackAudioInfo(),
    val errorMessage: String? = null
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
    private var restoredOnce = false

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    init {
        connectToService()
        observeDspAndRouting()
        deviceMonitor.setDisconnectListener { _ ->
            mediaController?.pause()
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
                    _uiState.value = _uiState.value.copy(
                        isServiceConnected = false,
                        errorMessage = e.localizedMessage
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
                    }.joinToString(" + ").ifBlank { "Unity Gain" }
                }

                val isBitTransparentActive = prefs.dspSettings.bitTransparentMode && !dspTel.isDspActuallyActive

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
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = controller.currentMediaItemIndex
                val queue = _uiState.value.queue
                val track = queue.getOrNull(idx)
                audioProcessor.updateReplayGainMetadata(track?.replayGainTrackDb, track?.replayGainAlbumDb)
                _uiState.value = _uiState.value.copy(
                    currentIndex = idx,
                    currentTrack = track,
                    durationMs = controller.duration.coerceAtLeast(track?.durationMs ?: 0L)
                )
                if (track != null) {
                    scope.launch { libraryRepository.markTrackPlayed(track.uri) }
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
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.localizedMessage ?: "Audio playback failed"
                )
            }
        })
    }

    private fun restoreLastSessionIfNeeded() {
        if (restoredOnce) return
        restoredOnce = true
        scope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            if (!prefs.resumePlaybackOnRestart || prefs.lastPlayedUri == null) return@launch
            val tracks = libraryRepository.getAllTracksSnapshot()
            val idx = tracks.indexOfFirst { it.uri == prefs.lastPlayedUri }
            if (idx >= 0) {
                prepareQueue(tracks, idx, autoPlay = false, startPositionMs = prefs.lastPlayedPositionMs)
            }
        }
    }

    fun playTrackList(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val safeIdx = startIndex.coerceIn(0, tracks.lastIndex)
        prepareQueue(tracks, safeIdx, autoPlay = true, startPositionMs = 0L)
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
        audioProcessor.updateReplayGainMetadata(selected.replayGainTrackDb, selected.replayGainAlbumDb)
        _uiState.value = _uiState.value.copy(
            queue = tracks,
            currentIndex = startIndex,
            currentTrack = selected,
            durationMs = selected.durationMs,
            errorMessage = null
        )
        controller.setMediaItems(items, startIndex, startPositionMs)
        controller.prepare()
        if (autoPlay) {
            controller.play()
        }
        scope.launch {
            if (autoPlay) libraryRepository.markTrackPlayed(selected.uri)
            preferencesRepository.saveLastPlayback(selected.uri, startPositionMs)
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
        _uiState.value = _uiState.value.copy(queue = currentQueue)
        controller.removeMediaItem(index)
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
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
        _uiState.value = _uiState.value.copy(errorMessage = null)
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

                    persistTicks++
                    if (persistTicks >= 10) {
                        persistTicks = 0
                        val uri = _uiState.value.currentTrack?.uri
                        if (uri != null && controller.isPlaying) {
                            preferencesRepository.saveLastPlayback(uri, pos)
                        }
                    }
                }

                // Check sleep timer
                val prefs = preferencesRepository.preferencesFlow.first()
                if (prefs.sleepTimerEndEpochMs > 0L) {
                    val rem = prefs.sleepTimerEndEpochMs - System.currentTimeMillis()
                    if (rem <= 0L) {
                        mediaController?.pause()
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
