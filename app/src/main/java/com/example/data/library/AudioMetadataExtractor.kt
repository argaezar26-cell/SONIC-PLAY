package com.example.data.library

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import com.example.data.db.TrackEntity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

data class ParsedAudioFile(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: Int,
    val trackNumber: Int,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val mimeType: String,
    val codec: String,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val bitrateKbps: Int,
    val channels: Int,
    val replayGainTrackDb: Float?,
    val replayGainAlbumDb: Float?,
    val artworkCachePath: String?,
    val lyrics: String? = null
)

class AudioMetadataExtractor(private val context: Context) {

    private val artworkDir = File(context.cacheDir, "artwork").apply { mkdirs() }

    fun extract(
        uri: Uri,
        displayName: String,
        mimeTypeHint: String,
        fileSizeBytes: Long
    ): ParsedAudioFile {
        val ext = displayName.substringAfterLast('.', "").lowercase()
        var title = displayName.substringBeforeLast('.')
        var artist = "Unknown Artist"
        var album = "Unknown Album"
        var albumArtist = ""
        var genre = "Unknown"
        var year = 0
        var trackNumber = 0
        var durationMs = 0L
        var bitrateKbps = 0
        var sampleRateHz = 0
        var bitDepth = 0
        var channels = 0
        var mimeType = mimeTypeHint
        var artworkPath: String? = null

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() }?.let { title = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }?.let { artist = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }?.let { album = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?.takeIf { it.isNotBlank() }?.let { albumArtist = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.takeIf { it.isNotBlank() }?.let { genre = it.trim() }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.toIntOrNull()?.let { year = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.trim()?.toIntOrNull()?.let { trackNumber = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()?.let { durationMs = it }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                ?.toIntOrNull()?.let { bitrateKbps = it / 1000 }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?.takeIf { it.isNotBlank() }?.let { mimeType = it }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    ?.toIntOrNull()?.let { sampleRateHz = it }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                    ?.toIntOrNull()?.let { bitDepth = it }
            }

            val embeddedPic = retriever.embeddedPicture
            if (embeddedPic != null && embeddedPic.isNotEmpty()) {
                val key = sha1("$album|$artist")
                val outFile = File(artworkDir, "$key.jpg")
                if (!outFile.exists()) {
                    FileOutputStream(outFile).use { it.write(embeddedPic) }
                }
                artworkPath = outFile.absolutePath
            }
        } catch (_: Exception) {
            // Continue with extractor & header probes
        } finally {
            runCatching { retriever.release() }
        }

        // MediaExtractor probe for sample rate, channels, codec mime
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val trackMime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                if (trackMime.startsWith("audio/")) {
                    if (mimeType.isBlank() || mimeType == "application/octet-stream") {
                        mimeType = trackMime
                    }
                    if (sampleRateHz == 0 && fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRateHz = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (channels == 0 && fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (durationMs == 0L && fmt.containsKey(MediaFormat.KEY_DURATION)) {
                        durationMs = fmt.getLong(MediaFormat.KEY_DURATION) / 1000L
                    }
                    break
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { extractor.release() }
        }

        // Direct binary header probe for exact FLAC/WAV bit depth, sample rate, ReplayGain, and Lyrics tags
        var rgTrack: Float? = null
        var rgAlbum: Float? = null
        var lyrics: String? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val header = parseBinaryHeader(stream, ext, mimeType)
                if (header != null) {
                    if (header.sampleRateHz > 0) sampleRateHz = header.sampleRateHz
                    if (header.bitDepth > 0) bitDepth = header.bitDepth
                    if (header.channels > 0) channels = header.channels
                    if (header.durationMs > 0L && durationMs == 0L) durationMs = header.durationMs
                    rgTrack = header.replayGainTrackDb
                    rgAlbum = header.replayGainAlbumDb
                    lyrics = header.lyrics
                }
            }
        } catch (_: Exception) {
        }

        val codec = resolveCodecName(ext, mimeType)
        if (bitrateKbps == 0 && durationMs > 0L && fileSizeBytes > 0L) {
            bitrateKbps = ((fileSizeBytes * 8L) / durationMs).toInt().coerceAtLeast(1)
        }
        if (channels == 0) channels = 2

        return ParsedAudioFile(
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist.ifBlank { artist },
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
            replayGainTrackDb = rgTrack,
            replayGainAlbumDb = rgAlbum,
            artworkCachePath = artworkPath,
            lyrics = lyrics
        )
    }

    private data class BinaryHeaderInfo(
        val sampleRateHz: Int = 0,
        val bitDepth: Int = 0,
        val channels: Int = 0,
        val durationMs: Long = 0L,
        val replayGainTrackDb: Float? = null,
        val replayGainAlbumDb: Float? = null,
        val lyrics: String? = null
    )

    private fun parseBinaryHeader(stream: InputStream, ext: String, mimeType: String): BinaryHeaderInfo? {
        val magic = ByteArray(4)
        if (stream.read(magic) < 4) return null
        val magicStr = String(magic, Charsets.US_ASCII)

        if (magicStr == "fLaC" || ext == "flac" || mimeType.contains("flac")) {
            if (magicStr != "fLaC") return null
            var sr = 0
            var ch = 0
            var bps = 0
            var durMs = 0L
            var rgTrack: Float? = null
            var rgAlbum: Float? = null
            var lyrics: String? = null

            var isLast = false
            var blockLimit = 0
            while (!isLast && blockLimit < 12) {
                blockLimit++
                val header = ByteArray(4)
                if (readFully(stream, header) < 4) break
                val first = header[0].toInt() and 0xFF
                isLast = (first and 0x80) != 0
                val blockType = first and 0x7F
                val length = ((header[1].toInt() and 0xFF) shl 16) or
                    ((header[2].toInt() and 0xFF) shl 8) or
                    (header[3].toInt() and 0xFF)

                if (length <= 0 || length > 262144) {
                    skipBytes(stream, length.toLong().coerceAtLeast(0))
                    continue
                }

                if (blockType == 0 && length >= 18) { // STREAMINFO
                    val data = ByteArray(length)
                    if (readFully(stream, data) < length) break
                    // Bytes 10..17 contain sample rate (20 bits), channels-1 (3 bits), bps-1 (5 bits), totalSamples (36 bits)
                    val b10 = data[10].toLong() and 0xFF
                    val b11 = data[11].toLong() and 0xFF
                    val b12 = data[12].toLong() and 0xFF
                    val b13 = data[13].toLong() and 0xFF
                    val b14 = data[14].toLong() and 0xFF
                    val b15 = data[15].toLong() and 0xFF
                    val b16 = data[16].toLong() and 0xFF
                    val b17 = data[17].toLong() and 0xFF

                    sr = ((b10 shl 12) or (b11 shl 4) or (b12 ushr 4)).toInt()
                    ch = (((b12 ushr 1) and 0x07) + 1).toInt()
                    bps = ((((b12 and 0x01) shl 4) or (b13 ushr 4)) + 1).toInt()
                    val totalSamples = ((b13 and 0x0F) shl 32) or
                        (b14 shl 24) or (b15 shl 16) or (b16 shl 8) or b17
                    if (sr > 0 && totalSamples > 0) {
                        durMs = (totalSamples * 1000L) / sr
                    }
                } else if (blockType == 4 && length <= 131072) { // VORBIS_COMMENT
                    val data = ByteArray(length)
                    if (readFully(stream, data) < length) break
                    val comments = parseVorbisComments(data)
                    rgTrack = comments["REPLAYGAIN_TRACK_GAIN"]?.let(::parseGainDb)
                    rgAlbum = comments["REPLAYGAIN_ALBUM_GAIN"]?.let(::parseGainDb)
                    lyrics = comments["LYRICS"]
                        ?: comments["UNSYNCEDLYRICS"]
                        ?: comments["SYNCEDLYRICS"]
                } else {
                    skipBytes(stream, length.toLong())
                }
            }
            return BinaryHeaderInfo(sr, bps, ch, durMs, rgTrack, rgAlbum, lyrics)
        }

        if (magicStr == "RIFF" || ext == "wav" || mimeType.contains("wav")) {
            if (magicStr != "RIFF") return null
            val rest = ByteArray(40)
            if (readFully(stream, rest) < 40) return null
            val wave = String(rest, 4, 4, Charsets.US_ASCII)
            if (wave != "WAVE") return null
            val buf = ByteBuffer.wrap(rest).order(ByteOrder.LITTLE_ENDIAN)
            // Find fmt chunk starting at offset 8
            var offset = 8
            if (offset + 28 <= rest.size) {
                val chunkId = String(rest, offset, 4, Charsets.US_ASCII)
                if (chunkId == "fmt ") {
                    val ch = buf.getShort(offset + 10).toInt() and 0xFFFF
                    val sr = buf.getInt(offset + 12)
                    val bps = buf.getShort(offset + 22).toInt() and 0xFFFF
                    return BinaryHeaderInfo(sampleRateHz = sr, bitDepth = bps, channels = ch)
                }
            }
        }

        return null
    }

    private fun parseVorbisComments(data: ByteArray): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (data.size < 8) return map
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val vendorLen = buf.int
        if (vendorLen < 0 || vendorLen > data.size - 8) return map
        buf.position(4 + vendorLen)
        if (buf.remaining() < 4) return map
        val count = buf.int.coerceAtMost(256)
        for (i in 0 until count) {
            if (buf.remaining() < 4) break
            val len = buf.int
            if (len < 0 || len > buf.remaining()) break
            val bytes = ByteArray(len)
            buf.get(bytes)
            val entry = String(bytes, Charsets.UTF_8)
            val eq = entry.indexOf('=')
            if (eq > 0) {
                val key = entry.substring(0, eq).uppercase()
                val value = entry.substring(eq + 1).trim()
                map[key] = value
            }
        }
        return map
    }

