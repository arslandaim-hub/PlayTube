/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import android.content.Context
import android.media.MediaCodecList
import com.arslandaim.playtube.utils.PTLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StartupWarmups @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val newPipeInitializer: NewPipeInitializer,
    private val poTokenProvider: PoTokenProvider
) {

    fun initiate(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            PTLog.d("StartupWarmups", "Initiating Application Cold-Start Warmups...")

            // 1. HTTP / TLS Pre-warming
            prewarmNetwork()

            // 2. Extractor / Cipher Pre-warming
            prewarmExtractor()

            // 3. MediaCodec Hardware Decoder Warmup
            prewarmCodecs()

            PTLog.d("StartupWarmups", "Application Cold-Start Warmups initiated successfully")
        }
    }

    private fun prewarmNetwork() {
        val targets = listOf(
            "https://www.youtube.com",
            "https://youtubei.googleapis.com",
            "https://www.googlevideo.com"
        )

        for (target in targets) {
            val request = Request.Builder()
                .url(target)
                .head()
                .build()

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    PTLog.w("StartupWarmups", "Pre-warm TLS handshake failed for $target: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    response.close()
                    PTLog.d("StartupWarmups", "Pre-warmed connection pool & TLS for $target")
                }
            })
        }
    }

    private suspend fun prewarmExtractor() {
        try {
            newPipeInitializer.ensureInitialized()
            val visitorData = poTokenProvider.getVisitorData()
            PTLog.d("StartupWarmups", "Extractor & visitorData pre-warmed (visitorData len: ${visitorData.length})")
        } catch (e: Exception) {
            PTLog.w("StartupWarmups", "Extractor pre-warming non-fatal error: ${e.message}")
        }
    }

    private fun prewarmCodecs() {
        try {
            val startTime = System.currentTimeMillis()
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            val infoCount = codecList.codecInfos.size
            val duration = System.currentTimeMillis() - startTime
            PTLog.d("StartupWarmups", "MediaCodec pre-query completed ($infoCount codecs found in ${duration}ms)")
        } catch (e: Exception) {
            PTLog.w("StartupWarmups", "MediaCodec pre-query failed: ${e.message}")
        }
    }
}
