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
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .addHeader("User-Agent", Constants.DEFAULT_USER_AGENT)

        // Bypass YouTube Consent/GDPR redirection in Europe
        if (url.contains("youtube.com") || url.contains("googlevideo.com")) {
            okHttpRequestBuilder.addHeader("Cookie", Constants.YouTube.CONSENT_COOKIE)
        }

        headers.forEach { (key, values) ->
            values.forEach { value ->
                okHttpRequestBuilder.addHeader(key, value)
            }
        }

        val okHttpResponse = client.newCall(okHttpRequestBuilder.build()).execute()

        val responseBody = okHttpResponse.body?.string()
        val responseCode = okHttpResponse.code
        val responseMessage = okHttpResponse.message
        val responseHeaders = okHttpResponse.headers.toMultimap()

        return Response(responseCode, responseMessage, responseHeaders, responseBody, url)
    }
}
