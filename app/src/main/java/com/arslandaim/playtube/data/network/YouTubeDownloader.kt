/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import com.arslandaim.playtube.utils.Constants
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException

class YouTubeDownloader(private val client: OkHttpClient) : Downloader() {

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val url = request.url()
        val method = request.httpMethod()
        val headers = request.headers()
        val data = request.dataToSend()

        val okHttpRequestBuilder = OkHttpRequest.Builder()
            .url(url)
            .method(method, data?.toRequestBody())

        val mergedHeaders = headers.toMutableMap()

        if (mergedHeaders.keys.none { it.equals("Accept-Language", ignoreCase = true) }) {
            mergedHeaders["Accept-Language"] = listOf("en-US,en;q=0.9")
        }

        if (mergedHeaders.keys.none { it.equals("User-Agent", ignoreCase = true) }) {
            mergedHeaders["User-Agent"] = listOf(Constants.DEFAULT_USER_AGENT)
        }

        // Bypass YouTube Consent/GDPR redirection in Europe
        // Merge with existing cookies if present
        if (url.contains("youtube.com") || url.contains("googlevideo.com")) {
            val existingCookies = mergedHeaders["Cookie"] ?: emptyList()
            if (existingCookies.none { it.contains("CONSENT=YES") || it.contains("SOCS=") }) {
                val newCookies = existingCookies.toMutableList().apply { 
                    add(Constants.YouTube.CONSENT_COOKIE) 
                }
                mergedHeaders["Cookie"] = newCookies
            }
        }

        mergedHeaders.forEach { (key, values) ->
            values.forEach { value ->
                okHttpRequestBuilder.addHeader(key, value)
            }
        }

        val maxRetries = 2
        var attempt = 0
        var lastResponse: okhttp3.Response? = null
        var lastException: IOException? = null

        while (attempt <= maxRetries) {
            try {
                val okHttpResponse = client.newCall(okHttpRequestBuilder.build()).execute()
                
                // If it's a 403 or 429, retry with short backoff; on final attempt return response so NewPipe can handle fallback
                if ((okHttpResponse.code == 403 || okHttpResponse.code == 429) && attempt < maxRetries) {
                    okHttpResponse.close()
                    attempt++
                    Thread.sleep(250L * attempt)
                    continue
                }
                
                lastResponse = okHttpResponse
                break
            } catch (e: IOException) {
                lastException = e
                attempt++
                if (attempt <= maxRetries) {
                    Thread.sleep(250L * attempt)
                }
            }
        }

        val response = lastResponse ?: throw lastException ?: IOException("Request failed after $maxRetries retries")

        val responseBody = response.body?.string()
        val responseCode = response.code
        val responseMessage = response.message
        val responseHeaders = response.headers.toMultimap()

        return Response(responseCode, responseMessage, responseHeaders, responseBody, url)
    }
}
