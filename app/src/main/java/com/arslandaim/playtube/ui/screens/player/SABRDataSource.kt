/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.exoplayer.upstream.BandwidthMeter
import com.arslandaim.playtube.utils.PTLog
import java.io.IOException

/**
 * High-performance Chunked Adaptive Streaming DataSource (SABR Proxy).
 * Intercepts requests and loads media in optimal bounded byte-range chunks.
 * Bypasses YouTube's CDN bandwidth throttling on video streams for instant playback.
 */
@UnstableApi
class SABRDataSource(
    private val upstream: DataSource,
    private val bandwidthMeter: BandwidthMeter
) : DataSource {

    companion object {
        private const val CHUNK_SIZE = 2 * 1024 * 1024L // 2 MB bounded chunk
        private const val MAX_RETRIES = 2
    }

    private var originalDataSpec: DataSpec? = null
    private var isChunked = false
    private var currentPosition = 0L
    private var totalLength = C.LENGTH_UNSET.toLong()
    private var bytesRemainingInOriginal = C.LENGTH_UNSET.toLong()
    private var currentChunkRemaining = 0L
    private var reachedEof = false
    private var opened = false

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        originalDataSpec = dataSpec
        currentPosition = dataSpec.position
        totalLength = dataSpec.length
        bytesRemainingInOriginal = dataSpec.length
        reachedEof = false

        val scheme = dataSpec.uri.scheme
        val host = dataSpec.uri.host ?: ""
        val isNetworkStream = (scheme == "http" || scheme == "https") && host.contains("googlevideo.com")

        // Enable chunking for googlevideo network streams when requested length is open-ended or larger than CHUNK_SIZE
        isChunked = isNetworkStream && (dataSpec.length == C.LENGTH_UNSET.toLong() || dataSpec.length > CHUNK_SIZE)

        opened = true

        return if (isChunked) {
            openNextChunkWithRetry()
            if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else C.LENGTH_UNSET.toLong()
        } else {
            try {
                val bytes = upstream.open(dataSpec)
                PTLog.d("SABRDataSource", "Opened direct stream at ${dataSpec.position}, bytes=$bytes")
                bytes
            } catch (e: Exception) {
                PTLog.e("SABRDataSource", "Failed to open direct stream at ${dataSpec.position}", e)
                throw e
            }
        }
    }

    private fun openNextChunkWithRetry() {
        val original = originalDataSpec ?: return
        var attempts = 0
        var lastException: Exception? = null

        val requestLength = if (bytesRemainingInOriginal != C.LENGTH_UNSET.toLong()) {
            CHUNK_SIZE.coerceAtMost(bytesRemainingInOriginal)
        } else {
            CHUNK_SIZE
        }

        val chunkSpec = original.buildUpon()
            .setPosition(currentPosition)
            .setLength(requestLength)
            .build()

        while (attempts <= MAX_RETRIES) {
            try {
                val chunkBytes = upstream.open(chunkSpec)
                currentChunkRemaining = if (chunkBytes != C.LENGTH_UNSET.toLong()) chunkBytes else requestLength
                if (chunkBytes != C.LENGTH_UNSET.toLong() && chunkBytes < requestLength) {
                    reachedEof = true
                }
                PTLog.d("SABRDataSource", "Opened chunk at pos=$currentPosition, chunkSize=$currentChunkRemaining, eof=$reachedEof")
                return
            } catch (e: Exception) {
                val invalidResp = (e as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)
                    ?: (e.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)
                if (invalidResp?.responseCode == 416) {
                    reachedEof = true
                    currentChunkRemaining = 0L
                    return
                }
                lastException = e
                attempts++
                if (attempts <= MAX_RETRIES) {
                    PTLog.w("SABRDataSource", "Retrying chunk open (attempt $attempts) at pos=$currentPosition")
                    try { upstream.close() } catch (_: Exception) {}
                }
            }
        }
        throw lastException ?: IOException("Failed to open chunk at pos=$currentPosition")
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (!opened) return C.RESULT_END_OF_INPUT
        if (length == 0) return 0

        if (!isChunked) {
            return upstream.read(buffer, offset, length)
        }

        if (bytesRemainingInOriginal == 0L || (reachedEof && currentChunkRemaining == 0L)) {
            return C.RESULT_END_OF_INPUT
        }

        val readLength = if (currentChunkRemaining > 0) {
            length.toLong().coerceAtMost(currentChunkRemaining).toInt()
        } else {
            length
        }

        var bytesRead = upstream.read(buffer, offset, readLength)

        if (bytesRead == C.RESULT_END_OF_INPUT || bytesRead == 0) {
            if (reachedEof) {
                return C.RESULT_END_OF_INPUT
            }
            // End of current chunk - attempt to open next chunk if more bytes are expected
            if (bytesRemainingInOriginal == C.LENGTH_UNSET.toLong() || bytesRemainingInOriginal > 0) {
                try {
                    upstream.close()
                } catch (_: Exception) {}

                try {
                    openNextChunkWithRetry()
                    if (reachedEof && currentChunkRemaining == 0L) {
                        return C.RESULT_END_OF_INPUT
                    }
                    bytesRead = upstream.read(buffer, offset, length)
                } catch (e: Exception) {
                    PTLog.e("SABRDataSource", "Failed to fetch subsequent chunk at pos=$currentPosition", e)
                    return C.RESULT_END_OF_INPUT
                }
            } else {
                return C.RESULT_END_OF_INPUT
            }
        }

        if (bytesRead > 0) {
            currentPosition += bytesRead
            if (currentChunkRemaining != C.LENGTH_UNSET.toLong() && currentChunkRemaining > 0) {
                currentChunkRemaining -= bytesRead
            }
            if (bytesRemainingInOriginal != C.LENGTH_UNSET.toLong()) {
                bytesRemainingInOriginal = (bytesRemainingInOriginal - bytesRead).coerceAtLeast(0L)
            }
        }

        return bytesRead
    }

    override fun getUri(): Uri? = upstream.getUri() ?: originalDataSpec?.uri

    override fun close() {
        if (opened) {
            opened = false
            try {
                upstream.close()
            } catch (e: Exception) {
                PTLog.w("SABRDataSource", "Error closing upstream: ${e.message}")
            }
        }
    }
}

@UnstableApi
class SABRDataSourceFactory(
    private val baseFactory: DataSource.Factory,
    private val bandwidthMeter: BandwidthMeter
) : DataSource.Factory {
    override fun createDataSource(): DataSource {
        return SABRDataSource(baseFactory.createDataSource(), bandwidthMeter)
    }
}