    private fun parseGainDb(raw: String): Float? {
        return raw.lowercase()
            .replace("db", "")
            .trim()
            .toFloatOrNull()
    }

    private fun readFully(stream: InputStream, dest: ByteArray): Int {
        var total = 0
        while (total < dest.size) {
            val read = stream.read(dest, total, dest.size - total)
            if (read <= 0) break
            total += read
        }
        return total
    }

    private fun skipBytes(stream: InputStream, count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = stream.skip(remaining)
            if (skipped <= 0) {
                if (stream.read() == -1) break
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    private fun resolveCodecName(ext: String, mime: String): String {
        val m = mime.lowercase()
        val e = ext.lowercase()
        return when {
            e == "flac" || m.contains("flac") -> "FLAC"
            e == "wav" || m.contains("wav") || m.contains("x-wav") -> "WAV PCM"
            e == "mp3" || m.contains("mpeg") -> "MP3"
            e == "opus" || m.contains("opus") -> "OPUS"
            e == "ogg" || e == "oga" || m.contains("vorbis") || m.contains("ogg") -> "OGG Vorbis"
            e == "alac" || m.contains("alac") -> "ALAC"
            e == "m4a" || e == "aac" || m.contains("mp4a") || m.contains("aac") -> "AAC / M4A"
            else -> e.uppercase().ifBlank { "PCM" }
        }
    }

    private fun sha1(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
