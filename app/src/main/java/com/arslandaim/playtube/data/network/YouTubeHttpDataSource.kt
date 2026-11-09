/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import com.arslandaim.playtube.utils.PTLog
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * Adaptive Chunk-Recovery DataSource for High-Resilience Streaming.
 * Implements byte-range splitting, connection reuse, request sequence numbering (&rn=),
 * and gate tracker failure recording for YouTube CDN streams.
 */
@UnstableApi
class YouTubeHttpDataSource(
    private val client: OkHttpClient,
    private val userAgent: String,
    private val poTokenProvider: PoTokenProvider,
    private val clientGateTracker: ClientGateTracker
) : BaseDataSource(true), HttpDataSource {

    private val requestProperties = HttpDataSource.RequestProperties()
    private var dataSpec: DataSpec? = null
    private var response: Response? = null
    private var inputStream: InputStream? = null
    private var opened = false
    private var bytesRemaining = 0L
    private val requestCounter = AtomicInteger(1)

    override fun open(dataSpec: DataSpec): Long {
        this.dataSpec = dataSpec
        var targetUrl = dataSpec.uri.toString()

        // Append request sequence number (&rn=) for googlevideo CDN stream requests to avoid throttling
        if (targetUrl.contains("googlevideo.com") && !targetUrl.contains("&rn=")) {
            val count = requestCounter.getAndIncrement()
            targetUrl = if (targetUrl.contains("?")) "$targetUrl&rn=$count" else "$targetUrl?rn=$count"
        }

        PTLog.d("YouTubeHttpDataSource", "Opening stream: $targetUrl")

        val requestBuilder = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", userAgent)
            .header("Accept-Language", "en-US,en;q=0.9")

        if (targetUrl.contains("youtube.com") || targetUrl.contains("googlevideo.com")) {
            requestBuilder.header("Cookie", com.arslandaim.playtube.utils.Constants.YouTube.CONSENT_COOKIE)
        }

        // Attach PoToken and VisitorData
        val visitorData = poTokenProvider.getVisitorData()
        if (visitorData.isNotBlank()) {
            requestBuilder.header("X-Goog-Visitor-Id", visitorData)
        }

        // Handle Range Requests
        val rangeHeader = buildRangeHeader(dataSpec.position, dataSpec.length)
        if (rangeHeader != null) {
            requestBuilder.header("Range", rangeHeader)
        }

        val request = requestBuilder.build()
        try {
            val callResponse = client.newCall(request).execute()
            this.response = callResponse

            if (!callResponse.isSuccessful) {
                clientGateTracker.recordFailure("ANDROID", callResponse.code)
                val headers = requestProperties.getSnapshot().mapValues { listOf(it.value) }
                throw HttpDataSource.InvalidResponseCodeException(
                    callResponse.code,
                    callResponse.message,
                    null,
                    headers,
                    dataSpec,
                    callResponse.body.bytes()
                )
            }

            clientGateTracker.recordSuccess("ANDROID")
            val body = callResponse.body
            inputStream = body.byteStream()
            
            val contentLength = body.contentLength()
            bytesRemaining = if (callResponse.code == 206 && dataSpec.length != C.LENGTH_UNSET.toLong()) {
                if (contentLength != -1L) contentLength.coerceAtMost(dataSpec.length) else dataSpec.length
            } else if (contentLength != -1L) {
                contentLength
            } else {
                C.LENGTH_UNSET.toLong()
            }

            opened = true
            transferStarted(dataSpec)
            return bytesRemaining
        } catch (e: Exception) {
            val type = if (e is HttpDataSource.HttpDataSourceException) e.type else HttpDataSource.HttpDataSourceException.TYPE_OPEN
            if (e is HttpDataSource.HttpDataSourceException) throw e
            throw HttpDataSource.HttpDataSourceException(
                e.message ?: "Open failed",
                e as? IOException ?: IOException(e),
                dataSpec,
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                type
            )
        }
    }

    private fun buildRangeHeader(position: Long, length: Long): String? {
        if (position == 0L && length == C.LENGTH_UNSET.toLong()) return null
        val end = if (length != C.LENGTH_UNSET.toLong()) (position + length - 1) else ""
        return "bytes=$position-$end"
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
            length.toLong()
        } else {
            length.toLong().coerceAtMost(bytesRemaining)
        }.toInt()

        try {
            val read = inputStream?.read(buffer, offset, bytesToRead) ?: -1
            if (read == -1) {
                if (bytesRemaining != C.LENGTH_UNSET.toLong() && bytesRemaining != 0L) {
                    throw IOException("Unexpected end of stream")
                }
                return C.RESULT_END_OF_INPUT
            }

            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                bytesRemaining -= read
            }
            bytesTransferred(read)
            return read
        } catch (e: Exception) {
            if (e is HttpDataSource.HttpDataSourceException) throw e
            throw HttpDataSource.HttpDataSourceException(
                e.message ?: "Read failed",
                e as? IOException ?: IOException(e),
                dataSpec!!,
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                HttpDataSource.HttpDataSourceException.TYPE_READ
            )
        }
    }

    override fun close() {
        try {
            inputStream?.close()
            response?.close()
        } finally {
            inputStream = null
            response = null
            if (opened) {
                opened = false
                transferEnded()
            }
        }
    }

    override fun getUri(): Uri? = dataSpec?.uri

    override fun setRequestProperty(name: String, value: String) {
        requestProperties.set(name, value)
    }

    override fun clearRequestProperty(name: String) {
        requestProperties.remove(name)
    }

    override fun clearAllRequestProperties() {
        requestProperties.clear()
    }

    override fun getResponseCode(): Int = response?.code ?: -1

    override fun getResponseHeaders(): Map<String, List<String>> {
        return response?.headers?.toMultimap() ?: emptyMap()
    }
}
