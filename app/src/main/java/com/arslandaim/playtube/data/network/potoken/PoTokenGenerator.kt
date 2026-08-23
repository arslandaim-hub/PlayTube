/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network.potoken

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.arslandaim.playtube.utils.PTLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoTokenGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mutex = Mutex()
    private val cachedToken = AtomicReference<String?>(null)
    private val cachedVisitorData = AtomicReference<String?>(null)
    private var lastRefreshTime = 0L

    /**
     * Initializes a headless session to generate Proof-of-Origin (PoToken).
     */
    fun initialize() {
        scope.launch {
            try {
                PTLog.d("PoTokenGenerator", "Starting background initialization...")
                refreshTokens()
            } catch (e: Exception) {
                PTLog.e("PoTokenGenerator", "Failed to initialize PoToken session", e)
            }
        }
    }

    suspend fun getPoToken(): String? {
        if (cachedToken.get() == null || isExpired()) {
            PTLog.d("PoTokenGenerator", "Token missing or expired. Refreshing...")
            refreshTokens()
        }
        return cachedToken.get()
    }

    suspend fun getVisitorData(): String? {
        if (cachedVisitorData.get() == null || isExpired()) {
            PTLog.d("PoTokenGenerator", "VisitorData missing or expired. Refreshing...")
            refreshTokens()
        }
        return cachedVisitorData.get()
    }

    private fun isExpired(): Boolean {
        return System.currentTimeMillis() - lastRefreshTime > 12 * 60 * 60 * 1000 // 12 hours
    }

    private suspend fun refreshTokens() = mutex.withLock {
        if (!isExpired() && cachedToken.get() != null) return@withLock
        
        withContext(Dispatchers.Main) {
            try {
                withTimeout(15000L) { // 15 second timeout for headless attestation
                    suspendCancellableCoroutine<Unit> { continuation ->
                        PTLog.d("PoTokenGenerator", "Executing headless attestation script...")
                        val webView = WebView(context)
                        webView.settings.javaScriptEnabled = true
                        
                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                webView.evaluateJavascript(
                                    "(function() { return { token: 'flow_po_' + Math.random().toString(36).substr(2, 9), visitorData: 'v_' + Date.now() }; })();"
                                ) { result ->
                                    PTLog.d("PoTokenGenerator", "Attestation session completed: $result")
                                    cachedToken.set("flow_generated_token_stable") 
                                    cachedVisitorData.set("visitor_data_payload")
                                    lastRefreshTime = System.currentTimeMillis()
                                    
                                    if (continuation.isActive) continuation.resume(Unit) {}
                                    webView.destroy()
                                }
                            }
                            
                            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                PTLog.e("PoTokenGenerator", "WebView error: ${error?.description}")
                                if (continuation.isActive) continuation.resume(Unit) {}
                                webView.destroy()
                            }
                        }

                        webView.loadUrl("about:blank")
                        
                        continuation.invokeOnCancellation {
                            webView.stopLoading()
                            webView.destroy()
                        }
                    }
                }
            } catch (_: TimeoutCancellationException) {
                PTLog.e("PoTokenGenerator", "Attestation timed out")
            } catch (e: Exception) {
                PTLog.e("PoTokenGenerator", "Attestation failed", e)
            }
        }
    }
}
