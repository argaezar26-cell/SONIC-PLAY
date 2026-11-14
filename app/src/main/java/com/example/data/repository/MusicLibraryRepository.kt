package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.example.audio.dsp.EqPresets
import com.example.data.db.EqPresetEntity
import com.example.data.db.PlaylistEntity
import com.example.data.db.PlaylistTrackCrossRef
import com.example.data.db.ScanFolderEntity
import com.example.data.db.SonicFlowDao
import com.example.data.db.TrackEntity
import com.example.data.library.AudioMetadataExtractor
import com.example.data.model.EqBand
import com.example.data.model.EqPreset
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class ScanProgress(
    val isScanning: Boolean = false,
    val scannedCount: Int = 0,
    val currentFile: String = "",
    val lastError: String? = null
)

class MusicLibraryRepository(
    private val context: Context,
    private val dao: SonicFlowDao
) {
    private val extractor = AudioMetadataExtractor(context)

    private val _scanProgress = MutableStateFlow(ScanProgress())
    val scanProgress: StateFlow<ScanProgress> = _scanProgress.asStateFlow()

    val allTracks: Flow<List<Track>> = dao.observeAllTracks().map { list -> list.map { it.toDomain() } }
    val favoriteTracks: Flow<List<Track>> = dao.observeFavoriteTracks().map { list -> list.map { it.toDomain() } }
    val recentlyPlayed: Flow<List<Track>> = dao.observeRecentlyPlayed().map { list -> list.map { it.toDomain() } }
    val recentlyAdded: Flow<List<Track>> = dao.observeRecentlyAdded().map { list -> list.map { it.toDomain() } }
    val playlists: Flow<List<PlaylistEntity>> = dao.observePlaylists()
    val scanFolders: Flow<List<ScanFolderEntity>> = dao.observeScanFolders()

    val eqPresets: Flow<List<EqPreset>> = dao.observeEqPresets().map { entities ->
        entities.map { entity ->
            EqPreset(
                id = entity.id,
                name = entity.name,
                isBuiltIn = entity.isBuiltIn,
                preampDb = entity.preampDb,
                bands = EqPresets.bandsFromJson(entity.bandsJson)
            )
        }
    }

    suspend fun ensureBuiltInPresetsSeeded() = withContext(Dispatchers.IO) {
        if (dao.countEqPresets() == 0) {
            val entities = EqPresets.builtInPresets.map { preset ->
                EqPresetEntity(
                    id = preset.id,
                    name = preset.name,
                    isBuiltIn = true,
                    preampDb = preset.preampDb,
                    bandsJson = EqPresets.bandsToJson(preset.bands)
                )
            }
            dao.insertEqPresets(entities)
        }
    }

    suspend fun saveCustomEqPreset(name: String, preampDb: Float, bands: List<EqBand>): Long = withContext(Dispatchers.IO) {
        dao.upsertEqPreset(
            EqPresetEntity(
                id = 0L,
                name = name.trim().ifBlank { "Custom Preset" },
                isBuiltIn = false,
                preampDb = preampDb,
                bandsJson = EqPresets.bandsToJson(bands)
            )
        )
    }

    suspend fun deleteCustomEqPreset(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteCustomEqPreset(id)
    }

    fun observePlaylistTracks(playlistId: Long): Flow<List<Track>> {
        return dao.observeTracksInPlaylist(playlistId).map { list -> list.map { it.toDomain() } }
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        dao.insertPlaylist(PlaylistEntity(name = name.trim().ifBlank { "Untitled Playlist" }))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        val pos = dao.getNextPlaylistPosition(playlistId)
        dao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId, position = pos))
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        dao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun toggleFavorite(trackId: Long, currentFavorite: Boolean) = withContext(Dispatchers.IO) {
        dao.setFavorite(trackId, !currentFavorite)
    }

    suspend fun markTrackPlayed(uri: String) = withContext(Dispatchers.IO) {
        dao.markTrackPlayed(uri, System.currentTimeMillis())
    }

    suspend fun getAllTracksSnapshot(): List<Track> = withContext(Dispatchers.IO) {
        dao.getAllTracksSnapshot().map { it.toDomain() }
    }

    suspend fun addAndScanFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        } catch (_: SecurityException) {
            // Permission may already be held or temporary
        }
        scanFolder(treeUri)
    }

    suspend fun removeFolder(treeUri: String) = withContext(Dispatchers.IO) {
        dao.deleteTracksByFolder(treeUri)
        dao.deleteScanFolder(treeUri)
    }

    suspend fun rescanAllFolders(folders: List<ScanFolderEntity>): Int = withContext(Dispatchers.IO) {
        var total = 0
        for (folder in folders) {
            total += scanFolder(Uri.parse(folder.treeUri))
        }
        total
    }

    suspend fun scanFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        _scanProgress.value = ScanProgress(isScanning = true, scannedCount = 0, currentFile = "Initializing SAF scan...")
        val folderKey = treeUri.toString()
        val folderLabel = DocumentsContract.getTreeDocumentId(treeUri)
            ?.substringAfterLast(':')
            ?.substringAfterLast('/')
            ?.ifBlank { "Music Folder" }
            ?: "Music Folder"

        val foundTracks = mutableListOf<TrackEntity>()
        val existingByUri = dao.getAllTracksSnapshot().associateBy { it.uri }

        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val queue = ArrayDeque<String>()
            queue.add(rootDocId)

            val supportedExtensions = setOf("flac", "wav", "mp3", "m4a", "aac", "ogg", "oga", "opus", "alac")

            while (queue.isNotEmpty()) {
                val parentDocId = queue.removeFirst()
                val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
                val projection = arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                )

                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIdx = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(idIdx)
                        val name = cursor.getString(nameIdx) ?: continue
                        val mime = cursor.getString(mimeIdx) ?: ""
                        val size = if (!cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            queue.add(docId)
                            continue
                        }

                        val ext = name.substringAfterLast('.', "").lowercase()
                        val isAudio = mime.startsWith("audio/") || ext in supportedExtensions
                        if (!isAudio) continue

                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        val docUriStr = docUri.toString()
                        _scanProgress.value = _scanProgress.value.copy(
                            scannedCount = foundTracks.size,
                            currentFile = name
                        )

                        val existing = existingByUri[docUriStr]
                        if (existing != null && existing.fileSizeBytes == size && existing.sampleRateHz > 0) {
                            foundTracks.add(existing)
                        } else {
                            val parsed = extractor.extract(docUri, name, mime, size)
                            foundTracks.add(
                                TrackEntity(
                                    id = existing?.id ?: 0L,
                                    uri = docUriStr,
                                    title = parsed.title,
                                    artist = parsed.artist,
                                    album = parsed.album,
                                    albumArtist = parsed.albumArtist,
                                    genre = parsed.genre,
                                    year = parsed.year,
                                    trackNumber = parsed.trackNumber,
                                    durationMs = parsed.durationMs,
                                    fileSizeBytes = parsed.fileSizeBytes,
                                    mimeType = parsed.mimeType,
                                    codec = parsed.codec,
                                    sampleRateHz = parsed.sampleRateHz,
                                    bitDepth = parsed.bitDepth,
                                    bitrateKbps = parsed.bitrateKbps,
                                    channels = parsed.channels,
                                    folderUri = folderKey,
                                    folderName = folderLabel,
                                    dateAdded = existing?.dateAdded ?: System.currentTimeMillis(),
                                    lastPlayedAt = existing?.lastPlayedAt ?: 0L,
                                    playCount = existing?.playCount ?: 0,
                                    isFavorite = existing?.isFavorite ?: false,
                                    replayGainTrackDb = parsed.replayGainTrackDb,
                                    replayGainAlbumDb = parsed.replayGainAlbumDb,
                                    artworkCachePath = parsed.artworkCachePath
                                )
                            )
                        }
                    }
                }
            }

            if (foundTracks.isNotEmpty()) {
                dao.upsertTracks(foundTracks)
                dao.deleteRemovedTracksInFolder(folderKey, foundTracks.map { it.uri })
            } else {
                dao.deleteTracksByFolder(folderKey)
            }
            dao.upsertScanFolder(
                ScanFolderEntity(
                    treeUri = folderKey,
                    displayName = folderLabel,
                    lastScannedAt = System.currentTimeMillis(),
                    trackCount = foundTracks.size
                )
            )
            _scanProgress.value = ScanProgress(isScanning = false, scannedCount = foundTracks.size, currentFile = "")
            foundTracks.size
        } catch (e: Exception) {
            _scanProgress.value = ScanProgress(
                isScanning = false,
                scannedCount = foundTracks.size,
                currentFile = "",
                lastError = e.localizedMessage ?: "Failed to scan SAF folder"
            )
            0
        }
    }

    private fun TrackEntity.toDomain() = Track(
        id = id,
        uri = uri,
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        genre = genre,
        year = year,
        trackNumber = trackNumber,
        durationMs = durationMs,
        fileSizeBytes = fileSizeBytes,
        mimeType = mimeType,
        codec = codec,
        sampleRateHz = sampleRateHz,
        bitDepth = bitDepth,
        bitrateKbps = bitrateKbps,
        channels = channels,
        folderUri = folderUri,
        folderName = folderName,
        dateAdded = dateAdded,
        lastPlayedAt = lastPlayedAt,
        playCount = playCount,
        isFavorite = isFavorite,
        replayGainTrackDb = replayGainTrackDb,
        replayGainAlbumDb = replayGainAlbumDb,
        artworkCachePath = artworkCachePath
    )
}
