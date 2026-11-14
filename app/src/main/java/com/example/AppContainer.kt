package com.example

import android.content.Context
import com.example.audio.controller.PlaybackController
import com.example.audio.dsp.SonicFlowAudioProcessor
import com.example.audio.routing.AudioDeviceMonitor
import com.example.audio.testlab.AudioQualityDiagnostics
import com.example.audio.testlab.ToneGeneratorEngine
import com.example.data.db.SonicFlowDatabase
import com.example.data.prefs.UserPreferencesRepository
import com.example.data.repository.MusicLibraryRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: SonicFlowDatabase by lazy {
        SonicFlowDatabase.getInstance(appContext)
    }

    val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    val libraryRepository: MusicLibraryRepository by lazy {
        MusicLibraryRepository(appContext, database.sonicFlowDao())
    }

    val audioProcessor: SonicFlowAudioProcessor by lazy {
        SonicFlowAudioProcessor()
    }

    val deviceMonitor: AudioDeviceMonitor by lazy {
        AudioDeviceMonitor(appContext)
    }

    val toneGeneratorEngine: ToneGeneratorEngine by lazy {
        ToneGeneratorEngine()
    }

    val qualityDiagnostics: AudioQualityDiagnostics by lazy {
        AudioQualityDiagnostics(appContext, deviceMonitor)
    }

    val playbackController: PlaybackController by lazy {
        PlaybackController(
            context = appContext,
            libraryRepository = libraryRepository,
            preferencesRepository = preferencesRepository,
            audioProcessor = audioProcessor,
            deviceMonitor = deviceMonitor
        )
    }
}
