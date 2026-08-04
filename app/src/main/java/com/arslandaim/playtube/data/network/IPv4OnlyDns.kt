/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

/**
 * Custom DNS implementation that prefers IPv4 addresses.
 * This is a workaround for some devices (like Xiaomi/MIUI) that have broken IPv6 discovery
 * or routing, leading to "No internet" or connection failures when IPv6 is attempted first.
 */
class IPv4OnlyDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val allAddresses = Dns.SYSTEM.lookup(hostname)
        val ipv4Addresses = allAddresses.filterIsInstance<Inet4Address>()
        
        // Return IPv4 addresses if found, otherwise fall back to all (to avoid total failure)
        return ipv4Addresses.ifEmpty { allAddresses }
    }
}
