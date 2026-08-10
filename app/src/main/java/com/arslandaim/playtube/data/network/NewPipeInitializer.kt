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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPipeInitializer @Inject constructor(
    private val okHttpClient: OkHttpClient
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
                }
                isInitialized = true
                initializationDeferred.complete(Unit)
                PTLog.d("NewPipeInitializer", "NewPipe initialized successfully")
            } catch (e: Exception) {
                PTLog.e("NewPipeInitializer", "NewPipe initialization failed", e)
                // We don't completeExceptionally here to allow retries if needed, 
                // or we could, depending on the error type.
                throw e
            }
        }
    }
}
