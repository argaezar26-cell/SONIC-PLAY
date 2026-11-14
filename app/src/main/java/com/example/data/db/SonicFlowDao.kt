package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SonicFlowDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT 50")
    fun observeRecentlyPlayed(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC LIMIT 50")
    fun observeRecentlyAdded(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE uri = :uri LIMIT 1")
    suspend fun getTrackByUri(uri: String): TrackEntity?

    @Query("SELECT * FROM tracks")
    suspend fun getAllTracksSnapshot(): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Query("UPDATE tracks SET isFavorite = :favorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: Long, favorite: Boolean)

    @Query("UPDATE tracks SET lastPlayedAt = :timestamp, playCount = playCount + 1 WHERE uri = :uri")
    suspend fun markTrackPlayed(uri: String, timestamp: Long)

    @Query("DELETE FROM tracks WHERE folderUri = :folderUri AND uri NOT IN (:activeUris)")
    suspend fun deleteRemovedTracksInFolder(folderUri: String, activeUris: List<String>)

    @Query("DELETE FROM tracks WHERE folderUri = :folderUri")
    suspend fun deleteTracksByFolder(folderUri: String)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrackToPlaylist(ref: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query(
        """
        SELECT t.* FROM tracks t
        INNER JOIN playlist_tracks pt ON t.id = pt.trackId
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.position ASC
        """
    )
    fun observeTracksInPlaylist(playlistId: Long): Flow<List<TrackEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getNextPlaylistPosition(playlistId: Long): Int

    // EQ Presets
    @Query("SELECT * FROM eq_presets ORDER BY isBuiltIn DESC, id ASC")
    fun observeEqPresets(): Flow<List<EqPresetEntity>>

    @Query("SELECT COUNT(*) FROM eq_presets")
    suspend fun countEqPresets(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEqPresets(presets: List<EqPresetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEqPreset(preset: EqPresetEntity): Long

    @Update
    suspend fun updateEqPreset(preset: EqPresetEntity)

    @Query("DELETE FROM eq_presets WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomEqPreset(id: Long)

    // Scan Folders
    @Query("SELECT * FROM scan_folders ORDER BY lastScannedAt DESC")
    fun observeScanFolders(): Flow<List<ScanFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScanFolder(folder: ScanFolderEntity)

    @Query("DELETE FROM scan_folders WHERE treeUri = :treeUri")
    suspend fun deleteScanFolder(treeUri: String)
}
