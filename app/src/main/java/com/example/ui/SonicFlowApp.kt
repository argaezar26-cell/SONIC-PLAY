package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.OutputRouteCategory
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.AudioQualityTestingScreen
import com.example.ui.screens.AudioTestLabScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MutedSteel
import com.example.ui.theme.SonicFlowTheme
import com.example.ui.viewmodel.SonicFlowViewModel

object SonicFlowRoutes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val PLAYER = "player"
    const val EQUALIZER = "equalizer"
    const val SETTINGS = "settings"
    const val TEST_LAB = "test_lab"
    const val QUALITY_TESTS = "quality_tests"
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(SonicFlowRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(SonicFlowRoutes.LIBRARY, "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    BottomNavItem(SonicFlowRoutes.PLAYER, "Player", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
    BottomNavItem(SonicFlowRoutes.EQUALIZER, "Equalizer", Icons.Filled.Equalizer, Icons.Outlined.Equalizer),
    BottomNavItem(SonicFlowRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun SonicFlowApp(viewModel: SonicFlowViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val filteredTracks by viewModel.filteredTracks.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val recentTracks by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val scanFolders by viewModel.scanFolders.collectAsStateWithLifecycle()
    val eqPresets by viewModel.eqPresets.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val routingState by viewModel.routingState.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val dspTelemetry by viewModel.dspTelemetry.collectAsStateWithLifecycle()
    val visualizerFrame by viewModel.visualizerFrame.collectAsStateWithLifecycle()
    val toneLabState by viewModel.toneLabState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedLibraryTab by viewModel.selectedLibraryTab.collectAsStateWithLifecycle()
    val formatFilter by viewModel.formatFilter.collectAsStateWithLifecycle()
    val selectedTrackIds by viewModel.selectedTrackIds.collectAsStateWithLifecycle()
    val canUndoEq by viewModel.canUndoEq.collectAsStateWithLifecycle()
    val mixerCheckResult by viewModel.mixerCheckResult.collectAsStateWithLifecycle()
    val qualityTests by viewModel.qualityTests.collectAsStateWithLifecycle()
    val isRunningTests by viewModel.isRunningTests.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    // SAF Music Folder Picker
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            viewModel.onFolderSelected(uri)
        }
    }

    // Android 13+ Playback Notification Permission (graceful)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(routingState.lastDisconnectEvent) {
        routingState.lastDisconnectEvent?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDisconnectBanner()
        }
    }

    LaunchedEffect(playbackState.errorMessage) {
        playbackState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPlaybackError()
        }
    }

    SonicFlowTheme(
        themeStyle = preferences.themeStyle,
        accentOption = preferences.accentColor
    ) {
        if (!preferences.hasCompletedOnboarding) {
            OnboardingScreen(
                trackCount = allTracks.size,
                folderCount = scanFolders.size,
                scanProgress = scanProgress,
                selectedOutputProfile = preferences.outputProfile,
                onPickFolder = { folderPickerLauncher.launch(null) },
                onSelectOutputProfile = viewModel::setOutputProfile,
                onComplete = viewModel::completeOnboarding
            )
            return@SonicFlowTheme
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val currentRoute = currentDestination?.route
        val showBottomBar = currentRoute != SonicFlowRoutes.TEST_LAB && currentRoute != SonicFlowRoutes.QUALITY_TESTS
        val showMiniPlayer = showBottomBar &&
            currentRoute != SonicFlowRoutes.PLAYER &&
            playbackState.currentTrack != null

        fun navigateTopLevel(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        val accent = MaterialTheme.colorScheme.primary
        val accentContainer = MaterialTheme.colorScheme.primaryContainer
        val surface = MaterialTheme.colorScheme.surface
        val bg = MaterialTheme.colorScheme.background

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 720.dp

            Scaffold(
                containerColor = bg,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!useRail && showBottomBar) {
                        Column {
                            if (showMiniPlayer && playbackState.currentTrack != null) {
                                MiniPlayerBar(
                                    track = playbackState.currentTrack!!,
                                    isPlaying = playbackState.isPlaying,
                                    artworkEnabled = preferences.albumArtworkEnabled,
                                    onTogglePlayPause = viewModel::togglePlayPause,
                                    onNext = viewModel::seekToNext,
                                    onClick = { navigateTopLevel(SonicFlowRoutes.PLAYER) }
                                )
                            }
                            NavigationBar(
                                containerColor = surface,
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                bottomNavItems.forEach { item ->
                                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTopLevel(item.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = { Text(item.label) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = accent,
                                            selectedTextColor = accent,
                                            indicatorColor = accentContainer,
                                            unselectedIconColor = MutedSteel,
                                            unselectedTextColor = MutedSteel
                                        ),
                                        modifier = Modifier.testTag("nav_${item.route}")
                                    )
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && showBottomBar) {
                        NavigationRail(containerColor = surface) {
                            bottomNavItems.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = accent,
                                        selectedTextColor = accent,
                                        indicatorColor = accentContainer,
                                        unselectedIconColor = MutedSteel,
                                        unselectedTextColor = MutedSteel
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        NavHost(
                            navController = navController,
                            startDestination = SonicFlowRoutes.HOME,
                            modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()
                        ) {
                            composable(SonicFlowRoutes.HOME) {
                                HomeScreen(
                                    allTracks = allTracks,
                                    recentTracks = recentTracks,
                                    recentlyAdded = recentlyAdded,
                                    favoriteTracks = favoriteTracks,
                                    playbackState = playbackState,
                                    routingState = routingState,
                                    preferences = preferences,
                                    onPickFolder = { folderPickerLauncher.launch(null) },
                                    onPlayTrackFromList = { list, idx ->
                                        viewModel.playTracks(list, idx)
                                        navigateTopLevel(SonicFlowRoutes.PLAYER)
                                    },
                                    onShuffleAll = { list ->
                                        viewModel.shuffleAndPlay(list)
                                        navigateTopLevel(SonicFlowRoutes.PLAYER)
                                    },
                                    onTogglePlayPause = viewModel::togglePlayPause,
                                    onJumpToQueueIndex = viewModel::jumpToQueueIndex,
                                    onSelectSimpleEqPreset = viewModel::selectSimpleEqPreset,
                                    onOpenLibrary = { navigateTopLevel(SonicFlowRoutes.LIBRARY) },
                                    onOpenPlayer = { navigateTopLevel(SonicFlowRoutes.PLAYER) },
                                    onOpenEqualizer = { navigateTopLevel(SonicFlowRoutes.EQUALIZER) }
                                )
                            }

                            composable(SonicFlowRoutes.LIBRARY) {
                                LibraryScreen(
                                    allTracks = allTracks,
                                    filteredTracks = filteredTracks,
                                    playlists = playlists,
                                    scanFolders = scanFolders,
                                    scanProgress = scanProgress,
                                    searchQuery = searchQuery,
                                    selectedTab = selectedLibraryTab,
                                    formatFilter = formatFilter,
                                    sortOrder = preferences.librarySortOrder,
                                    selectedTrackIds = selectedTrackIds,
                                    currentPlayingUri = playbackState.currentTrack?.uri,
                                    artworkEnabled = preferences.albumArtworkEnabled,
                                    onSearchQueryChange = viewModel::setSearchQuery,
                                    onSelectTab = viewModel::setLibraryTab,
                                    onSelectFormatFilter = viewModel::setFormatFilter,
                                    onSelectSortOrder = viewModel::setLibrarySortOrder,
                                    onToggleTrackSelection = viewModel::toggleTrackSelection,
                                    onSelectAllTracks = viewModel::selectAllVisibleTracks,
                                    onClearTrackSelection = viewModel::clearTrackSelection,
                                    onAddSelectedToQueue = viewModel::addSelectedToQueue,
                                    onFavoriteSelectedTracks = viewModel::favoriteSelectedTracks,
                                    onAddSelectedToPlaylist = viewModel::addSelectedToPlaylist,
                                    onPickFolder = { folderPickerLauncher.launch(null) },
                                    onRescanFolders = viewModel::rescanFolders,
                                    onRemoveFolder = viewModel::removeFolder,
                                    onPlayTracks = { list, idx ->
                                        viewModel.playTracks(list, idx)
                                        navigateTopLevel(SonicFlowRoutes.PLAYER)
                                    },
                                    onShuffleTracks = { list ->
                                        viewModel.shuffleAndPlay(list)
                                        navigateTopLevel(SonicFlowRoutes.PLAYER)
                                    },
                                    onPlayNext = viewModel::playNext,
                                    onAddToQueue = viewModel::addToQueue,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onCreatePlaylist = viewModel::createPlaylist,
                                    onDeletePlaylist = viewModel::deletePlaylist,
                                    onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                    observePlaylistTracks = viewModel::observePlaylistTracks
                                )
                            }

                            composable(SonicFlowRoutes.PLAYER) {
                                PlayerScreen(
                                    playbackState = playbackState,
                                    routingState = routingState,
                                    visualizerFrame = visualizerFrame,
                                    visualizerEnabled = preferences.visualizerEnabled,
                                    artworkEnabled = preferences.albumArtworkEnabled,
                                    artworkStyle = preferences.artworkStyle,
                                    playerLayout = preferences.playerLayout,
                                    gesturesEnabled = preferences.gesturesEnabled,
                                    bitTransparentRequested = preferences.dspSettings.bitTransparentMode,
                                    outputProfile = preferences.outputProfile,
                                    playlists = playlists,
                                    onTogglePlayPause = viewModel::togglePlayPause,
                                    onSeekTo = viewModel::seekTo,
                                    onNext = viewModel::seekToNext,
                                    onPrevious = viewModel::seekToPrevious,
                                    onToggleShuffle = viewModel::toggleShuffle,
                                    onCycleRepeat = viewModel::cycleRepeatMode,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onSetPlaybackSpeed = viewModel::setPlaybackSpeed,
                                    onSelectOutputProfile = viewModel::setOutputProfile,
                                    onJumpToQueueIndex = viewModel::jumpToQueueIndex,
                                    onRemoveFromQueue = viewModel::removeFromQueue,
                                    onShuffleQueue = viewModel::shuffleCurrentQueue,
                                    onClearQueue = viewModel::clearQueue,
                                    onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                    onSetSleepTimerMinutes = viewModel::setSleepTimerMinutes,
                                    onToggleBitTransparent = viewModel::setBitTransparentMode,
                                    onClearError = viewModel::clearPlaybackError,
                                    onGoToLibrary = { navigateTopLevel(SonicFlowRoutes.LIBRARY) },
                                    onGoToEqualizer = { navigateTopLevel(SonicFlowRoutes.EQUALIZER) }
                                )
                            }

                            composable(SonicFlowRoutes.EQUALIZER) {
                                EqualizerScreen(
                                    dspSettings = preferences.dspSettings,
                                    eqUiMode = preferences.eqUiMode,
                                    simplePreset = preferences.simpleEqPreset,
                                    simpleIntensity = preferences.simpleEqIntensity,
                                    rememberEqPerDevice = preferences.rememberEqPerDevice,
                                    activeRouteCategory = routingState.activeDevice?.routeCategory ?: OutputRouteCategory.BUILT_IN_SPEAKERS,
                                    canUndoEq = canUndoEq,
                                    telemetry = dspTelemetry,
                                    presets = eqPresets,
                                    sampleRateHz = playbackState.audioInfo.decodedSampleRateHz.takeIf { it > 0 } ?: 48000,
                                    onSetEqUiMode = viewModel::setEqUiMode,
                                    onSelectSimplePreset = viewModel::selectSimpleEqPreset,
                                    onSetSimpleIntensity = viewModel::setSimpleEqIntensity,
                                    onUndoEq = viewModel::undoEqChange,
                                    onResetEq = viewModel::resetEqToNeutral,
                                    onSetRememberEqPerDevice = viewModel::setRememberEqPerDevice,
                                    onSetBitTransparent = viewModel::setBitTransparentMode,
                                    onSetEqEnabled = viewModel::setEqEnabled,
                                    onToggleBypassBeforeAfter = viewModel::toggleEqBypassBeforeAfter,
                                    onSetAutoHeadroom = viewModel::setAutoHeadroom,
                                    onSetPreampDb = viewModel::setPreampDb,
                                    onUpdateBand = viewModel::updateEqBand,
                                    onAddBand = viewModel::addEqBand,
                                    onRemoveLastBand = viewModel::removeLastEqBand,
                                    onSelectPreset = viewModel::selectEqPreset,
                                    onSaveCustomPreset = viewModel::saveCustomPreset,
                                    onDeleteCustomPreset = viewModel::deleteCustomPreset,
                                    onExportPresetJson = viewModel::exportCurrentPresetJson,
                                    onImportPresetJson = viewModel::importPresetJson,
                                    onSetCrossfeed = viewModel::setCrossfeed,
                                    onSetReplayGain = viewModel::setReplayGain,
                                    onSetDigitalGain = viewModel::setDigitalGainDb,
                                    onSetLimiter = viewModel::setLimiter,
                                    onSetStereoControls = viewModel::setStereoControls,
                                    onResetClipDetector = viewModel::resetClipDetector
                                )
                            }

                            composable(SonicFlowRoutes.SETTINGS) {
                                SettingsScreen(
                                    preferences = preferences,
                                    routingState = routingState,
                                    mixerCheckResult = mixerCheckResult,
                                    onSetOutputProfile = viewModel::setOutputProfile,
                                    onRefreshDevices = viewModel::refreshAudioDevices,
                                    onVerifyBitPerfectMixer = viewModel::verifyBitPerfectMixer,
                                    onSetRememberEqPerDevice = viewModel::setRememberEqPerDevice,
                                    onSetGapless = viewModel::setGapless,
                                    onSetFadeInOut = viewModel::setFadeInOut,
                                    onSetCrossfadeSeconds = viewModel::setCrossfadeSeconds,
                                    onSetPlaybackSpeed = viewModel::setPlaybackSpeed,
                                    onSetAutoPlayOnSelect = viewModel::setAutoPlayOnSelect,
                                    onSetAudioFocus = viewModel::setAudioFocus,
                                    onSetResumeOnRestart = viewModel::setResumeOnRestart,
                                    onSetVisualizerEnabled = viewModel::setVisualizerEnabled,
                                    onSetArtworkEnabled = viewModel::setArtworkEnabled,
                                    onSetNotificationEnabled = viewModel::setNotificationEnabled,
                                    onSetBufferPreset = viewModel::setBufferPreset,
                                    onSetPreferDirectRate = viewModel::setPreferDirectRate,
                                    onSetThemeStyle = viewModel::setThemeStyle,
                                    onSetAccentColor = viewModel::setAccentColor,
                                    onSetArtworkStyle = viewModel::setArtworkStyle,
                                    onSetPlayerLayout = viewModel::setPlayerLayout,
                                    onSetGesturesEnabled = viewModel::setGesturesEnabled,
                                    onResetAllToDefault = viewModel::resetAllSettingsToDefault,
                                    onOpenTestLab = { navController.navigate(SonicFlowRoutes.TEST_LAB) },
                                    onOpenQualityDiagnostics = {
                                        viewModel.runQualityDiagnostics()
                                        navController.navigate(SonicFlowRoutes.QUALITY_TESTS)
                                    }
                                )
                            }

                            composable(SonicFlowRoutes.TEST_LAB) {
                                AudioTestLabScreen(
                                    toneState = toneLabState,
                                    routingState = routingState,
                                    onStartTone = viewModel::startTestTone,
                                    onStopTone = viewModel::stopTestTone,
                                    onSetSineFrequency = viewModel::setSineFrequency,
                                    onSetOutputLevelDb = viewModel::setToneLevelDb,
                                    onSetChannelBalance = viewModel::setToneBalance,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(SonicFlowRoutes.QUALITY_TESTS) {
                                AudioQualityTestingScreen(
                                    tests = qualityTests,
                                    isRunning = isRunningTests,
                                    onRunDiagnostics = viewModel::runQualityDiagnostics,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
