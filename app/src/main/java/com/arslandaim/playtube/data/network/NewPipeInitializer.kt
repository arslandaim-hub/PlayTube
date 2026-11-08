/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import com.arslandaim.playtube.utils.PTLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPipeInitializer @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val poTokenProvider: PoTokenProvider
) {
    private val initializationDeferred = CompletableDeferred<Unit>()
    private val mutex = Mutex()
    private var isInitialized = false

    suspend fun ensureInitialized() {
        if (isInitialized) return
        
        mutex.withLock {
            if (isInitialized) return
            
            try {
                withContext(Dispatchers.IO) {
                    NewPipe.init(
                        YouTubeDownloader(okHttpClient),
                        Localization.DEFAULT,
                        ContentCountry("US")
                    )

                    val poTokenImpl = object : org.schabi.newpipe.extractor.services.youtube.PoTokenProvider {
                        override fun getWebClientPoToken(s: String): PoTokenResult? {
                            val vData = poTokenProvider.getVisitorData()
                            val token = poTokenProvider.generatePoToken(s)
                            if (vData.isNullOrBlank() || token.isNullOrBlank()) {
                                return null
                            }
                            return PoTokenResult(vData, token, token)
                        }

                        override fun getWebEmbedClientPoToken(s: String): PoTokenResult? = getWebClientPoToken(s)
                        override fun getAndroidClientPoToken(s: String): PoTokenResult? = null
                        override fun getIosClientPoToken(s: String): PoTokenResult? = null
                    }
                    
                    YoutubeStreamExtractor.setPoTokenProvider(poTokenImpl)
                }
                isInitialized = true
                initializationDeferred.complete(Unit)
                PTLog.d("NewPipeInitializer", "NewPipe initialized successfully")
            } catch (e: Exception) {
                PTLog.e("NewPipeInitializer", "NewPipe initialization failed", e)
                throw e
            }
        }
    }
}
