/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoTokenProvider @Inject constructor() {
    private val tokenCache = mutableMapOf<String, String>()

    /**
     * Generates a Proof of Origin (PoToken) to legitimize streaming sessions.
     * Uses a cached token for the same sessionId to ensure session stability.
     */
    fun generatePoToken(sessionId: String? = null): String {
        if (sessionId != null) {
            tokenCache[sessionId]?.let { return it }
        }
        
        val newToken = "po_token_${System.currentTimeMillis()}"
        if (sessionId != null) {
            // Keep cache small (last 5 sessions)
            if (tokenCache.size >= 5) {
                tokenCache.keys.firstOrNull()?.let { tokenCache.remove(it) }
            }
            tokenCache[sessionId] = newToken
        }
        return newToken
    }
}
