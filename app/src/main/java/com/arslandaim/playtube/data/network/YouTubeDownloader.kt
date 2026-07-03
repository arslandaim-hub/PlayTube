package com.arslandaim.playtube.data.network

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
