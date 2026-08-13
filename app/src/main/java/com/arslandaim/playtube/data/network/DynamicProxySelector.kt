/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import com.arslandaim.playtube.data.local.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.*

/**
 * A ProxySelector that dynamically checks preferences without blocking the calling thread.
 */
class DynamicProxySelector(
    preferencesManager: PreferencesManager
) : ProxySelector() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    @Volatile
    private var currentProxy: Proxy = Proxy.NO_PROXY

    init {
        scope.launch {
            combine(
                preferencesManager.isProxyEnabled,
                preferencesManager.proxyHost,
                preferencesManager.proxyPort
            ) { enabled, host, port ->
                if (enabled && host.isNotBlank()) {
                    try {
                        val address = InetSocketAddress.createUnresolved(host, port)
                        Proxy(Proxy.Type.HTTP, address)
                    } catch (e: Exception) {
                        Proxy.NO_PROXY
                    }
                } else {
                    Proxy.NO_PROXY
                }
            }.collect {
                currentProxy = it
            }
        }
    }

    override fun select(uri: URI?): List<Proxy> {
        // Return NO_PROXY for local/loopback addresses to avoid issues
        val host = uri?.host ?: ""
        if (host == "localhost" || host == "127.0.0.1") {
            return listOf(Proxy.NO_PROXY)
        }
        
        return listOf(currentProxy)
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        // Log failure if needed
    }
}
